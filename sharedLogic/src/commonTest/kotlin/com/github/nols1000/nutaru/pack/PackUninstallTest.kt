package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.FoodSeed
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Uninstall semantics — issue-07 criterion 10 ("uninstall removes only matching
 * `pack_id` rows, leaves other packs intact") and criterion 7 ("source row
 * removed if no other packs reference it").
 *
 * Installs two packs sharing the OFF source (id 1) plus the hardcoded seed
 * foods (pack_id NULL), uninstalls one, and asserts: only that pack's products
 * are gone, the other pack + seeds survive, and the OFF source row is reclaimed
 * only once the last pack referencing it is removed.
 */
class PackUninstallTest {

    private val path = testDbAbsolutePath("nutaru-pack-uninstall.db")
    private val key = testKey()
    private val driver: SqlDriver = openTestDriver(path, key)
    private val db: NutaruDatabase = NutaruDatabase.invoke(driver)

    private val usProducts = listOf(
        Product(1_000_001L, "1", linkedMapOf("en" to "US Cereal"), "BrandA", listOf("cereal"),
            1.0, 1.0, 1.0, 1.0, emptyList(), null, PackFormat.SOURCE_ID_OFF, PackFormat.LICENSE_ODBL, PackFormat.ATTRIBUTION_OFF),
        Product(1_000_002L, "2", linkedMapOf("en" to "US Bread"), "BrandB", listOf("bread"),
            2.0, 2.0, 2.0, 2.0, emptyList(), null, PackFormat.SOURCE_ID_OFF, PackFormat.LICENSE_ODBL, PackFormat.ATTRIBUTION_OFF),
    )
    private val ukProducts = listOf(
        Product(1_000_003L, "3", linkedMapOf("en" to "UK Biscuit"), "BrandC", listOf("biscuit"),
            3.0, 3.0, 3.0, 3.0, emptyList(), null, PackFormat.SOURCE_ID_OFF, PackFormat.LICENSE_ODBL, PackFormat.ATTRIBUTION_OFF),
    )

    @AfterTest
    fun teardown() {
        driver.close()
        deleteTestDb(path)
    }

    @Test
    fun uninstall_removes_only_the_matching_pack_and_reclaims_source_when_unreferenced() {
        val repo = NutaruRepository(db)
        repo.seedFoods() // pack_id NULL seed rows must survive every uninstall.

        importPack("us", usProducts)
        importPack("uk", ukProducts)

        // Precondition: both packs + their products + the shared OFF source row.
        assertEquals(2, repo.installedPacks().size)
        assertEquals(2, repo.searchFoods("us").size)
        assertEquals(1, repo.searchFoods("uk").size)
        assertEquals(1L, db.sourcesQueries.selectSource(PackFormat.SOURCE_ID_OFF).executeAsOne().id)

        // Uninstall the US pack.
        repo.uninstallPack("us")

        // US pack row + US products gone; UK pack + products + seeds intact.
        assertEquals(1, repo.installedPacks().size)
        assertEquals("uk", repo.installedPacks()[0].id)
        assertTrue(repo.searchFoods("us").isEmpty(), "US products must be gone after uninstall.")
        assertEquals(1, repo.searchFoods("uk").size, "UK products must survive.")
        // 5 seed foods (pack_id NULL) + 1 UK product = 6 total; the 5 seed names
        // must all still be present, proving uninstall didn't touch them.
        val allNames = repo.allFoods().map { it.name }.toSet()
        assertTrue(FoodSeed.ITEMS.map { it.name }.all { it in allNames }, "Seed foods must survive (pack_id NULL).")
        assertEquals(5 + 1, repo.allFoods().size, "Seeds (5) + UK products (1) after US uninstall.")

        // OFF source still referenced by the UK pack → row kept.
        assertEquals(1L, db.sourcesQueries.selectSource(PackFormat.SOURCE_ID_OFF).executeAsOne().id)

        // Uninstall the last pack → source row reclaimed.
        repo.uninstallPack("uk")
        assertEquals(0, repo.installedPacks().size)
        assertNull(
            db.sourcesQueries.selectSource(PackFormat.SOURCE_ID_OFF).executeAsOneOrNull(),
            "Source row reclaimed once no pack references it.",
        )
        assertEquals(5, repo.allFoods().size, "Only seed foods remain after both uninstalls.")
    }

    private fun importPack(id: String, products: List<Product>) {
        val bytes = PackCodec.encode(products)
        val entry = CatalogEntry(
            id = id, name = id, version = "1.0.0", region = id.uppercase(),
            itemCount = products.size, byteSize = bytes.size.toLong(),
            sha256 = sha256Hex(bytes), url = "https://example/$id.pack",
            license = PackFormat.LICENSE_ODBL, attribution = PackFormat.ATTRIBUTION_OFF,
        )
        PackImporter.importPack(db, bytes, entry, nowMillis = 1_700_000_000L)
    }

    private fun testKey() = Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        iterations = 1, memoryKib = 1024, parallelism = 1, outputLength = 32,
    )
}
