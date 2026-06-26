package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver
import com.github.nols1000.nutaru.crypto.Argon2id
import com.github.nols1000.nutaru.crypto.Bip39

/**
 * Opens the encrypted SQLDelight database used as the single source of truth.
 *
 * Flow: mnemonic → entropy → Argon2id key → SQLCipher driver → [NutaruDatabase].
 * Same mnemonic always produces the same key, so subsequent launches re-open
 * the existing database without re-onboarding.
 *
 * The salt is a fixed app-level constant rather than a per-install random salt.
 * Trade-off: a per-install random salt would need to live somewhere readable
 * before the DB opens (e.g. plain SharedPreferences). The mnemonic itself is
 * already 128 bits of unique per-user entropy, so app-level salt is sufficient
 * to bind the derived key to this app. Re-evaluate if mnemonic + a server-side
 * identifier ever become a thing (they shouldn't — local-first only).
 */
object EncryptedDatabase {

    private val APP_SALT: ByteArray = "nutaru-v1-sqlcipher-salt".encodeToByteArray()

    fun deriveKey(mnemonic: List<String>): ByteArray {
        val entropy = Bip39.mnemonicToEntropy(mnemonic)
        return Argon2id.deriveKey(password = entropy, salt = APP_SALT)
    }

    fun open(path: String, mnemonic: List<String>): NutaruDatabase {
        val key = deriveKey(mnemonic)
        val driver = createSqlCipherDriver(path, key)
        return NutaruDatabase.invoke(driver)
    }
}

internal expect fun createSqlCipherDriver(path: String, key: ByteArray): SqlDriver
