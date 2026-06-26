package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.InstalledPack
import com.github.nols1000.nutaru.NutaruRepository

/**
 * One-shot install outcome the UI renders. [PackInstallState] is the live
 * progress; this is the terminal result. */
sealed class PackInstallResult {
    data class Success(val pack: InstalledPack) : PackInstallResult()
    data class Failure(val message: String) : PackInstallResult()
}

/**
 * Coordinator between [NutaruRepository] (DB) and [PackFetcher] (network).
 *
 * The UI talks to this instead of the raw repository so the install flow —
 * catalog → download (with progress) → SHA-256 verify → bulk import → packs row
 * — lives in one testable place. All methods are blocking; the caller dispatches
 * off the main thread (matching the rest of the app).
 *
 * Install runs as a single blocking call here; "background task with progress"
 * (issue-07 criterion 2) is the caller's job — it launches this in a coroutine
 * and reports [onProgress] back to the UI as state changes. The DB transaction
 * inside [PackImporter] is atomic, so the app stays consistent even if the
 * download is slow.
 */
class PackManager(
    private val repository: NutaruRepository,
    private val fetcher: PackFetcher,
    private val nowMillis: () -> Long,
) {

    /** The live catalog (network). Throws [PackFetchException] on failure. */
    fun catalog(): Catalog = fetcher.fetchCatalog()

    /** Installed packs (DB). Cheap, no network. */
    fun installedPacks(): List<InstalledPack> = repository.installedPacks()

    /**
     * Installs [entry]: download → verify SHA-256 → bulk import. [onProgress]
     * reports the download fraction in `[0.0, 1.0]`; import itself is fast and
     * not separately reported. Returns the terminal result for the UI.
     *
     * On checksum mismatch the import is aborted and nothing is written
     * (criterion 3); on download failure the error is surfaced as a Failure.
     */
    fun install(entry: CatalogEntry, onProgress: (Double) -> Unit): PackInstallResult = try {
        val bytes = fetcher.fetchPack(entry, onProgress)
        val result = repository.importPack(bytes, entry, nowMillis())
        val installed = repository.installedPacks().first { it.id == result.packId }
        PackInstallResult.Success(installed)
    } catch (e: ChecksumMismatchException) {
        PackInstallResult.Failure("Pack checksum mismatch — the download was corrupted or tampered with. Install aborted.")
    } catch (e: PackFetchException) {
        PackInstallResult.Failure(e.message ?: "Download failed.")
    } catch (e: Exception) {
        PackInstallResult.Failure(e.message ?: "Install failed.")
    }

    /** Re-imports [entry] over the existing pack (update path, criterion 7).
     *  Same flow as [install]; [PackImporter] drops the old products first. */
    fun update(entry: CatalogEntry, onProgress: (Double) -> Unit): PackInstallResult =
        install(entry, onProgress)

    /** Uninstalls [packId] — removes its products, prunes FTS, reclaims the
     *  source row if unreferenced. */
    fun uninstall(packId: String) {
        repository.uninstallPack(packId)
    }

    /**
     * Side-loads [packBytes] picked via the OS file picker (criterion 8). No
     * catalog entry, so no checksum to compare; the pack is still parsed and
     * imported, and its computed SHA-256 is recorded for later re-verification.
     */
    fun sideLoad(packBytes: ByteArray, fileName: String): PackInstallResult = try {
        val result = repository.importSideLoadedPack(packBytes, fileName, nowMillis())
        val installed = repository.installedPacks().first { it.id == result.packId }
        PackInstallResult.Success(installed)
    } catch (e: Exception) {
        PackInstallResult.Failure(e.message ?: "Side-load failed.")
    }
}
