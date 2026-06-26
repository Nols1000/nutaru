package com.github.nols1000.nutaru.crypto

/**
 * Persists the 12-word recovery mnemonic between launches so the encrypted DB
 * can be re-opened without re-onboarding.
 *
 * The mnemonic is the root secret: anyone who reads it can derive the SQLCipher
 * key. On Android it lives in SharedPreferences private to the app's data dir
 * (OS sandbox); a hardware-backed KeyStore wrapper is a TODO tracked separately.
 * On iOS it will live in the Keychain (issue-18 follow-ups).
 *
 * `null` return from [load] means "first launch — generate and store".
 */
expect object MnemonicStore {

    /** Read the stored mnemonic, or null if none has been written yet. */
    fun load(): List<String>?

    /** Persist [mnemonic]. Called once on first launch, before any DB write. */
    fun store(mnemonic: List<String>)
}