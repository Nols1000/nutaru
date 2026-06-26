package com.github.nols1000.nutaru.db

import com.github.nols1000.nutaru.NutaruRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * commonTest round-trip for the `settings` singleton + mnemonic acknowledgment
 * flag (issue-04, criterion 6). Verifies the migration v4→v5 applies (the
 * `settings` table exists), the flag defaults to not-acknowledged, and an
 * acknowledgment survives a close/reopen — which is what makes relaunch skip
 * the forced reveal screen and go straight to Home.
 */
class MnemonicAckPersistenceTest {

    private val path = testDbAbsolutePath("nutaru-mnem-ack-roundtrip.db")
    private val key = AckDeriveKeys.fastTestKey()

    @AfterTest
    fun teardown() {
        deleteTestDb(path)
    }

    @Test
    fun fresh_database_is_not_acknowledged() {
        val driver = openTestDriver(path, key)
        val repo = NutaruRepository(NutaruDatabase.invoke(driver))
        assertFalse(repo.isMnemonicAcknowledged(), "No settings row yet → not acknowledged.")
        driver.close()
    }

    @Test
    fun acknowledgment_survives_relaunch() {
        // First launch: persist profile so `hasProfile` would be true, then ack.
        val driver1 = openTestDriver(path, key)
        val repo1 = NutaruRepository(NutaruDatabase.invoke(driver1))
        repo1.saveOnboardingProfile(
            com.github.nols1000.nutaru.Profile(
                30,
                com.github.nols1000.nutaru.Sex.MALE,
                180,
                80.0,
                com.github.nols1000.nutaru.Goal.MAINTAIN,
                com.github.nols1000.nutaru.ActivityLevel.MODERATE,
            ),
            nowMillis = 1_783_372_800_000L,
        )
        assertFalse(repo1.isMnemonicAcknowledged())
        repo1.acknowledgeMnemonic()
        assertTrue(repo1.isMnemonicAcknowledged())
        driver1.close()

        // Relaunch: reopen at the same path, the flag must survive.
        val driver2 = openTestDriver(path, key)
        val repo2 = NutaruRepository(NutaruDatabase.invoke(driver2))
        assertTrue(repo2.isMnemonicAcknowledged(), "Acknowledgment must persist across relaunch.")
        driver2.close()
    }

    @Test
    fun acknowledge_is_idempotent() {
        val driver = openTestDriver(path, key)
        val repo = NutaruRepository(NutaruDatabase.invoke(driver))
        repo.acknowledgeMnemonic()
        repo.acknowledgeMnemonic()
        assertTrue(repo.isMnemonicAcknowledged())
        driver.close()
    }
}

private object AckDeriveKeys {
    fun fastTestKey(): ByteArray = com.github.nols1000.nutaru.crypto.Argon2id.deriveKey(
        password = "test-mnemonic-entropy-fixture".encodeToByteArray(),
        salt = "nutaru-test-salt".encodeToByteArray(),
        iterations = 1,
        memoryKib = 1024,
        parallelism = 1,
        outputLength = 32,
    )
}
