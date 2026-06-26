package com.github.nols1000.nutaru

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and radius tokens shared across the library.
 *
 * Components never hard-code dimensions; they read from here (or from a per-component
 * `*Sizes` holder seeded from here) so that an app can rescale the whole system from
 * one place — e.g. for a compact watch layout vs. a phone.
 */
@Immutable
public class HealthDimens(
    public val spaceXs: Dp = 4.dp,
    public val spaceSm: Dp = 8.dp,
    public val spaceMd: Dp = 12.dp,
    public val spaceLg: Dp = 16.dp,
    public val spaceXl: Dp = 24.dp,
    public val pillCornerRadius: Dp = 14.dp,
    public val minTouchTarget: Dp = 48.dp,
)

public val LocalHealthDimens: ProvidableCompositionLocal<HealthDimens> =
    staticCompositionLocalOf { HealthDimens() }

/**
 * Optional convenience wrapper. The library does not require it — every component falls back to
 * [MaterialTheme] + sensible defaults — but providing it lets you override library-wide tokens
 * without touching individual call sites.
 */
@Composable
public fun HealthTheme(
    dimens: HealthDimens = HealthTheme.dimens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHealthDimens provides dimens,
        content = content,
    )
}

public object HealthTheme {
    public val dimens: HealthDimens
        @Composable @ReadOnlyComposable get() = LocalHealthDimens.current
}
