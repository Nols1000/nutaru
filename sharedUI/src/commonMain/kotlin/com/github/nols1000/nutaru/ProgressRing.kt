package com.github.nols1000.nutaru

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A delta marker rendered as a thin inner bar at the leading edge of the ring's progress,
 * with an optional arc label (e.g. "+34"). Pass `null` to [ProgressRing] to omit it entirely —
 * the ring then draws no inner bar and no label.
 *
 * @param fraction portion of the full ring this delta represents, `0f..1f`. Drawn ending at the
 *   current progress tip and extending backwards; coerced so it can never exceed the current fill.
 * @param label short text drawn along the inner bar (e.g. "+34"). If `null`, only the bar is drawn.
 */
@Immutable
public class ProgressRingDelta(
    public val fraction: Float,
    public val label: String? = null,
)

/**
 * A piece of text drawn along the ring's arc.
 *
 * Angles use the **ring convention** (matching [drawArcText]): `0°` = 12 o'clock, increasing
 * clockwise — `90°` = 3 o'clock, `180°` = 6 o'clock, `270°` = 9 o'clock. This is independent of
 * the ring's [ProgressRing.startAngle] (which uses Compose `drawArc` convention), so labels stay
 * at fixed visual positions regardless of where the fill begins.
 *
 * [flip] rotates glyphs 180° so text on the lower half of the ring stays upright. Use [top] /
 * [bottom] factories for the common cases; they pick [flip] for you.
 *
 * @param text the label content.
 * @param angleDeg position along the arc, ring convention (0 = top, clockwise).
 * @param flip rotate glyphs to keep them upright; `false` for the upper half, `true` for the lower.
 */
@Immutable
public class ProgressRingArcLabel(
    public val text: String,
    public val angleDeg: Float,
    public val flip: Boolean,
) {
    public companion object {
        /** Label at the top of the ring (12 o'clock), reading left-to-right. */
        public fun top(text: String): ProgressRingArcLabel =
            ProgressRingArcLabel(text, angleDeg = 0f, flip = false)

        /** Label at the bottom of the ring (6 o'clock), flipped to stay upright. */
        public fun bottom(text: String): ProgressRingArcLabel =
            ProgressRingArcLabel(text, angleDeg = 180f, flip = true)
    }
}

@Immutable
public class ProgressRingColors(
    public val track: Color,
    public val indicator: Color,
    public val deltaBar: Color,
    public val deltaLabel: Color,
    public val label: Color,
)

@Immutable
public class ProgressRingSizes(
    public val diameter: Dp,
    public val stroke: Dp,
    public val deltaStroke: Dp,
    public val labelInset: Dp,
)

public object ProgressRingDefaults {
    /**
     * Default fill start in Compose `drawArc` degrees (0 = 3 o'clock, clockwise).
     * `90f` places the start at 6 o'clock — the historical visual of this component.
     * Use `270f` to start at 12 o'clock (matches `CircularProgressIndicator`).
     */
    public const val DefaultStartAngle: Float = 90f

    /** Sweep that closes the ring into a full circle. */
    public const val FullSweep: Float = 360f

    @Composable
    public fun colors(
        track: Color = MaterialTheme.colorScheme.surfaceVariant,
        indicator: Color = MaterialTheme.colorScheme.primary,
        deltaBar: Color = MaterialTheme.colorScheme.primaryContainer,
        deltaLabel: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        label: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): ProgressRingColors = ProgressRingColors(track, indicator, deltaBar, deltaLabel, label)

    public fun sizes(
        diameter: Dp = 176.dp,
        stroke: Dp = 32.dp,
        deltaStroke: Dp = 24.dp,
        labelInset: Dp = 12.dp,
    ): ProgressRingSizes = ProgressRingSizes(diameter, stroke, deltaStroke, labelInset)

    /** Default label typography for arc text. Override [ProgressRing.labelStyle] to customize. */
    @Composable
    public fun labelStyle(): TextStyle = MaterialTheme.typography.labelMedium

    /** Default progress animation; pass `null` as [ProgressRing.animationSpec] to render instantly. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    public fun motionSpec(): AnimationSpec<Float> =
        MaterialTheme.motionScheme.defaultSpatialSpec()
}

/**
 * Circular progress gauge with curved inner labels and an optional recent-delta indicator.
 *
 * The component is metric-agnostic: it knows only a normalized [progress] in `0f..1f`, a list of
 * [arcLabels] drawn along the ring, an optional [delta], and a [center] slot. Everything visual
 * (the "58%", units, formatting) is supplied by the caller, so the same ring drives cardio %,
 * sleep score, readiness, or anything else. See [normalizedProgress] for a raw-number helper.
 *
 * @param progress normalized fill, coerced into `0f..1f`.
 * @param delta optional delta marker; when `null`, no inner bar or badge is drawn.
 * @param arcLabels text drawn along the arc; empty by default. Use [ProgressRingArcLabel.top] /
 *   [ProgressRingArcLabel.bottom] for the common top/bottom positions.
 * @param colors theme-driven colors; defaults pull from `MaterialTheme.colorScheme`.
 * @param sizes geometry of the ring and its labels.
 * @param labelStyle typography for every arc label; color is overridden per-label by [ProgressRingColors].
 * @param startAngle fill start in Compose `drawArc` degrees (0 = 3 o'clock, clockwise).
 * @param sweepAngle total arc swept by a full ring; `360f` is a closed ring, `< 360f` a gauge.
 * @param strokeCap cap applied to the track, indicator, and delta strokes.
 * @param animationSpec animates progress changes; pass `null` to render instantly.
 * @param contentDescription spoken description of the whole gauge for screen readers.
 * @param center content drawn in the inner circle (typically the headline value).
 */
@Composable
public fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    delta: ProgressRingDelta? = null,
    arcLabels: List<ProgressRingArcLabel> = emptyList(),
    colors: ProgressRingColors = ProgressRingDefaults.colors(),
    sizes: ProgressRingSizes = ProgressRingDefaults.sizes(),
    labelStyle: TextStyle = ProgressRingDefaults.labelStyle(),
    startAngle: Float = ProgressRingDefaults.DefaultStartAngle,
    sweepAngle: Float = ProgressRingDefaults.FullSweep,
    strokeCap: StrokeCap = StrokeCap.Round,
    animationSpec: AnimationSpec<Float>? = ProgressRingDefaults.motionSpec(),
    contentDescription: String? = null,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = (if (animationSpec != null) {
        animateFloatAsState(target, animationSpec, label = "progress").value
    } else {
        target
    }).coerceIn(0f, 1f)

    val measurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .size(sizes.diameter)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(animated, 0f..1f)
                contentDescription?.let { this.contentDescription = it }
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val strokePx = with(density) { sizes.stroke.toPx() }
        val deltaStrokePx = with(density) { sizes.deltaStroke.toPx() }
        val labelInsetPx = with(density) { sizes.labelInset.toPx() }
        val diameterPx = with(density) { sizes.diameter.toPx() }

        val ringRadius = (diameterPx - strokePx) / 2f
        val centerOffset = Offset(diameterPx / 2f, diameterPx / 2f)
        val labelRadius = ringRadius - strokePx / 2f - labelInsetPx

        Canvas(Modifier.size(sizes.diameter)) {
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)

            // Track
            drawArc(
                color = colors.track,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = strokeCap),
            )

            // Progress
            if (animated > 0f) {
                drawArc(
                    color = colors.indicator,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = strokeCap),
                )
            }

            // Delta inner bar — only when supplied
            if (delta != null && delta.fraction > 0f) {
                val (deltaStart, deltaSweep) = deltaArc(startAngle, sweepAngle, animated, delta.fraction)
                val innerTopLeft = Offset(centerOffset.x - ringRadius, centerOffset.y - ringRadius)
                val innerDiameter = ringRadius * 2f
                drawArc(
                    color = colors.deltaBar,
                    startAngle = deltaStart,
                    sweepAngle = deltaSweep,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = Size(innerDiameter, innerDiameter),
                    style = Stroke(width = deltaStrokePx, cap = strokeCap),
                )

                delta.label?.let { label ->
                    // Bridge drawArc degrees (0 = 3 o'clock) -> ring/ArcText degrees (0 = 12 o'clock): +90.
                    val deltaCenterRingDeg = deltaStart + deltaSweep / 2f + 90f
                    drawArcText(
                        measurer = measurer,
                        text = label,
                        style = labelStyle,
                        color = colors.deltaLabel,
                        center = centerOffset,
                        radius = ringRadius,
                        centerAngleDeg = deltaCenterRingDeg,
                        flip = arcLabelFlip(deltaCenterRingDeg),
                    )
                }
            }

            // Curved arc labels, sitting just inside the stroke
            for (arcLabel in arcLabels) {
                if (arcLabel.text.isEmpty()) continue
                drawArcText(
                    measurer = measurer,
                    text = arcLabel.text,
                    style = labelStyle,
                    color = colors.label,
                    center = centerOffset,
                    radius = labelRadius,
                    centerAngleDeg = arcLabel.angleDeg,
                    flip = arcLabel.flip,
                )
            }
        }

        center()
    }
}

/**
 * Normalizes a raw [value] against a [target] into the `0f..1f` range [ProgressRing] expects.
 *
 * Returns `0f` for a non-positive [target] (avoiding divide-by-zero). Negative [value]s and
 * overshoot are clamped. Useful for "222 of 384" style metrics so the call site stays arithmetic-free:
 *
 * ```
 * ProgressRing(progress = normalizedProgress(222f, 384f), ...)
 * ```
 */
public fun normalizedProgress(value: Float, target: Float): Float =
    if (target > 0f) (value / target).coerceIn(0f, 1f) else 0f

/**
 * Clamps a delta fraction so it never exceeds the current [progress] fill or drops below zero.
 */
internal fun coerceDeltaFraction(deltaFraction: Float, progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return deltaFraction.coerceIn(0f, p)
}

/**
 * Computes the `drawArc` geometry (start angle, sweep) of the delta bar so that it ends at the
 * current progress tip and extends backwards by [deltaFraction] of the full ring.
 *
 * Inputs use `drawArc` degrees (0 = 3 o'clock, clockwise); so does the result.
 */
internal fun deltaArc(
    startAngle: Float,
    sweepAngle: Float,
    progress: Float,
    deltaFraction: Float,
): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    val d = coerceDeltaFraction(deltaFraction, p)
    val sweep = sweepAngle * d
    val start = startAngle + sweepAngle * p - sweep
    return start to sweep
}

/**
 * Decides whether an arc label at [angleDeg] (ring convention: 0 = 12 o'clock, clockwise) should
 * be flipped 180° to keep its glyphs upright. Labels on the lower half (`90° < a < 270°`) flip.
 */
internal fun arcLabelFlip(angleDeg: Float): Boolean {
    val a = ((angleDeg % 360f) + 360f) % 360f
    return a > 90f && a < 270f
}
