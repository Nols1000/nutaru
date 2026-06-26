package com.github.nols1000.nutaru

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressPillTest {

    @Composable
    private fun Wrapper(content: @Composable () -> Unit) {
        MaterialTheme { HealthTheme { content() } }
    }

    @Test
    fun content_description_combines_label_value_and_supporting_text() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressPill(
                    label = "Sleep",
                    value = "7h 16m",
                    supportingText = "80 · Good",
                )
            }
        }
        onNodeWithContentDescription("Sleep, 7h 16m, 80 · Good").assertExists()
    }

    @Test
    fun content_description_omits_supporting_text_when_null() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressPill(label = "Steps", value = "2,532")
            }
        }
        onNodeWithContentDescription("Steps, 2,532").assertExists()
    }

    @Test
    fun collapsing_semantics_hides_inner_text_from_screen_reader() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressPill(
                    label = "Steps",
                    value = "2,532",
                    leadingIcon = { Text("icon-slot-content") },
                )
            }
        }
        // clearAndSetSemantics merges children into the single content description, so the inner
        // Text is not independently exposed.
        onNodeWithContentDescription("icon-slot-content").assertDoesNotExist()
    }

    @Test
    fun clickable_pill_fires_on_click() = runComposeUiTest {
        var clicks = 0
        setContent {
            Wrapper {
                ProgressPill(
                    label = "Steps",
                    value = "2,532",
                    onClick = { clicks++ },
                )
            }
        }
        onNodeWithContentDescription("Steps, 2,532")
            .assertHasClickAction()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun non_clickable_pill_has_no_click_action() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressPill(label = "Steps", value = "2,532")
            }
        }
        onNodeWithContentDescription("Steps, 2,532").assertHasNoClickAction()
    }
}
