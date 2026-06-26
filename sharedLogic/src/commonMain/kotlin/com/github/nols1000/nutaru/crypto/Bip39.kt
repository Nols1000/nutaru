package com.github.nols1000.nutaru.crypto

/**
 * BIP-39 mnemonic generation over the official English wordlist.
 *
 * Generates a 12-word mnemonic from 128 bits of caller-supplied entropy
 * (use [secureRandomBytes]). The 4-bit checksum is SHA-256(entropy) >> 4,
 * giving the standard 132-bit / 12 * 11-bit encoding.
 *
 * Derivation of the SQLCipher key from the mnemonic lives in [Argon2id]; BIP-39
 * itself only concerns itself with the mnemonic step.
 */
object Bip39 {

    private const val ENTROPY_BYTES_12_WORDS = 16
    private const val WORDS_FOR_12 = 12
    private const val BITS_PER_WORD = 11

    /** Generate a 12-word mnemonic from freshly supplied entropy. */
    fun generateMnemonic(entropy: ByteArray = secureRandomBytes(ENTROPY_BYTES_12_WORDS)): List<String> {
        require(entropy.size == ENTROPY_BYTES_12_WORDS) {
            "12-word mnemonic requires 16 bytes of entropy; got ${entropy.size}"
        }

        val checksumByte = Sha256.digest(entropy)[0]
        // First 4 bits of the SHA-256 checksum become bits 128..131.
        val combined = ByteArray(ENTROPY_BYTES_12_WORDS + 1)
        for (i in entropy.indices) combined[i] = entropy[i]
        combined[ENTROPY_BYTES_12_WORDS] = checksumByte

        val words = ArrayList<String>(WORDS_FOR_12)
        var bitIndex = 0
        val totalBits = (ENTROPY_BYTES_12_WORDS + 1) * 8
        repeat(WORDS_FOR_12) {
            require(bitIndex + BITS_PER_WORD <= totalBits)
            val wordIndex = readBits(combined, bitIndex, BITS_PER_WORD)
            words.add(BIP39_ENGLISH[wordIndex])
            bitIndex += BITS_PER_WORD
        }
        return words
    }

    /** Decode a 12-word mnemonic back to its entropy bytes. */
    fun mnemonicToEntropy(mnemonic: List<String>): ByteArray {
        require(mnemonic.size == WORDS_FOR_12) { "12-word mnemonic required; got ${mnemonic.size}" }
        val bitAccumulator = BooleanArray(ENTROPY_BYTES_12_WORDS * 8 + 4)
        var bitIndex = 0
        for (word in mnemonic) {
            val wordIndex = BIP39_ENGLISH.indexOf(word.lowercase())
            require(wordIndex >= 0) { "Not a BIP-39 word: $word" }
            for (b in BITS_PER_WORD - 1 downTo 0) {
                bitAccumulator[bitIndex++] = ((wordIndex ushr b) and 1) == 1
            }
        }
        val entropy = ByteArray(ENTROPY_BYTES_12_WORDS)
        for (i in 0 until ENTROPY_BYTES_12_WORDS) {
            entropy[i] = (
                (if (bitAccumulator[i * 8]) 0x80 else 0) or
                    (if (bitAccumulator[i * 8 + 1]) 0x40 else 0) or
                    (if (bitAccumulator[i * 8 + 2]) 0x20 else 0) or
                    (if (bitAccumulator[i * 8 + 3]) 0x10 else 0) or
                    (if (bitAccumulator[i * 8 + 4]) 0x08 else 0) or
                    (if (bitAccumulator[i * 8 + 5]) 0x04 else 0) or
                    (if (bitAccumulator[i * 8 + 6]) 0x02 else 0) or
                    (if (bitAccumulator[i * 8 + 7]) 0x01 else 0)
                ).toByte()
        }
        return entropy
    }

    private fun readBits(bytes: ByteArray, offset: Int, count: Int): Int {
        var value = 0
        repeat(count) { i ->
            val bitPos = offset + i
            val byte = bytes[bitPos ushr 3].toInt() and 0xFF
            val bit = (byte shr (7 - (bitPos and 7))) and 1
            value = (value shl 1) or bit
        }
        return value
    }
}
