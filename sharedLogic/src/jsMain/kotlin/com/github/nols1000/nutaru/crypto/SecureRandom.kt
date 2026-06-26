package com.github.nols1000.nutaru.crypto

// Synchronous CSPRNG backed by globalThis.crypto.getRandomValues (browsers
// and Node ≥ 19). Kotlin/JS lowers ByteArray to Int8Array, so we hand the
// buffer to the WebCrypto call in-place.
@JsFun("(size) => { const buf = new Int8Array(size); globalThis.crypto.getRandomValues(buf); return buf; }")
private external fun jsCryptoRandom(size: Int): ByteArray

actual fun secureRandomBytes(size: Int): ByteArray = jsCryptoRandom(size)
