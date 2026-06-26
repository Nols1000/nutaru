package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mifflin-St Jeor + activity factor + goal delta self-check (no DB, no platform).
 *
 * Covers the non-trivial pieces of the Plan screen's target calculation:
 *   - BMR sex branch (+5 male / -161 female)
 *   - activity factor mapping across all five levels
 *   - goal delta mapping across lose/maintain/gain
 *   - macro split (protein 2g/kg, fat 25% kcal, carbs remainder)
 *
 * Vectors are hand-computed from the formulas in [TargetCalc] so a regression in
 * any constant surfaces here.
 */
class TargetCalcTest {

    @Test
    fun male_bmr_uses_plus_5_constant() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val p = Profile(30, Sex.MALE, 180, 80.0, Goal.MAINTAIN, ActivityLevel.MODERATE)
        assertEquals(1780.0, TargetCalc.bmr(p), 0.001)
    }

    @Test
    fun female_bmr_uses_minus_161_constant() {
        // 10*60 + 6.25*165 - 5*28 - 161 = 600 + 1031.25 - 140 - 161 = 1330.25
        val p = Profile(28, Sex.FEMALE, 165, 60.0, Goal.LOSE, ActivityLevel.LIGHT)
        assertEquals(1330.25, TargetCalc.bmr(p), 0.001)
    }

    @Test
    fun activity_factors_match_standard_mifflin_multipliers() {
        assertEquals(1.2, TargetCalc.activityFactor(ActivityLevel.SEDENTARY))
        assertEquals(1.375, TargetCalc.activityFactor(ActivityLevel.LIGHT))
        assertEquals(1.55, TargetCalc.activityFactor(ActivityLevel.MODERATE))
        assertEquals(1.725, TargetCalc.activityFactor(ActivityLevel.VERY))
        assertEquals(1.9, TargetCalc.activityFactor(ActivityLevel.EXTRA))
    }

    @Test
    fun goal_deltas_are_lose_minus_500_maintain_zero_gain_plus_500() {
        assertEquals(-500.0, TargetCalc.goalDelta(Goal.LOSE))
        assertEquals(0.0, TargetCalc.goalDelta(Goal.MAINTAIN))
        assertEquals(500.0, TargetCalc.goalDelta(Goal.GAIN))
    }

    @Test
    fun calculate_male_moderate_maintain_sums_to_tdee() {
        val p = Profile(30, Sex.MALE, 180, 80.0, Goal.MAINTAIN, ActivityLevel.MODERATE)
        val r = TargetCalc.calculate(p)
        // BMR 1780 * 1.55 = 2759; maintain delta 0 -> target 2759.
        assertEquals(1780.0, r.bmr, 0.001)
        assertEquals(2759.0, r.tdee, 0.001)
        assertEquals(2759.0, r.target.kcal, 0.001)
        // Protein 2g/kg -> 160g.
        assertEquals(160.0, r.target.proteinG, 0.001)
        // Fat 25% of 2759 / 9 = 76.6388...
        assertEquals(2759.0 * 0.25 / 9.0, r.target.fatG, 0.001)
        // Carbs = (2759 - 640 - 689.75) / 4 = 357.3125.
        assertEquals(357.3125, r.target.carbsG, 0.001)
    }

    @Test
    fun calculate_female_light_lose_applies_negative_delta() {
        val p = Profile(28, Sex.FEMALE, 165, 60.0, Goal.LOSE, ActivityLevel.LIGHT)
        val r = TargetCalc.calculate(p)
        // BMR 1330.25 * 1.375 = 1829.09375; lose -500 -> 1329.09375.
        assertEquals(1330.25, r.bmr, 0.001)
        assertEquals(1829.09375, r.tdee, 0.001)
        assertEquals(1329.09375, r.target.kcal, 0.001)
        // Protein 120g, fat 25% of 1329.09375 / 9.
        assertEquals(120.0, r.target.proteinG, 0.001)
        assertEquals(1329.09375 * 0.25 / 9.0, r.target.fatG, 0.001)
    }

    @Test
    fun calculate_male_very_gain_applies_positive_delta() {
        val p = Profile(40, Sex.MALE, 175, 90.0, Goal.GAIN, ActivityLevel.VERY)
        val r = TargetCalc.calculate(p)
        // BMR 1798.75 * 1.725 = 3102.84375; gain +500 -> 3602.84375.
        assertEquals(1798.75, r.bmr, 0.001)
        assertEquals(3102.84375, r.tdee, 0.001)
        assertEquals(3602.84375, r.target.kcal, 0.001)
        assertEquals(180.0, r.target.proteinG, 0.001)
    }

    @Test
    fun calculate_enforces_1200_kcal_safety_floor() {
        // Tiny weight + sedentary + aggressive cut would drop well below 1200.
        val p = Profile(80, Sex.FEMALE, 150, 40.0, Goal.LOSE, ActivityLevel.SEDENTARY)
        val r = TargetCalc.calculate(p)
        assertTrue(r.target.kcal >= 1200.0, "Target kcal must never drop below the 1200 safety floor.")
        assertEquals(1200.0, r.target.kcal, 0.001)
    }

    @Test
    fun calculate_macros_never_go_negative() {
        // Heavy user on the 1200 floor: protein alone is 240g (960 kcal); carbs
        // would go negative without the coerceAtLeast(0) guard.
        val p = Profile(50, Sex.MALE, 160, 120.0, Goal.LOSE, ActivityLevel.SEDENTARY)
        val r = TargetCalc.calculate(p)
        assertTrue(r.target.proteinG >= 0.0)
        assertTrue(r.target.fatG >= 0.0)
        assertTrue(r.target.carbsG >= 0.0)
    }
}
