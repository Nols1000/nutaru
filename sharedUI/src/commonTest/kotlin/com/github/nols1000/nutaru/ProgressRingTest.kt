package com.github.nols1000.nutaru

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRingTest {

    @Composable
    private fun Wrapper(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    private fun textIs(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(value)))

    private fun contentDescriptionIs(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(value))

    private fun progressIs(value: Float): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(value, 0f..1f))

    @Test
    fun exposes_content_description() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressRing(
                    progress = 0.58f,
                    animationSpec = null,
                    contentDescription = "Weekly cardio, 58%",
                )
            }
        }
        onNode(contentDescriptionIs("Weekly cardio, 58%")).assertExists()
    }

    @Test
    fun renders_center_slot() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressRing(
                    progress = 0.5f,
                    animationSpec = null,
                    center = { Text("58%") },
                )
            }
        }
        onNode(textIs("58%")).assertExists()
    }

    @Test
    fun progress_range_info_reflects_normalized_progress() = runComposeUiTest {
        setContent {
            Wrapper {
                ProgressRing(
                    progress = normalizedProgress(3f, 4f),
                    animationSpec = null,
                    contentDescription = "ring",
                )
            }
        }
        onNode(progressIs(0.75f)).assertExists()
    }
}
