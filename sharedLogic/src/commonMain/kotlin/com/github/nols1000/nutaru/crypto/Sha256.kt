package com.github.nols1000.nutaru.crypto

/**
 * Minimal pure-Kotlin SHA-256 implementation.
 *
 * Written as a single dependency-free primitive so the BIP-39 checksum works
 * across every KMP target (JVM, iOS, JS) without per-platform crypto bindings.
 * The SHA-256 spec is RFC 6234; this is the textbook FIPS 180-4 layout.
 */
internal object Sha256 {

    fun digest(input: ByteArray): ByteArray {
        val padded = pad(input)
        val state = IntArray(8) { H[it] }
        val blocks = padded.size / 64
        val blockWords = IntArray(64)
        for (b in 0 until blocks) {
            val off = b * 64
            for (i in 0 until 16) {
                blockWords[i] = (
                    ((padded[off + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[off + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[off + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[off + i * 4 + 3].toInt() and 0xFF)
                    )
            }
            for (i in 16 until 64) {
                val s0 = rotateRight(blockWords[i - 15], 7) xor rotateRight(blockWords[i - 15], 18) xor (blockWords[i - 15] ushr 3)
                val s1 = rotateRight(blockWords[i - 2], 17) xor rotateRight(blockWords[i - 2], 19) xor (blockWords[i - 2] ushr 10)
                blockWords[i] = blockWords[i - 16] + s0 + blockWords[i - 7] + s1
            }
            var a = state[0]; var b = state[1]; var c = state[2]; var d = state[3]
            var e = state[4]; var f = state[5]; var g = state[6]; var h = state[7]
            for (i in 0 until 64) {
                val s1 = rotateRight(e, 6) xor rotateRight(e, 11) xor rotateRight(e, 25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + K[i] + blockWords[i]
                val s0 = rotateRight(a, 2) xor rotateRight(a, 13) xor rotateRight(a, 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            state[0] += a; state[1] += b; state[2] += c; state[3] += d
            state[4] += e; state[5] += f; state[6] += g; state[7] += h
        }
        return ByteArray(32) { i ->
            val word = state[i / 4]
            val byteInWord = i and 3
            (word ushr ((3 - byteInWord) * 8)).toByte()
        }
    }

    private fun pad(input: ByteArray): ByteArray {
        val originalBitLength = input.size.toLong() * 8
        val withOneByte = input.size + 1
        // Zeros needed so that (input + 0x80 + zeros) % 64 == 56.
        val zeros = ((56 - withOneByte % 64) + 64) % 64
        val totalSize = withOneByte + zeros + 8
        val padded = ByteArray(totalSize)
        for (i in input.indices) padded[i] = input[i]
        padded[input.size] = 0x80.toByte()
        // Append 64-bit big-endian bit length.
        for (i in 0 until 8) {
            padded[totalSize - 8 + i] = (originalBitLength ushr ((7 - i) * 8)).toByte()
        }
        return padded
    }

    private fun rotateRight(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))

    private val H = intArrayOf(
        0x6a09e667,
        0xbb67ae85.toInt(),
        0x3c6ef372,
        0xa54ff53a.toInt(),
        0x510e527f,
        0x9b05688c.toInt(),
        0x1f83d9ab,
        0x5be0cd19,
    )

    private val K = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
    )
}
