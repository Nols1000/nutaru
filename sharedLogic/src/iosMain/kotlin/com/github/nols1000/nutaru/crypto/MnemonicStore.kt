package com.github.nols1000.nutaru.crypto

// TODO(issue-18): persist to iOS Keychain. The iOS encrypted-storage wiring is
// currently a stub, so this returns null on every launch (treated as first
// launch). Acceptable for the tracer-bullet which targets Android for V1.
actual object MnemonicStore {
    actual fun load(): List<String>? = null
    actual fun store(mnemonic: List<String>) = Unit
}