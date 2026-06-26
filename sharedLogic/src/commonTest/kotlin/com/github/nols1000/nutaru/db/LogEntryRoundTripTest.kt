package com.github.nols1000.nutaru.db

import com.github.nols1000.nutaru.NutaruRepository
import com.github.nols1000.nutaru.crypto.Argon2id
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tracer-bullet round-trip for the "log food → dashboard" slice at the DB seam.
 *
 * Seeds the hardcoded foods, writes `log_entries` for two different days, reopens
 * the DB at the same path with the same key (the relaunch path), and asserts the
 * day-total rollup reads back the right sums per day. Exercises the exact path
 * the Android app takes through [NutaruRepository] and [com.github.nols1000.nutaru.FoodSeed].
 */
class LogEntryRoundTripTest {

    private val path = testDbAbsolutePath("nutaru-logentries-roundtrip.db")
    private val key = Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        iterations = 1,
        memoryKib = 1024,
        parallelism = 1,
        outputLength = 32,
    )

    private val dayAStart = 1_783_372_800_000L
    private val dayAEnd = dayAStart + 24L * 60 * 60 * 1000
    private val dayBStart = dayAEnd
    private val dayBEnd = dayBStart + 24L * 60 * 60 * 1000

    @AfterTest
    fun teardown() {
        deleteTestDb(path)
    }

    @Test
    fun seed_is_idempotent_and_day_totals_survive_relaunch() {
        // First launch: seed + log two entries on different days.
        val driver1 = openTestDriver(path, key)
        val repo1 = NutaruRepository(NutaruDatabase.invoke(driver1))
        repo1.seedFoods()
        repo1.seedFoods() // idempotent — must not duplicate the 5 seed rows.
        val foods = repo1.allFoods()
        assertEquals(5, foods.size, "Idempotent seed must produce exactly 5 rows.")

        val banana = foods.first { it.name == "Banana" }
        val chicken = foods.first { it.name == "Chicken breast (cooked)" }

        repo1.logFood(banana.id, 1.0, "ea", 118.0, "snack", "manual", dayAStart + 60_000)
        repo1.logFood(chicken.id, 1.0, "ea", 120.0, "lunch", "manual", dayBStart + 60_000)

        val dayA = repo1.dayTotal(dayAStart, dayAEnd)
        val dayB = repo1.dayTotal(dayBStart, dayBEnd)
        // Banana 118g: 89 * 1.18 = 105.02 kcal
        assertEquals(89.0 * 1.18, dayA.kcal, 0.001)
        // Chicken 120g: 165 * 1.2 = 198.0 kcal
        assertEquals(165.0 * 1.2, dayB.kcal, 0.001)
        driver1.close()

        // Relaunch: reopen with the same key at the same path, no re-seed.
        val driver2 = openTestDriver(path, key)
        val repo2 = NutaruRepository(NutaruDatabase.invoke(driver2))
        val reopenedDayA = repo2.dayTotal(dayAStart, dayAEnd)
        val reopenedDayB = repo2.dayTotal(dayBStart, dayBEnd)
        assertEquals(dayA, reopenedDayA, "Day A totals must survive relaunch.")
        assertEquals(dayB, reopenedDayB, "Day B totals must survive relaunch.")
        driver2.close()
    }

    @Test
    fun different_date_logs_roll_up_to_their_own_day_not_today() {
        val driver = openTestDriver(path, key)
        val repo = NutaruRepository(NutaruDatabase.invoke(driver))
        repo.seedFoods()
        val banana = repo.allFoods().first { it.name == "Banana" }

        // Log one on day A and one on day B.
        repo.logFood(banana.id, 1.0, "ea", 100.0, "snack", "manual", dayAStart + 1)
        repo.logFood(banana.id, 1.0, "ea", 200.0, "snack", "manual", dayBStart + 1)

        // Day A sees only the 100g banana (89 kcal); Day B sees only the 200g (178 kcal).
        assertEquals(89.0, repo.dayTotal(dayAStart, dayAEnd).kcal, 0.001)
        assertEquals(178.0, repo.dayTotal(dayBStart, dayBEnd).kcal, 0.001)
        driver.close()
    }
}