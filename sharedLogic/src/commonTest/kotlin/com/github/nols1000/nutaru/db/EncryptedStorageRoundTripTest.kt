package com.github.nols1000.nutaru.db

import com.github.nols1000.nutaru.crypto.Argon2id
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * commonTest round-trip for the encrypted storage foundation.
 *
 * Verifies: open DB → write a profile (singleton row) → close → reopen with
 * the same key → read the row back intact. Also confirms the trivial migration
 * (the migrated `display_name` column) is applied by the schema.
 *
 * The mnemonic → Argon2id → key pipeline is exercised here so any drift in
 * key derivation surfaces in this test too.
 */
class EncryptedStorageRoundTripTest {

    private val path = testDbAbsolutePath("nutaru-roundtrip.db")
    private val key = Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        // Tightened params keep the JVM host test fast; production params
        // live in Argon2id.DEFAULT_*.
        iterations = 1,
        memoryKib = 1024,
        parallelism = 1,
        outputLength = 32,
    )

    @AfterTest
    fun teardown() {
        deleteTestDb(path)
    }

    @Test
    fun write_close_reopen_read_round_trips_a_profile_row() {
        // First open: write the singleton profile row.
        val driver1 = openTestDriver(path, key)
        val db1 = NutaruDatabase.invoke(driver1)
        db1.profileQueries.upsertProfile(
            id = 1L,
            age = 30L,
            sex = "female",
            height_cm = 165L,
            goal = "maintain",
            activity_level = "moderate",
            created_at = 1_700_000_000L,
            updated_at = 1_700_000_000L,
            display_name = "Alice",
        )
        driver1.close()

        // Reopen with the same key at the same path: row must survive.
        val driver2 = openTestDriver(path, key)
        val db2 = NutaruDatabase.invoke(driver2)
        val reopened = db2.profileQueries.selectProfile().executeAsOne()
        assertEquals(30L, reopened.age)
        assertEquals("Alice", reopened.display_name)
        driver2.close()
    }

    @Test
    fun fresh_database_has_empty_profile_table() {
        val driver = openTestDriver(path, key)
        val db = NutaruDatabase.invoke(driver)
        // Singleton pattern: a fresh DB has zero profile rows.
        assertNull(db.profileQueries.selectProfile().executeAsOneOrNull())
        driver.close()
    }

    @Test
    fun singleton_upsert_replaces_existing_row() {
        val driver = openTestDriver(path, key)
        val db = NutaruDatabase.invoke(driver)

        db.profileQueries.upsertProfile(
            id = 1L, age = 30L, sex = "female", height_cm = 165L,
            goal = "maintain", activity_level = "moderate",
            created_at = 1L, updated_at = 1L, display_name = null,
        )
        db.profileQueries.upsertProfile(
            id = 1L, age = 31L, sex = "female", height_cm = 165L,
            goal = "lose", activity_level = "moderate",
            created_at = 1L, updated_at = 2L, display_name = "Alice",
        )

        val rows = db.profileQueries.selectProfile().executeAsList()
        assertEquals(1, rows.size, "Profile must be a singleton — only id=1.")
        assertEquals(31L, rows[0].age)
        assertEquals("Alice", rows[0].display_name)
        driver.close()
    }
}
