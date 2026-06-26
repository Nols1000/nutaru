package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-math rollup self-check (no DB, no platform).
 *
 * Covers the two non-trivial pieces of the tracer-bullet's dashboard math:
 * 1. Per-row contribution `quantity_grams / 100 * per-100g` aggregates correctly.
 * 2. Day boundary `[dayStart, dayEnd)` includes the right rows and excludes
 *    logs written for a different date (the manual date-picker criterion).
 */
class MacroRollupTest {

    private val banana = ProductMacros(1L, "Banana", 89.0, 1.1, 23.0, 0.3)
    private val chicken = ProductMacros(2L, "Chicken breast", 165.0, 31.0, 0.0, 3.6)
    private val products = mapOf(banana.id to banana, chicken.id to chicken)

    // Day window = the 24h of 2026-06-25 in UTC.
    private val dayStart = 1_783_372_800_000L
    private val dayEnd = dayStart + 24L * 60 * 60 * 1000

    @Test
    fun empty_window_is_zero_not_null() {
        assertEquals(
            MacroTotals.ZERO,
            MacroRollup.dayTotal(emptyList(), products, dayStart, dayEnd),
        )
    }

    @Test
    fun single_entry_100g_contributes_exactly_its_per_100g_macros() {
        val entries = listOf(LoggedEntry(1L, banana.id, 100.0, dayStart + 1000))
        val total = MacroRollup.dayTotal(entries, products, dayStart, dayEnd)
        assertEquals(89.0, total.kcal, 0.001)
        assertEquals(1.1, total.proteinG, 0.001)
        assertEquals(23.0, total.carbsG, 0.001)
        assertEquals(0.3, total.fatG, 0.001)
    }

    @Test
    fun multiple_entries_sum_across_products_and_quantities() {
        val entries = listOf(
            LoggedEntry(1L, banana.id, 200.0, dayStart + 1_000),     // 2x banana
            LoggedEntry(2L, chicken.id, 150.0, dayStart + 2_000),    // 1.5x chicken
        )
        val total = MacroRollup.dayTotal(entries, products, dayStart, dayEnd)
        // banana 200g: 178 kcal, 2.2 P, 46.0 C, 0.6 F
        // chicken 150g: 247.5 kcal, 46.5 P, 0.0 C, 5.4 F
        assertEquals(178.0 + 247.5, total.kcal, 0.001)
        assertEquals(2.2 + 46.5, total.proteinG, 0.001)
        assertEquals(46.0, total.carbsG, 0.001)
        assertEquals(0.6 + 5.4, total.fatG, 0.001)
    }

    @Test
    fun entries_outside_the_day_window_are_excluded() {
        val entries = listOf(
            LoggedEntry(1L, banana.id, 100.0, dayStart - 1_000),       // day before
            LoggedEntry(2L, chicken.id, 100.0, dayEnd),               // exact upper bound excluded
            LoggedEntry(3L, banana.id, 100.0, dayEnd + 1_000),         // next day
            LoggedEntry(4L, chicken.id, 100.0, dayStart + 60_000),     // inside
        )
        val total = MacroRollup.dayTotal(entries, products, dayStart, dayEnd)
        // Only the chicken entry inside the day counts.
        assertEquals(165.0, total.kcal, 0.001)
        assertEquals(31.0, total.proteinG, 0.001)
    }

    @Test
    fun entry_with_unknown_product_is_skipped_not_crashed() {
        // Orphan log (product deleted): rollup must degrade gracefully, not throw.
        val entries = listOf(
            LoggedEntry(1L, 999L, 100.0, dayStart + 1_000),
            LoggedEntry(2L, banana.id, 100.0, dayStart + 2_000),
        )
        val total = MacroRollup.dayTotal(entries, products, dayStart, dayEnd)
        assertEquals(89.0, total.kcal, 0.001)
    }

    @Test
    fun zero_grams_entry_contributes_nothing() {
        val entries = listOf(LoggedEntry(1L, banana.id, 0.0, dayStart + 1_000))
        assertEquals(
            MacroTotals.ZERO,
            MacroRollup.dayTotal(entries, products, dayStart, dayEnd),
        )
    }
}