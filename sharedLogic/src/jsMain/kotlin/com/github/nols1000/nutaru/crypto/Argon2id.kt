package com.github.nols1000.nutaru.crypto

// TODO: wire WebCrypto SubtleCrypto.deriveBits with an Argon2 polyfill, or
// bundle a WASM argon2 implementation. JS is not shipped for V1.
internal actual fun argon2idHash(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    memoryKib: Int,
    parallelism: Int,
    outputLength: Int,
): ByteArray = throw UnsupportedOperationException(
    "Argon2id on JS is not wired yet — JS is not shipped for V1.",
)
