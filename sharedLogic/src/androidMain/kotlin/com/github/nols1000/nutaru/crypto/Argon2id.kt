package com.github.nols1000.nutaru.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

internal actual fun argon2idHash(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    memoryKib: Int,
    parallelism: Int,
    outputLength: Int,
): ByteArray {
    val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
        .withVersion(Argon2Parameters.ARGON2_VERSION_13)
        .withIterations(iterations)
        .withMemoryAsKB(memoryKib)
        .withParallelism(parallelism)
        .withSalt(salt)
        .build()

    val generator = Argon2BytesGenerator().apply { init(params) }
    val out = ByteArray(outputLength)
    generator.generateBytes(out, password)
    return out
}
