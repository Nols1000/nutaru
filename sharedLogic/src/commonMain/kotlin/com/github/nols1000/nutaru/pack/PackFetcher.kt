package com.github.nols1000.nutaru.pack

/**
 * Platform-supplied catalog + pack byte fetcher. The import runtime's pure DB
 * logic ([PackImporter]) is covered by `commonTest`; this seam is where the
 * network lives, so the platform wires the real HTTP client and a fake drives
 * any future orchestration test.
 *
 * All calls are blocking — the caller dispatches off the main thread, matching
 * the rest of [com.github.nols1000.nutaru.NutaruRepository]. [fetchPack] reports
 * download progress as a fraction in `[0.0, 1.0]` so the UI can render a bar
 * while the app stays usable (issue-07 criterion 2).
 */
interface PackFetcher {

    /** Fetches + decodes the catalog manifest at `nutaru.app/catalog.json`. */
    fun fetchCatalog(): Catalog

    /**
     * Downloads the pack at [entry.url]. Calls [onProgress] with the fraction
     * complete (bytes downloaded / total) as bytes arrive; the final call is 1.0.
     * Throws on any network/HTTP failure — the caller surfaces the error.
     */
    fun fetchPack(entry: CatalogEntry, onProgress: (Double) -> Unit): ByteArray
}

/**
 * Thrown when the catalog or pack download fails (no network, HTTP error, etc.).
 * The UI maps this to a user-readable "install failed" message.
 */
class PackFetchException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
