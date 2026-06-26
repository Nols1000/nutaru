package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressRingMathTest {

    // ---------- normalizedProgress ----------

    @Test
    fun normalizedProgress_divides_value_by_target() {
        assertEquals(0.5f, normalizedProgress(1f, 2f))
        assertEquals(0.25f, normalizedProgress(222f, 888f))
    }

    @Test
    fun normalizedProgress_clamps_above_one() {
        assertEquals(1f, normalizedProgress(200f, 100f))
    }

    @Test
    fun normalizedProgress_clamps_below_zero() {
        assertEquals(0f, normalizedProgress(-5f, 100f))
    }

    @Test
    fun normalizedProgress_is_zero_for_non_positive_target() {
        assertEquals(0f, normalizedProgress(50f, 0f))
        assertEquals(0f, normalizedProgress(50f, -10f))
    }

    // ---------- coerceDeltaFraction ----------

    @Test
    fun coerceDeltaFraction_passes_through_when_in_range() {
        assertEquals(0.3f, coerceDeltaFraction(0.3f, 0.5f))
    }

    @Test
    fun coerceDeltaFraction_clamps_to_progress() {
        assertEquals(0.5f, coerceDeltaFraction(0.9f, 0.5f))
    }

    @Test
    fun coerceDeltaFraction_clamps_to_zero() {
        assertEquals(0f, coerceDeltaFraction(-0.2f, 0.5f))
    }

    @Test
    fun coerceDeltaFraction_clamps_when_progress_itself_exceeds_one() {
        assertEquals(1f, coerceDeltaFraction(5f, 2f))
    }

    @Test
    fun coerceDeltaFraction_is_zero_when_progress_is_zero() {
        assertEquals(0f, coerceDeltaFraction(0.4f, 0f))
    }

    // ---------- deltaArc ----------

    @Test
    fun deltaArc_sits_at_progress_tip_and_extends_backwards() {
        // startAngle = 0 (3 o'clock), sweep = 360, progress = 0.5 (180°), delta = 0.25 (90°)
        // Expected: ends at 0 + 360*0.5 = 180°, extends back by 90°, so start = 90, sweep = 90.
        val (start, sweep) = deltaArc(startAngle = 0f, sweepAngle = 360f, progress = 0.5f, deltaFraction = 0.25f)
        assertEquals(90f, start)
        assertEquals(90f, sweep)
    }

    @Test
    fun deltaArc_sweep_is_delta_fraction_of_full_sweep() {
        val (_, sweep) = deltaArc(startAngle = 90f, sweepAngle = 180f, progress = 1f, deltaFraction = 0.5f)
        assertEquals(90f, sweep)
    }

    @Test
    fun deltaArc_is_empty_when_delta_is_zero() {
        val (start, sweep) = deltaArc(startAngle = 0f, sweepAngle = 360f, progress = 0.8f, deltaFraction = 0f)
        assertEquals(360f * 0.8f, start)
        assertEquals(0f, sweep)
    }

    @Test
    fun deltaArc_delta_is_clamped_to_progress() {
        // progress = 0.2 (72°), delta = 1.0 → clamped to 0.2 → sweep = 72°, start = 0.
        val (start, sweep) = deltaArc(startAngle = 0f, sweepAngle = 360f, progress = 0.2f, deltaFraction = 1f)
        assertEquals(0f, start)
        assertEquals(72f, sweep)
    }

    // ---------- arcLabelFlip ----------

    @Test
    fun arcLabelFlip_lower_half_flips() {
        assertTrue(arcLabelFlip(135f))
        assertTrue(arcLabelFlip(180f))
        assertTrue(arcLabelFlip(225f))
    }

    @Test
    fun arcLabelFlip_upper_half_does_not_flip() {
        assertFalse(arcLabelFlip(0f))
        assertFalse(arcLabelFlip(45f))
        assertFalse(arcLabelFlip(315f))
    }

    @Test
    fun arcLabelFlip_wraps_negative_angles() {
        // -90° ≡ 270° (left edge, 9 o'clock) — boundary, no flip.
        assertFalse(arcLabelFlip(-90f))
        // -180° ≡ 180° (bottom) — flips.
        assertTrue(arcLabelFlip(-180f))
    }

    @Test
    fun arcLabelFlip_wraps_angles_above_360() {
        // 360° ≡ 0° → no flip.
        assertFalse(arcLabelFlip(360f))
        // 540° ≡ 180° → flips.
        assertTrue(arcLabelFlip(540f))
    }

    @Test
    fun arcLabelFlip_boundaries_do_not_flip() {
        // Exactly 90° and 270° are the half-boundaries; glyphs are upright, no flip.
        assertFalse(arcLabelFlip(90f))
        assertFalse(arcLabelFlip(270f))
    }
}
