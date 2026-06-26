package com.github.nols1000.nutaru.crypto

import com.github.nols1000.nutaru.db.AppContextHolder

/**
 * Android mnemonic persistence: SharedPreferences private to the app sandbox.
 *
 * TODO(issue-20 / hardening): wrap the mnemonic with a hardware-backed Keystore
 * key before writing to prefs. The app sandbox already isolates it from other
 * apps, but a rooted device or a backup extractor would read it in plaintext.
 * SQLCipher's at-rest guarantee covers the DB, not this prefs file — that gap is
 * acceptable for the tracer-bullet, not for the V1 ship bar.
 */
actual object MnemonicStore {

    private const val PREFS_NAME = "nutaru-secrets"
    private const val KEY_MNEMONIC = "mnemonic"

    private val prefs by lazy {
        val context = AppContextHolder.context
            ?: error("AppContextHolder.context must be set before using MnemonicStore.")
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    actual fun load(): List<String>? {
        val raw = prefs.getString(KEY_MNEMONIC, null) ?: return null
        val words = raw.split(',')
        return if (words.size == 12) words else null
    }

    actual fun store(mnemonic: List<String>) {
        require(mnemonic.size == 12) { "12-word mnemonic required; got ${mnemonic.size}" }
        prefs.edit().putString(KEY_MNEMONIC, mnemonic.joinToString(",")).apply()
    }
}