package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.NutaruRepository
import com.github.nols1000.nutaru.crypto.Argon2id
import com.github.nols1000.nutaru.db.NutaruDatabase
import com.github.nols1000.nutaru.db.deleteTestDb
import com.github.nols1000.nutaru.db.openTestDriver
import com.github.nols1000.nutaru.db.testDbAbsolutePath
import app.cash.sqldelight.db.SqlDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * PackManager orchestration — the install/update/uninstall/side-load flow the UI
 * drives. Uses a fake [PackFetcher] so the catalog + download + progress are
 * deterministic; the DB half runs against the real JDBC driver, so this is an
 * end-to-end test of the coordinator with only the network stubbed.
 *
 * Covers issue-07 criterion 2 (install runs with progress, app stays usable —
 * the manager is non-blocking from the caller's perspective via [onProgress])
 * and the failure paths (download failure → Failure; checksum mismatch →
 * Failure with a clear message, nothing written).
 */
class PackManagerTest {

    private val path = testDbAbsolutePath("nutaru-pack-manager.db")
    private val key = testKey()
    private val driver: SqlDriver = openTestDriver(path, key)
    private val db: NutaruDatabase = NutaruDatabase.invoke(driver)
    private val repo = NutaruRepository(db)

    private val products = listOf(
        Product(1_000_001L, "1", linkedMapOf("en" to "Cereal"), "BrandA", listOf("cereal"),
            1.0, 1.0, 1.0, 1.0, emptyList(), null, PackFormat.SOURCE_ID_OFF, PackFormat.LICENSE_ODBL, PackFormat.ATTRIBUTION_OFF),
    )

    @AfterTest
    fun teardown() {
        driver.close()
        deleteTestDb(path)
    }

    @Test
    fun install_downloads_verifies_and_imports_with_progress_callbacks() {
        val bytes = PackCodec.encode(products)
        val entry = entry("us", bytes)
        val fetcher = FakeFetcher(catalog = Catalog(packs = listOf(entry)), packBytes = bytes)
        val progress = mutableListOf<Double>()
        val manager = PackManager(repo, fetcher, nowMillis = { 1_700_000_000L })

        val result = manager.install(entry, onProgress = { progress += it })

        assertIs<PackInstallResult.Success>(result)
        assertEquals("us", result.pack.id)
        assertEquals(1, repo.installedPacks().size)
        assertEquals("Cereal", repo.searchFoods("cereal")[0].name)
        // Progress reported monotonically, ending at 1.0.
        assertTrue(progress.isNotEmpty(), "Progress must be reported during download.")
        assertEquals(1.0, progress.last(), "Final progress must be 1.0.")
    }

    @Test
    fun install_surfaces_download_failure_as_Failure_and_writes_nothing() {
        val bytes = PackCodec.encode(products)
        val entry = entry("us", bytes)
        val fetcher = FakeFetcher(catalog = Catalog(packs = listOf(entry)), packBytes = bytes, failPack = true)
        val manager = PackManager(repo, fetcher, nowMillis = { 1L })

        val result = manager.install(entry, onProgress = {})

        assertIs<PackInstallResult.Failure>(result)
        assertTrue(repo.installedPacks().isEmpty(), "No pack row after a failed download.")
    }

    @Test
    fun install_surfaces_checksum_mismatch_as_Failure_and_writes_nothing() {
        val bytes = PackCodec.encode(products)
        val entry = entry("us", bytes)
        // Fetcher returns the right bytes but the entry pins a WRONG checksum.
        val tamperedEntry = entry.copy(sha256 = "deadbeef".repeat(8))
        val fetcher = FakeFetcher(catalog = Catalog(packs = listOf(tamperedEntry)), packBytes = bytes)
        val manager = PackManager(repo, fetcher, nowMillis = { 1L })

        val result = manager.install(tamperedEntry, onProgress = {})

        assertIs<PackInstallResult.Failure>(result)
        assertTrue(result.message.contains("checksum", ignoreCase = true), "Failure must explain the checksum mismatch: ${result.message}")
        assertTrue(repo.installedPacks().isEmpty(), "No pack row after a checksum mismatch.")
    }

    @Test
    fun sideLoad_imports_without_a_catalog_entry() {
        val bytes = PackCodec.encode(products)
        val fetcher = FakeFetcher(catalog = Catalog(), packBytes = bytes)
        val manager = PackManager(repo, fetcher, nowMillis = { 1_700_000_000L })

        val result = manager.sideLoad(bytes, fileName = "custom.pack")

        assertIs<PackInstallResult.Success>(result)
        assertEquals(1, repo.installedPacks().size)
        assertEquals("Cereal", repo.searchFoods("cereal")[0].name)
    }

    @Test
    fun uninstall_via_manager_removes_pack_products() {
        val bytes = PackCodec.encode(products)
        val entry = entry("us", bytes)
        val fetcher = FakeFetcher(catalog = Catalog(packs = listOf(entry)), packBytes = bytes)
        val manager = PackManager(repo, fetcher, nowMillis = { 1_700_000_000L })
        manager.install(entry, onProgress = {})

        manager.uninstall("us")

        assertTrue(repo.installedPacks().isEmpty())
        assertTrue(repo.searchFoods("cereal").isEmpty())
    }

    private fun entry(id: String, bytes: ByteArray) = CatalogEntry(
        id = id, name = "$id pack", version = "1.0.0", region = id.uppercase(),
        itemCount = products.size, byteSize = bytes.size.toLong(),
        sha256 = sha256Hex(bytes), url = "https://example/$id.pack",
        license = PackFormat.LICENSE_ODBL, attribution = PackFormat.ATTRIBUTION_OFF,
    )

    private fun testKey() = Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        iterations = 1, memoryKib = 1024, parallelism = 1, outputLength = 32,
    )
}

/** Fake fetcher: serves the catalog + pack bytes from memory, optionally fails
 *  the pack download to exercise the error path. Reports progress in two steps. */
private class FakeFetcher(
    private val catalog: Catalog,
    private val packBytes: ByteArray,
    private val failPack: Boolean = false,
) : PackFetcher {
    override fun fetchCatalog(): Catalog = catalog
    override fun fetchPack(entry: CatalogEntry, onProgress: (Double) -> Unit): ByteArray {
        if (failPack) throw PackFetchException("Network unavailable (fake)")
        onProgress(0.5)
        onProgress(1.0)
        return packBytes
    }
}
