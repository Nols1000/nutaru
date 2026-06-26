package com.github.nols1000.nutaru.crypto

import java.security.SecureRandom

private val secureRandom: SecureRandom = SecureRandom()

actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    secureRandom.nextBytes(bytes)
    return bytes
}
