package com.example.healthui.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.nols1000.nutaru.HealthTheme

/**
 * Prominent top region of a screen: a [hero] visual (typically a [com.github.nols1000.nutaru.ProgressRing])
 * beside a column of supporting [metrics] (typically [com.github.nols1000.nutaru.ProgressPill]s).
 *
 * Purely a layout primitive — it owns no metric data and imposes no styling on its slots, so it
 * composes the same way whether the hero is a ring, a sparkline, or a custom illustration. Both
 * slots are optional-by-construction: pass `null` for [metrics] to render a hero-only header.
 *
 * @param hero the focal visual.
 * @param metrics scoped slot for the supporting column; `null` renders the hero alone.
 * @param spacing gap between hero and metrics column.
 */
@Composable
public fun HeroHeader(
    hero: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    metrics: (@Composable ColumnScope.() -> Unit)? = null,
    spacing: Dp = HealthTheme.dimens.spaceLg,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = verticalAlignment,
    ) {
        hero()
        if (metrics != null) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HealthTheme.dimens.spaceSm),
                content = metrics,
            )
        }
    }
}

/** Convenience overload taking a plain row of metric slots instead of a scoped column. */
@Composable
public fun HeroHeaderRow(
    hero: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = HealthTheme.dimens.spaceLg,
    metrics: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            hero()
            metrics()
        },
    )
}
