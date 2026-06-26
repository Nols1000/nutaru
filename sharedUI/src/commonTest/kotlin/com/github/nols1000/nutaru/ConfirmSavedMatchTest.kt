package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the confirm-saved gate logic (issue-04, criterion 2). The composable
 * delegates accept/reject to [confirmSavedMatch]; these cases must hold for
 * the gate to block progression on wrong entry while accepting correct entry.
 */
class ConfirmSavedMatchTest {

    private val mnemonic = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident",
    )

    @Test
    fun exact_match_at_challenge_index_accepts() {
        assertTrue(confirmSavedMatch(challengeIndex = 1, mnemonic = mnemonic, entry = "abandon"))
        assertTrue(confirmSavedMatch(challengeIndex = 12, mnemonic = mnemonic, entry = "accident"))
        assertTrue(confirmSavedMatch(challengeIndex = 7, mnemonic = mnemonic, entry = "absorb"))
    }

    @Test
    fun case_insensitive_match_accepts() {
        assertTrue(confirmSavedMatch(1, mnemonic, "ABANDON"))
        assertTrue(confirmSavedMatch(1, mnemonic, "Abandon"))
    }

    @Test
    fun surrounding_whitespace_is_trimmed() {
        assertTrue(confirmSavedMatch(3, mnemonic, "  able  "))
    }

    @Test
    fun wrong_word_rejects() {
        assertFalse(confirmSavedMatch(1, mnemonic, "ability"), "Word #2 is not word #1.")
        assertFalse(confirmSavedMatch(12, mnemonic, "abandon"), "Word #1 is not word #12.")
    }

    @Test
    fun empty_entry_rejects() {
        assertFalse(confirmSavedMatch(1, mnemonic, ""))
        assertFalse(confirmSavedMatch(1, mnemonic, "   "))
    }

    @Test
    fun out_of_range_challenge_index_rejects_safely() {
        assertFalse(confirmSavedMatch(0, mnemonic, "abandon"))
        assertFalse(confirmSavedMatch(13, mnemonic, "abandon"))
    }

    @Test
    fun partial_word_rejects() {
        assertFalse(confirmSavedMatch(1, mnemonic, "abando"))
        assertFalse(confirmSavedMatch(1, mnemonic, "abandoned"))
    }
}
