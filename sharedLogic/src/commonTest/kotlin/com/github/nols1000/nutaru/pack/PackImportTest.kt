package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.NutaruRepository
import com.github.nols1000.nutaru.crypto.Argon2id
import com.github.nols1000.nutaru.db.NutaruDatabase
import com.github.nols1000.nutaru.db.deleteTestDb
import com.github.nols1000.nutaru.db.openTestDriver
import com.github.nols1000.nutaru.db.testDbAbsolutePath
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Pack import runtime — issue-07 criterion 9 ("FlatBuffer stream parse → SQL
 * bulk insert round-trip") and criterion 3 ("SHA-256 mismatch aborts install").
 *
 * Builds a small pack in-memory with the KMP codec, imports it through
 * [PackImporter] against a real (JDBC) NutaruDatabase, and asserts:
 *  - the catalog row lands in `packs` with the right item count,
 *  - every product is bulk-inserted and reachable via the FTS5 search index,
 *  - the OFF `sources` row is populated with attribution,
 *  - a checksum mismatch aborts BEFORE any product is written (supply-chain
 *    integrity: a tampered pack never reaches the products table).
 *
 * The same code path runs on Android (SQLCipher) and iOS; only the driver
 * differs, which is why this lives at the commonTest seam.
 */
class PackImportTest {

    private val path = testDbAbsolutePath("nutaru-pack-import.db")
    private val key = testKey()
    private lateinit var db: NutaruDatabase

    private val packProducts = listOf(
        Product(
            id = 1_000_001L,
            barcode = "0012345678905",
            nameI18n = linkedMapOf("en" to "Cheerios"),
            brand = "General Mills",
            categories = listOf("breakfast-cereals"),
            kcalPer100g = 366.0, proteinGPer100g = 7.2, carbsGPer100g = 73.2, fatGPer100g = 5.4,
            servings = listOf(Serving("1 cup (28g)", 28.0)),
            ingredients = "Whole grain oats, sugar",
            sourceId = PackFormat.SOURCE_ID_OFF,
            license = PackFormat.LICENSE_ODBL,
            attribution = PackFormat.ATTRIBUTION_OFF,
        ),
        Product(
            id = 1_000_002L,
            barcode = null,
            nameI18n = linkedMapOf("en" to "Tap water"),
            brand = null,
            categories = emptyList(),
            kcalPer100g = 0.0, proteinGPer100g = 0.0, carbsGPer100g = 0.0, fatGPer100g = 0.0,
            servings = emptyList(),
            ingredients = null,
            sourceId = PackFormat.SOURCE_ID_OFF,
            license = PackFormat.LICENSE_ODBL,
            attribution = PackFormat.ATTRIBUTION_OFF,
        ),
    )

    private val entry = CatalogEntry(
        id = "us", name = "US starter pack", version = "1.0.0", region = "US",
        itemCount = packProducts.size, byteSize = 0L,
        sha256 = "", url = "https://example/us.pack",
        license = PackFormat.LICENSE_ODBL, attribution = PackFormat.ATTRIBUTION_OFF,
    )

    @AfterTest
    fun teardown() {
        deleteTestDb(path)
    }

    @Test
    fun import_inserts_products_populates_fts_and_records_pack_metadata() {
        db = openDb()
        val bytes = PackCodec.encode(packProducts)
        val catalogEntry = entry.copy(sha256 = sha256Hex(bytes), byteSize = bytes.size.toLong())

        val result = PackImporter.importPack(db, bytes, catalogEntry, nowMillis = 1_700_000_000L)

        assertEquals("us", result.packId)
        assertEquals(packProducts.size, result.itemCount)

        val repo = NutaruRepository(db)
        val installed = repo.installedPacks()
        assertEquals(1, installed.size)
        assertEquals("us", installed[0].id)
        assertEquals(packProducts.size, installed[0].itemCount)

        // Both pack products are searchable via FTS5.
        val cheerios = repo.searchFoods("cheerios")
        assertEquals(1, cheerios.size)
        assertEquals("Cheerios", cheerios[0].name)
        val water = repo.searchFoods("water")
        assertEquals("Tap water", water[0].name)

        // OFF source row carries attribution.
        val source = db.sourcesQueries.selectSource(PackFormat.SOURCE_ID_OFF).executeAsOne()
        assertEquals(PackFormat.LICENSE_ODBL, source.license)
        assertEquals(PackFormat.ATTRIBUTION_OFF, source.attribution)
    }

    @Test
    fun checksum_mismatch_aborts_install_with_no_products_written() {
        db = openDb()
        val bytes = PackCodec.encode(packProducts)
        val tampered = bytes.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0xFF).toByte()
        val catalogEntry = entry.copy(sha256 = sha256Hex(bytes)) // expected hash of the ORIGINAL

        assertFailsWith<ChecksumMismatchException> {
            PackImporter.importPack(db, tampered, catalogEntry, nowMillis = 1_700_000_000L)
        }

        // Nothing was committed: no pack row, no products.
        val repo = NutaruRepository(db)
        assertTrue(repo.installedPacks().isEmpty(), "No pack row after a failed import.")
        assertTrue(repo.searchFoods("cheerios").isEmpty(), "No products after a failed import.")
    }

    @Test
    fun side_loaded_pack_without_checksum_imports_anyway() {
        db = openDb()
        val bytes = PackCodec.encode(packProducts)
        // Side-load path: no manifest entry, so no expected checksum to compare.
        val result = PackImporter.importSideLoadedPack(
            db, bytes, fileName = "us.pack", nowMillis = 1_700_000_000L,
        )
        assertEquals(packProducts.size, result.itemCount)
        assertEquals(1, NutaruRepository(db).installedPacks().size)
    }

    private fun openDb(): NutaruDatabase {
        val driver = openTestDriver(path, key)
        return NutaruDatabase.invoke(driver)
    }

    private fun testKey() = Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        iterations = 1, memoryKib = 1024, parallelism = 1, outputLength = 32,
    )
}
