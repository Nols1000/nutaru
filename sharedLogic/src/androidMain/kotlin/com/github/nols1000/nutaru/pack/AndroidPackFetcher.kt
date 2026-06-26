package com.github.nols1000.nutaru.pack

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android [PackFetcher] — plain `HttpURLConnection` over HTTPS.
 *
 * No OkHttp/ktor dependency: the catalog + packs are plain HTTPS GETs with a
 * pinned manifest URL, and `HttpURLConnection` is in the platform. Downloads
 * stream into a `ByteArrayOutputStream` with progress reported per 64 KiB chunk
 * against the `Content-Length` header. This is the seam the import runtime
 * pulls pack bytes through; the pure DB logic is covered by `commonTest`.
 *
 * TODO(issue-17 / beta hardening): move the download onto a WorkManager job so
 * a pack install survives a backgrounding, and surface a notification. For V1
 * the foreground coroutine + progress UI is sufficient.
 */
object AndroidPackFetcher : PackFetcher {

    private const val CATALOG_URL = "https://nutaru.app/catalog.json"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val PROGRESS_CHUNK_BYTES = 64 * 1024

    override fun fetchCatalog(): Catalog = try {
        val bytes = download(URL(CATALOG_URL)) {}
        Catalog.decode(String(bytes, Charsets.UTF_8))
    } catch (e: Exception) {
        throw PackFetchException("Could not fetch the pack catalog. Check your connection and try again.", e)
    }

    override fun fetchPack(entry: CatalogEntry, onProgress: (Double) -> Unit): ByteArray = try {
        download(URL(entry.url), onProgress)
    } catch (e: Exception) {
        throw PackFetchException("Could not download \"${entry.name}\". Check your connection and try again.", e)
    }

    private fun download(url: URL, onProgress: (Double) -> Unit): ByteArray {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw PackFetchException("HTTP $code downloading ${url.file}")
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
            val out = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buf = ByteArray(PROGRESS_CHUNK_BYTES)
                var read: Int
                var downloaded = 0L
                while (input.read(buf).also { read = it } >= 0) {
                    if (read == 0) continue
                    out.write(buf, 0, read)
                    downloaded += read
                    if (total > 0) onProgress((downloaded.toDouble() / total).coerceIn(0.0, 1.0))
                }
            }
            if (total <= 0) onProgress(1.0)
            onProgress(1.0)
            return out.toByteArray()
        } finally {
            conn.disconnect()
        }
    }
}

/** A pointer to the side-load file-picker host so the UI can request a pack
 *  file via the OS picker without `sharedUI` importing Android SAF. The
 *  Activity implements this; sharedUI calls it through the interface. */
interface SideLoadPicker {
    /** Launches the OS file picker for `*.pack` files; [onPicked] receives the
     *  bytes + display name, or is not called if the user cancels. */
    fun pickPackFile(onPicked: (ByteArray, String) -> Unit)
}

/** Helper for androidApp: the default catalog URL, surfaced so a debug build
 *  could point at a staging manifest without touching commonMain. */
fun defaultCatalogUrl(): String = "https://nutaru.app/catalog.json"
