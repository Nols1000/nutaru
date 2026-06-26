package com.github.nols1000.nutaru.crypto

// TODO: wire native Argon2id (CommonCrypto/KCryptoKit don't expose Argon2; ship
// the argon2 C reference impl via a pod or bundle a Swift port). Until then,
// iOS calls throw — only the JVM/Android path is exercised by tests today.
internal actual fun argon2idHash(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    memoryKib: Int,
    parallelism: Int,
    outputLength: Int,
): ByteArray = throw UnsupportedOperationException(
    "Argon2id on iOS is not wired yet — see TODO in this file.",
)
