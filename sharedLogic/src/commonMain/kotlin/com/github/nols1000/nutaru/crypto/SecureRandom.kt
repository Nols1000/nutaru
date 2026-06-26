package com.github.nols1000.nutaru.crypto

/**
 * CSPRNG source for entropy. Implemented per platform so each uses its
 * platform-native secure RNG (java.security.SecureRandom on JVM/Android,
 * SecRandomCopyBytes on iOS, Web Crypto on JS).
 *
 * Returns [size] cryptographically-secure random bytes.
 */
expect fun secureRandomBytes(size: Int): ByteArray
