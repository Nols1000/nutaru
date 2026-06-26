package com.github.nols1000.nutaru.crypto

// TODO: web persistence (localStorage) is out of scope for V1 — the webApp
// ships no product. Stubbed so the JS target compiles.
actual object MnemonicStore {
    actual fun load(): List<String>? = null
    actual fun store(mnemonic: List<String>) = Unit
}