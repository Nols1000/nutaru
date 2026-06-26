package com.github.nols1000.nutaru

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws [text] along a circular arc, centered on [centerAngleDeg].
 *
 * Angles are measured in degrees, **clockwise from 12 o'clock** (0 = top, 90 = right,
 * 180 = bottom, 270 = left) — the natural mental model for a ring gauge.
 *
 * Each glyph is measured, placed at its own angular slot, and rotated so its baseline is
 * tangent to the circle. This is fully multiplatform (no `nativeCanvas` / `Paint`), at the
 * cost of cross-glyph kerning — which is imperceptible for the short labels this is built for.
 *
 * @param flip set `true` for text on the lower half so glyphs read upright instead of upside-down.
 * @param letterSpacingDeg extra angular padding between glyphs, in degrees.
 */
internal fun DrawScope.drawArcText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    color: Color,
    center: Offset,
    radius: Float,
    centerAngleDeg: Float,
    flip: Boolean,
    letterSpacingDeg: Float = 0f,
) {
    if (text.isEmpty() || radius <= 0f) return

    val layouts = text.map { measurer.measure(AnnotatedString(it.toString()), style) }
    val degPerPx = 180f / (PI.toFloat() * radius)
    val glyphAngles = layouts.map { it.size.width * degPerPx }
    val totalAngle = glyphAngles.sum() + letterSpacingDeg * (text.length - 1).coerceAtLeast(0)

    // Reading direction: top text walks clockwise (+), bottom text walks counter-clockwise (-)
    // so that, after the 180° flip, characters still read left-to-right.
    val dir = if (flip) -1f else 1f
    var edge = centerAngleDeg - dir * totalAngle / 2f

    layouts.forEachIndexed { i, layout ->
        val glyphAngle = glyphAngles[i]
        val a = edge + dir * glyphAngle / 2f
        val rad = a * (PI.toFloat() / 180f)

        // 0° = top, clockwise. Screen y grows downward, hence the -cos on y.
        val px = center.x + radius * sin(rad)
        val py = center.y - radius * cos(rad)

        rotate(degrees = if (flip) a + 180f else a, pivot = Offset(px, py)) {
            drawText(
                textLayoutResult = layout,
                color = color,
                topLeft = Offset(px - layout.size.width / 2f, py - layout.size.height / 2f),
            )
        }
        edge += dir * (glyphAngle + letterSpacingDeg)
    }
}
