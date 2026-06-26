package com.github.nols1000.nutaru

/**
 * Summed macros for a day (or any rollup window).
 *
 * Values are in the same units stored per-100g in `products`: kcal and grams.
 * A "no entries" window is exactly the [ZERO] sentinel — never null — so the
 * dashboard renders a flat zero tile on first launch.
 */
data class MacroTotals(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    operator fun plus(other: MacroTotals): MacroTotals =
        MacroTotals(
            kcal = kcal + other.kcal,
            proteinG = proteinG + other.proteinG,
            carbsG = carbsG + other.carbsG,
            fatG = fatG + other.fatG,
        )

    companion object {
        val ZERO = MacroTotals(0.0, 0.0, 0.0, 0.0)
    }
}

/** A food item's per-100g nutrition fingerprint, the shape `products` stores. */
data class ProductMacros(
    val id: Long,
    val name: String,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbsGPer100g: Double,
    val fatGPer100g: Double,
)

/** One installed pack — the read model for Settings → Packs and onboarding's
 *  "already installed" check. Mirrors the `packs` table row. */
data class InstalledPack(
    val id: String,
    val name: String,
    val version: String,
    val region: String?,
    val itemCount: Int,
    val byteSize: Long,
    val importedAtMillis: Long,
    val sha256: String?,
    val license: String?,
    val attribution: String?,
)

/** A single committed log entry with the grams needed for rollup math. */
data class LoggedEntry(
    val id: Long,
    val productId: Long,
    val quantityGrams: Double,
    val timestampMillis: Long,
)

/**
 * A diary row: a log entry joined with its product's display name and per-100g
 * macros. Carries everything the day view needs to render and roll up in one
 * pass — no second product lookup. The repository maps the joined SQL row to
 * this; [MacroRollup.groupByMeal] consumes it as pure input so `commonTest` can
 * verify meal grouping + rollup math with no DB.
 */
data class DiaryEntry(
    val id: Long,
    val productId: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val quantityGrams: Double,
    val mealType: String,
    val timestampMillis: Long,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbsGPer100g: Double,
    val fatGPer100g: Double,
)

/** Canonical meal ordering for the diary (issue-05, criterion 3). */
enum class MealType(val key: String, val display: String) {
    BREAKFAST("breakfast", "Breakfast"),
    LUNCH("lunch", "Lunch"),
    DINNER("dinner", "Dinner"),
    SNACK("snack", "Snack");

    companion object {
        val ORDER: List<MealType> = entries
        private val byKey = entries.associateBy { it.key }
        fun fromKey(key: String): MealType = byKey[key.lowercase()] ?: SNACK
    }
}

/** One rendered entry row with its computed macros. */
data class MealRow(
    val entryId: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val macros: MacroTotals,
)

/** A meal section of the diary: rows under one [MealType] + their subtotal. */
data class MealGroup(
    val mealType: MealType,
    val rows: List<MealRow>,
    val subtotal: MacroTotals,
)

/**
 * Day-total macro rollup — pure math, no SQL.
 *
 * The dashboard groups `log_entries` by calendar day and sums each row's
 * contribution: `quantity_grams / 100 * per-100g macros`. Keeping this in
 * `commonMain` (not in the SQL layer) lets `commonTest` verify the day-grouping
 * and the per-row math against fixed fixtures, independent of the driver.
 *
 * Day boundaries come from the caller as epoch-millis `[dayStart, dayEnd)` so
 * this function stays timezone-agnostic: the platform decides which calendar
 * day "today" maps to. This avoids dragging `java.util.Calendar` or
 * `kotlinx-datetime` into a tracer-bullet.
 */
object MacroRollup {

    fun dayTotal(
        entries: List<LoggedEntry>,
        productsById: Map<Long, ProductMacros>,
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): MacroTotals {
        val within = entries.filter { it.timestampMillis in dayStartMillis until dayEndMillis }
        return within.fold(MacroTotals.ZERO) { acc, entry ->
            val macros = productsById[entry.productId]
                // A log entry without a resolvable product contributes nothing.
                // Hard-delete products (V1.1 feature) would orphan logs; for the
                // tracer-bullet seed ids are fixed and never removed.
                ?: return@fold acc
            val factor = entry.quantityGrams / 100.0
            acc + MacroTotals(
                kcal = macros.kcalPer100g * factor,
                proteinG = macros.proteinGPer100g * factor,
                carbsG = macros.carbsGPer100g * factor,
                fatG = macros.fatGPer100g * factor,
            )
        }
    }

    /**
     * Groups a day's [DiaryEntry]s into [MealGroup]s in canonical meal order
     * (breakfast → lunch → dinner → snack), computing per-row macros and
     * per-meal subtotals. Pure math, no SQL — pinned in `commonTest`.
     *
     * Entries are sorted by timestamp within each meal so the diary lists them
     * in log order. An unrecognized `meal_type` buckets into [MealType.SNACK]
     * rather than dropping the row (hard-delete posture: never silently lose a
     * logged entry).
     */
    fun groupByMeal(entries: List<DiaryEntry>): List<MealGroup> {
        val byMeal = entries.groupBy { MealType.fromKey(it.mealType) }
        return MealType.ORDER.mapNotNull { mt ->
            val mealEntries = byMeal[mt] ?: return@mapNotNull null
            val rows = mealEntries.sortedBy { it.timestampMillis }.map { it.toMealRow() }
            MealGroup(
                mealType = mt,
                rows = rows,
                subtotal = rows.fold(MacroTotals.ZERO) { acc, r -> acc + r.macros },
            )
        }
    }

    /** Day total as the sum of per-meal subtotals — the diary day-total card. */
    fun dayTotal(groups: List<MealGroup>): MacroTotals =
        groups.fold(MacroTotals.ZERO) { acc, g -> acc + g.subtotal }

    private fun DiaryEntry.toMealRow(): MealRow {
        val factor = quantityGrams / 100.0
        return MealRow(
            entryId = id,
            name = name,
            quantity = quantity,
            unit = unit,
            macros = MacroTotals(
                kcal = kcalPer100g * factor,
                proteinG = proteinGPer100g * factor,
                carbsG = carbsGPer100g * factor,
                fatG = fatGPer100g * factor,
            ),
        )
    }
}