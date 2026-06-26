package com.github.nols1000.nutaru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Picks a 1-based word index for the confirm-saved gate. Default impl draws
 * uniformly from [1, 12] via [kotlin.random.Random]. Kept as a function seam
 * so tests can pin the index and assert the gate's accept/reject behavior.
 *
 * The challenge type is "type the Nth word" rather than a multiple-choice tap
 * because typing proves recall; a tap could be guessed (1/12) or fumbled. This
 * also sidestates the "copy-to-clipboard" path entirely — there's nothing to
 * copy from the challenge field.
 */
typealias WordIndexPicker = () -> Int

/** Uniformly random 1..12 — the production picker. */
val DefaultWordIndexPicker: WordIndexPicker = { (1..12).random() }

/**
 * Confirm-saved gate logic, extracted from the composable so it's testable
 * without Compose. The gate accepts only the exact (case-insensitive) word at
 * [challengeIndex] (1-based). Returns true on match, false otherwise.
 */
fun confirmSavedMatch(challengeIndex: Int, mnemonic: List<String>, entry: String): Boolean {
    if (challengeIndex !in 1..mnemonic.size) return false
    val expected = mnemonic[challengeIndex - 1].trim()
    return expected.equals(entry.trim(), ignoreCase = true)
}

/**
 * Recovery mnemonic reveal screen — the final onboarding step and the forced
 * relaunch destination if the user killed the app before acknowledging.
 *
 * Shows all 12 words in stable index order. Copy-to-clipboard is intentionally
 * disabled (no `SelectionContainer`, no copy affordance) to force the user to
 * write the words down manually. The confirm-saved gate prompts for one
 * randomly-chosen word by its 1-based position; a wrong entry blocks
 * progression and shows an error, the user can retry.
 *
 * On [onAcknowledged] the caller persists the acknowledgment flag so relaunch
 * goes straight to Home.
 */
@Composable
fun RecoveryRevealScreen(
    mnemonic: List<String>,
    onAcknowledged: () -> Unit,
    wordIndexPicker: WordIndexPicker = DefaultWordIndexPicker,
) {
    // The challenge index is chosen once per composition and held across
    // retries so a wrong answer doesn't reshuffle the target.
    var challengeIndex by remember { mutableStateOf(wordIndexPicker()) }
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Text("Your recovery mnemonic", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "These 12 words are the only way to recover your data. Write them down " +
            "on paper and store them somewhere safe. No one else can recover them for you.",
        style = MaterialTheme.typography.bodyMedium,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mnemonic.forEachIndexed { i, word ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${i + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.wrapContentWidth(),
                    )
                    // No SelectionContainer: copy-to-clipboard is disabled by design
                    // (issue-04, criterion 1) so the user transcribes manually.
                    Text(word, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    Text(
        "To confirm you've saved it, type word #$challengeIndex.",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    OutlinedTextField(
        value = entry,
        onValueChange = { entry = it; error = false },
        label = { Text("Word #$challengeIndex") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        isError = error,
        supportingText = { if (error) Text("That doesn't match. Check your spelling and try again.") },
        modifier = Modifier.fillMaxWidth(),
    )

    Button(
        onClick = {
            if (confirmSavedMatch(challengeIndex, mnemonic, entry)) {
                onAcknowledged()
            } else {
                error = true
            }
        },
        enabled = entry.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("I've saved it") }
}
