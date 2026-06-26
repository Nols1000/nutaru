package com.github.nols1000.nutaru.crypto

/**
 * Argon2id key derivation for the SQLCipher key.
 *
 * Parameters tuned to land near ~250ms on the reference hardware called out in
 * the PRD (Pixel 5 / iPhone 17 Pro). 64 MiB / 3 passes / parallelism 1 is the
 * baseline; tightening the budget per platform is a TODO once we have on-device
 * benchmarks.
 */
object Argon2id {

    // TODO: tune iterations/memory against Pixel 5 / iPhone 17 Pro reference
    // hardware once on-device benchmarks are available. Target ~250ms.
    const val DEFAULT_ITERATIONS = 3
    const val DEFAULT_MEMORY_KIB = 65_536 // 64 MiB
    const val DEFAULT_PARALLELISM = 1
    const val DEFAULT_OUTPUT_LENGTH = 32   // 256-bit SQLCipher key

    fun deriveKey(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
        memoryKib: Int = DEFAULT_MEMORY_KIB,
        parallelism: Int = DEFAULT_PARALLELISM,
        outputLength: Int = DEFAULT_OUTPUT_LENGTH,
    ): ByteArray = argon2idHash(
        password = password,
        salt = salt,
        iterations = iterations,
        memoryKib = memoryKib,
        parallelism = parallelism,
        outputLength = outputLength,
    )
}

internal expect fun argon2idHash(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    memoryKib: Int,
    parallelism: Int,
    outputLength: Int,
): ByteArray
