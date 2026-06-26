package com.github.nols1000.nutaru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Color tokens for a [ProgressPill].
 *
 * As with [ProgressRingColors], these carry no semantic meaning of their own — they're the slots a
 * pill needs (container, label, value, supporting text, progress track/indicator and the
 * leading-icon badge background), sourced from `MaterialTheme.colorScheme` by default. Pass a
 * tailored instance per metric to tint pills (e.g. teal for steps, lilac for sleep).
 */
@Immutable
public class ProgressPillColors(
    public val container: Color,
    public val label: Color,
    public val value: Color,
    public val supportingText: Color,
    public val progressTrack: Color,
    public val progressIndicator: Color,
    public val leadingIconContainer: Color,
)

/**
 * Geometry tokens for a [ProgressPill]. Mirrors [ProgressRingSizes]: every dp that influences the
 * pill's layout lives here so a screen can rescale pills uniformly.
 *
 * The leading-icon badge is sized to fill the pill's content area vertically
 * (`[height] - 2 × [verticalPadding]`) and shaped with `RoundedCornerShape(badgeHeight / 2)`, so
 * its top and bottom are always a full capsule — fully rounded — regardless of the chosen tokens.
 */
@Immutable
public class ProgressPillSizes(
    public val height: Dp,
    public val horizontalPadding: Dp,
    public val verticalPadding: Dp,
    public val leadingIconWidth: Dp,
    public val leadingIconSize: Dp,
    public val progressBarHeight: Dp,
    public val cornerRadius: Dp,
)

public object ProgressPillDefaults {
    @Composable
    public fun colors(
        container: Color = MaterialTheme.colorScheme.surfaceVariant,
        label: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        value: Color = MaterialTheme.colorScheme.onSurface,
        supportingText: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        progressTrack: Color = MaterialTheme.colorScheme.surface,
        progressIndicator: Color = MaterialTheme.colorScheme.primary,
        leadingIconContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    ): ProgressPillColors = ProgressPillColors(
        container = container,
        label = label,
        value = value,
        supportingText = supportingText,
        progressTrack = progressTrack,
        progressIndicator = progressIndicator,
        leadingIconContainer = leadingIconContainer,
    )

    public fun sizes(
        height: Dp = 48.dp,
        horizontalPadding: Dp = 4.dp,
        verticalPadding: Dp = 4.dp,
        leadingIconWidth: Dp = 32.dp,
        leadingIconSize: Dp = 20.dp,
        progressBarHeight: Dp = 4.dp,
        cornerRadius: Dp = 20.dp,
    ): ProgressPillSizes = ProgressPillSizes(
        height = height,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        leadingIconWidth = leadingIconWidth,
        leadingIconSize = leadingIconSize,
        progressBarHeight = progressBarHeight,
        cornerRadius = cornerRadius,
    )
}

/**
 * Compact pill summarizing a single metric: an optional [leadingIcon] badge, a small [label], a
 * prominent [value], optional [supportingText] (e.g. "80 · Good"), and an optional inline
 * [progress] bar.
 *
 * Like [ProgressRing], it carries no metric-specific logic — values arrive pre-formatted as strings.
 * Both the [progress] bar and [leadingIcon] follow the "render only if set" rule: pass `null` and
 * neither is drawn.
 *
 * The leading-icon badge is rendered by this component (not the caller): it fills the pill's
 * content area vertically and uses `RoundedCornerShape(badgeHeight / 2)`, so the top and bottom
 * are always fully rounded. Callers supply only the icon content (e.g. an `Icon`) via [leadingIcon];
 * the badge background comes from [ProgressPillColors.leadingIconContainer].
 *
 * @param progress normalized `0f..1f`; the inline bar renders only when non-null.
 * @param onClick makes the pill tappable; when `null` the pill is presentational.
 */
@Composable
public fun ProgressPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    progress: Float? = null,
    colors: ProgressPillColors = ProgressPillDefaults.colors(),
    sizes: ProgressPillSizes = ProgressPillDefaults.sizes(),
    onClick: (() -> Unit)? = null,
) {
    val description = buildString {
        append(label)
        append(", ")
        append(value)
        supportingText?.let {
            append(", ")
            append(it)
        }
    }

    val baseModifier = modifier
        .fillMaxWidth()
        .height(sizes.height)

    val shape = RoundedCornerShape(sizes.cornerRadius)

    // The semantics-collapsing Box lives INSIDE the Surface so the Surface's clickable (when
    // `onClick != null`) keeps its own onClick action / Role on the parent node — putting
    // `clearAndSetSemantics` on the Surface itself would wipe the click action and break keyboard
    // & screen-reader activation. The merged parent inherits the Box's contentDescription.
    val content: @Composable () -> Unit = {
        Box(Modifier.clearAndSetSemantics { contentDescription = description }) {
            ProgressPillContent(
                label = label,
                value = value,
                supportingText = supportingText,
                leadingIcon = leadingIcon,
                progress = progress,
                colors = colors,
                sizes = sizes,
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = baseModifier,
            shape = shape,
            color = colors.container,
            content = content,
        )
    } else {
        Surface(
            modifier = baseModifier,
            shape = shape,
            color = colors.container,
            content = content,
        )
    }
}

@Composable
private fun ProgressPillContent(
    label: String,
    value: String,
    supportingText: String?,
    leadingIcon: @Composable (() -> Unit)?,
    progress: Float?,
    colors: ProgressPillColors,
    sizes: ProgressPillSizes,
) {
    val dimens = HealthTheme.dimens
    val badgeHeight = sizes.height - sizes.verticalPadding * 2
    val badgeShape = RoundedCornerShape(badgeHeight / 2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = sizes.horizontalPadding, vertical = sizes.verticalPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            if (leadingIcon != null) {
                Surface(
                    shape = badgeShape,
                    color = colors.leadingIconContainer,
                    modifier = Modifier.size(width = sizes.leadingIconWidth, height = badgeHeight),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingIcon()
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        lineHeight = 19.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.value,
                    maxLines = 1,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                        ),
                        color = colors.supportingText,
                        maxLines = 1,
                    )
                }
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceXs)
                    .height(sizes.progressBarHeight),
                color = colors.progressIndicator,
                trackColor = colors.progressTrack,
            )
        }
    }
}
