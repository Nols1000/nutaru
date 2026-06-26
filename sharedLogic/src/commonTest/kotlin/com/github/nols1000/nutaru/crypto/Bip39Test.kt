package com.github.nols1000.nutaru.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * BIP-39 mnemonic self-check. The acceptance criteria call for sufficient
 * entropy + a 12-word mnemonic; this verifies the entropy→mnemonic→entropy
 * loop and a known test vector from the BIP-39 reference set.
 */
class Bip39Test {

    @Test
    fun mnemonic_has_exactly_12_words_from_the_wordlist() {
        val mnemonic = Bip39.generateMnemonic()
        assertEquals(12, mnemonic.size)
        assertTrue(mnemonic.all { it in BIP39_ENGLISH })
    }

    @Test
    fun different_calls_produce_different_mnemonics() {
        // 128 bits of entropy: collisions are astronomically unlikely.
        val a = Bip39.generateMnemonic()
        val b = Bip39.generateMnemonic()
        assertNotEquals(a, b)
    }

    @Test
    fun entropy_round_trips_through_mnemonic() {
        val entropy = byteArrayOf(
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        )
        val mnemonic = Bip39.generateMnemonic(entropy)
        val recovered = Bip39.mnemonicToEntropy(mnemonic)
        assertEquals(entropy.toList(), recovered.toList())
    }

    @Test
    fun known_test_vector_all_zero_entropy() {
        // BIP-39 reference: all-zero entropy → "abandon ... about".
        val mnemonic = Bip39.generateMnemonic(ByteArray(16))
        assertEquals(
            listOf("abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "about"),
            mnemonic,
        )
    }

    @Test
    fun sha256_matches_known_vector() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        val hash = Sha256.digest("abc".encodeToByteArray())
        val hex = hash.joinToString("") { byteToHex(it) }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex)
    }

    private fun byteToHex(b: Byte): String {
        val v = b.toInt() and 0xFF
        val chars = "0123456789abcdef"
        return chars[v ushr 4].toString() + chars[v and 0x0F]
    }
}
