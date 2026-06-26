package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-math meal-grouping self-check (issue-05, criteria 9–10). No DB, no
 * platform — feeds [MacroRollup.groupByMeal] fixed [DiaryEntry] fixtures and
 * asserts:
 *  1. Day grouping returns the correct rows per meal, in canonical order.
 *  2. Per-meal subtotals and the day-total rollup (sum of subtotals) are
 *     arithmetically correct.
 */
class MealGroupingTest {

    private val dayStart = 1_783_372_800_000L

    private fun e(
        id: Long,
        productId: Long,
        name: String,
        grams: Double,
        meal: String,
        ts: Long,
        k: Double,
        p: Double,
        c: Double,
        f: Double,
        qty: Double = 1.0,
        unit: String = "servings",
    ): DiaryEntry = DiaryEntry(
        id = id,
        productId = productId,
        name = name,
        quantity = qty,
        unit = unit,
        quantityGrams = grams,
        mealType = meal,
        timestampMillis = ts,
        kcalPer100g = k,
        proteinGPer100g = p,
        carbsGPer100g = c,
        fatGPer100g = f,
    )

    // Banana 89/1.1/23/0.3, Chicken 165/31/0/3.6, Rice 130/2.7/28/0.3
    private fun banana(id: Long, grams: Double, meal: String, ts: Long) =
        e(id, 1, "Banana", grams, meal, ts, 89.0, 1.1, 23.0, 0.3)

    private fun chicken(id: Long, grams: Double, meal: String, ts: Long) =
        e(id, 2, "Chicken", grams, meal, ts, 165.0, 31.0, 0.0, 3.6)

    private fun rice(id: Long, grams: Double, meal: String, ts: Long) =
        e(id, 3, "Rice", grams, meal, ts, 130.0, 2.7, 28.0, 0.3)

    // ---------- day grouping: correct rows per meal, canonical order ----------

    @Test
    fun groups_appear_in_canonical_breakfast_lunch_dinner_snack_order() {
        val entries = listOf(
            chicken(1, 120.0, "dinner", dayStart + 3),
            banana(2, 100.0, "breakfast", dayStart + 1),
            e(3, 5, "Yogurt", 150.0, "snack", dayStart + 4, 59.0, 10.0, 3.6, 0.4),
            rice(4, 200.0, "lunch", dayStart + 2),
        )
        val groups = MacroRollup.groupByMeal(entries)
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK),
            groups.map { it.mealType },
        )
    }

    @Test
    fun empty_meals_are_omitted_not_rendered_as_zero_groups() {
        val groups = MacroRollup.groupByMeal(listOf(banana(1, 100.0, "breakfast", dayStart + 1)))
        assertEquals(1, groups.size)
        assertEquals(MealType.BREAKFAST, groups.single().mealType)
    }

    @Test
    fun rows_within_a_meal_are_sorted_by_timestamp() {
        val entries = listOf(
            banana(2, 100.0, "breakfast", dayStart + 5_000),
            rice(1, 200.0, "breakfast", dayStart + 1_000),
            chicken(3, 120.0, "breakfast", dayStart + 3_000),
        )
        assertEquals(listOf(1L, 3L, 2L), MacroRollup.groupByMeal(entries).single().rows.map { it.entryId })
    }

    @Test
    fun unrecognized_meal_type_buckets_into_snack_not_dropped() {
        val groups = MacroRollup.groupByMeal(listOf(banana(1, 100.0, "midnight", dayStart + 1)))
        assertEquals(MealType.SNACK, groups.single().mealType)
        assertEquals(1, groups.single().rows.size)
    }

    @Test
    fun meal_type_matching_is_case_insensitive() {
        val groups = MacroRollup.groupByMeal(listOf(banana(1, 100.0, "BREAKFAST", dayStart + 1)))
        assertEquals(MealType.BREAKFAST, groups.single().mealType)
    }

    // ---------- per-meal subtotal + day-total rollup math ----------

    @Test
    fun per_meal_subtotal_sums_its_rows_macros() {
        val entries = listOf(
            banana(1, 100.0, "breakfast", dayStart + 1), // 89/1.1/23/0.3
            rice(2, 200.0, "breakfast", dayStart + 2),   // 260/5.4/56/0.6
        )
        val sub = MacroRollup.groupByMeal(entries).single().subtotal
        assertEquals(89.0 + 260.0, sub.kcal, 0.001)
        assertEquals(1.1 + 5.4, sub.proteinG, 0.001)
        assertEquals(23.0 + 56.0, sub.carbsG, 0.001)
        assertEquals(0.3 + 0.6, sub.fatG, 0.001)
    }

    @Test
    fun day_total_equals_sum_of_per_meal_subtotals() {
        val entries = listOf(
            banana(1, 100.0, "breakfast", dayStart + 1),
            chicken(2, 150.0, "lunch", dayStart + 2),
            rice(3, 200.0, "dinner", dayStart + 3),
            banana(4, 118.0, "snack", dayStart + 4),
        )
        val groups = MacroRollup.groupByMeal(entries)
        val dayTotal = MacroRollup.dayTotal(groups)
        assertEquals(dayTotal, groups.fold(MacroTotals.ZERO) { acc, g -> acc + g.subtotal })
        // Banana 100g: 89/1.1/23/0.3; Chicken 150g: 247.5/46.5/0/5.4;
        // Rice 200g: 260/5.4/56/0.6; Banana 118g: 105.02/1.298/27.14/0.354
        assertEquals(89.0 + 247.5 + 260.0 + 105.02, dayTotal.kcal, 0.01)
        assertEquals(1.1 + 46.5 + 5.4 + 1.298, dayTotal.proteinG, 0.01)
        assertEquals(23.0 + 0.0 + 56.0 + 27.14, dayTotal.carbsG, 0.01)
        assertEquals(0.3 + 5.4 + 0.6 + 0.354, dayTotal.fatG, 0.01)
    }

    @Test
    fun day_total_for_no_entries_is_zero() {
        assertEquals(MacroTotals.ZERO, MacroRollup.dayTotal(MacroRollup.groupByMeal(emptyList())))
    }

    @Test
    fun row_macros_scale_by_quantity_grams_over_100() {
        val macros = MacroRollup.groupByMeal(listOf(banana(1, 50.0, "snack", dayStart + 1)))
            .single().rows.single().macros
        // 50g = half of per-100g
        assertEquals(44.5, macros.kcal, 0.001)
        assertEquals(0.55, macros.proteinG, 0.001)
        assertEquals(11.5, macros.carbsG, 0.001)
        assertEquals(0.15, macros.fatG, 0.001)
    }

    @Test
    fun groupByMeal_groups_only_what_it_is_given() {
        // Day scoping is the repository's job (SQL WHERE); the math groups
        // whatever it receives with no leakage between calls.
        val groups = MacroRollup.groupByMeal(listOf(banana(1, 100.0, "breakfast", dayStart + 1)))
        assertTrue(groups.all { g -> g.rows.all { it.entryId == 1L } })
    }
}
