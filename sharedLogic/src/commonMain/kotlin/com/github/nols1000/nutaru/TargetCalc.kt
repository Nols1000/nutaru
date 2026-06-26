package com.github.nols1000.nutaru

/**
 * Onboarding profile + daily target calculation (Mifflin-St Jeor).
 *
 * Pure math, no SQL, no platform — verifiable in `commonTest` against fixed
 * vectors. The repository persists [Profile] (minus weight, which lives in
 * `weight_entries`) and the resulting [MacroTarget]; the UI renders
 * [TargetReasoning] so the user can see how the number was derived and trust it.
 *
 * Formulas:
 *   BMR (Mifflin-St Jeor)
 *     men:   10*kg + 6.25*cm - 5*age + 5
 *     women: 10*kg + 6.25*cm - 5*age - 161
 *   TDEE      = BMR * activityFactor
 *   target    = TDEE + goalDelta   (kcal)
 *   protein   = 2.0 g/kg bodyweight
 *   fat       = 25% of target kcal / 9
 *   carbs     = remaining kcal / 4
 */

enum class Sex { MALE, FEMALE }

enum class Goal { LOSE, MAINTAIN, GAIN }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, VERY, EXTRA }

/**
 * The six onboarding profile fields. `weightKg` is read from the latest
 * `weight_entries` row at calc time, not stored in the `profile` table.
 */
data class Profile(
    val ageYears: Int,
    val sex: Sex,
    val heightCm: Int,
    val weightKg: Double,
    val goal: Goal,
    val activity: ActivityLevel,
)

/** Daily kcal + macro target. Same shape as [MacroTotals] but semantically a
 *  goal, not a rollup. Kept structurally separate so the dashboard can show
 *  "consumed vs target" without overloading the rollup type. */
data class MacroTarget(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

/** The derivation of [MacroTarget], rendered on the Plan screen so the user
 *  can audit the math (BMR + activity + goal delta). */
data class TargetReasoning(
    val bmr: Double,
    val activityFactor: Double,
    val tdee: Double,
    val goalDeltaKcal: Double,
    val target: MacroTarget,
    val proteinPerKg: Double,
)

object TargetCalc {

    fun activityFactor(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.2
        ActivityLevel.LIGHT -> 1.375
        ActivityLevel.MODERATE -> 1.55
        ActivityLevel.VERY -> 1.725
        ActivityLevel.EXTRA -> 1.9
    }

    fun goalDelta(goal: Goal): Double = when (goal) {
        Goal.LOSE -> -500.0
        Goal.MAINTAIN -> 0.0
        Goal.GAIN -> 500.0
    }

    fun bmr(profile: Profile): Double {
        val base = 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.ageYears
        return when (profile.sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
        }
    }

    fun calculate(profile: Profile): TargetReasoning {
        val b = bmr(profile)
        val factor = activityFactor(profile.activity)
        val tdee = b * factor
        val delta = goalDelta(profile.goal)
        // Safety floor: a cut this aggressive is unsafe below ~1200 kcal.
        // TODO: floor should scale with lean body mass; a 1200 hard floor lets a
        //   very heavy user on an aggressive cut get macros that don't sum to
        //   kcal (carbs coerced to 0). Fine for typical onboarding profiles;
        //   revisit with adaptive targets in V1.1.
        val kcal = (tdee + delta).coerceAtLeast(1200.0)
        val proteinG = 2.0 * profile.weightKg
        val fatG = kcal * 0.25 / 9.0
        val carbsG = ((kcal - proteinG * 4.0 - fatG * 9.0) / 4.0).coerceAtLeast(0.0)
        return TargetReasoning(
            bmr = b,
            activityFactor = factor,
            tdee = tdee,
            goalDeltaKcal = delta,
            target = MacroTarget(kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG),
            proteinPerKg = 2.0,
        )
    }
}
