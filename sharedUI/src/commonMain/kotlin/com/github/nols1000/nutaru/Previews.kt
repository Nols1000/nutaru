package com.github.nols1000.nutaru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthui.header.HeroHeader

@Preview
@Composable
internal fun CardioHeaderPreview() {
    MaterialTheme {
        HealthTheme {
            Surface {
                HeroHeader(
                    modifier = Modifier.padding(16.dp),
                    hero = {
                        ProgressRing(
                            progress = normalizedProgress(value = 222f, target = 384f),
                            delta = ProgressRingDelta(fraction = 34f / 384f, label = "+34"),
                            arcLabels = listOf(
                                ProgressRingArcLabel.top("Weekly cardio"),
                                ProgressRingArcLabel.bottom("222 of 384"),
                            ),
                        ) {
                            Text(
                                text = "58%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                    metrics = {
                        ProgressPill(
                            label = "Steps",
                            value = "2,532",
                            colors = ProgressPillDefaults.colors(
                                container = Color(0xFF86E8DC),
                                label = Color(0xFF16474A),
                                value = Color(0xFF16474A),
                                leadingIconContainer = Color(0xFFB8F3EA),
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = Color(0xFF066A67),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        ProgressPill(
                            label = "Readiness",
                            value = "89",
                            colors = ProgressPillDefaults.colors(
                                container = Color(0xFFC1F2BE),
                                label = Color(0xFF135B2E),
                                value = Color(0xFF135B2E),
                                leadingIconContainer = Color(0xFFDAF8D8),
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = Color(0xFF0D6B34),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        ProgressPill(
                            label = "Sleep",
                            value = "7h 16m",
                            supportingText = "80 · Good",
                            colors = ProgressPillDefaults.colors(
                                container = Color(0xFFE9D2FA),
                                label = Color(0xFF5B3A88),
                                value = Color(0xFF5B3A88),
                                leadingIconContainer = Color(0xFFF3E3FE),
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.NightsStay,
                                    contentDescription = null,
                                    tint = Color(0xFF5A2E93),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
internal fun GaugeNoChangePreview() {
    MaterialTheme {
        HealthTheme {
            Surface {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // No `delta` passed -> no inner bar, no badge.
                    ProgressRing(
                        progress = 0.62f,
                        arcLabels = listOf(
                            ProgressRingArcLabel.top("Sleep score"),
                            ProgressRingArcLabel.bottom("Good"),
                        ),
                    ) {
                        Text("80", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}
