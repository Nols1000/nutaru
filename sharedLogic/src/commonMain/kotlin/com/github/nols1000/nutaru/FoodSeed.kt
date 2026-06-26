package com.github.nols1000.nutaru

import com.github.nols1000.nutaru.db.NutaruDatabase

/**
 * Curated starter foods seeded into the `products` table on first launch.
 *
 * Fixed ids + `INSERT OR IGNORE` make [seed] idempotent across relaunches:
 * the second call hits the existing rows and writes nothing. No pack system
 * yet — this is the tracer-bullet's replacement for the pack import seam so
 * the "log food" form has something to offer.
 *
 * TODO: replace with pack imports once `issue-*-pack-import` lands. Macros are
 * approximate, sourced from common USDA-ish reference values; precise provenance
 * is not a tracer-bullet concern.
 */
object FoodSeed {

    data class SeedItem(
        val id: Long,
        val barcode: String?,
        val name: String,
        val brand: String?,
        val kcalPer100g: Double,
        val proteinGPer100g: Double,
        val carbsGPer100g: Double,
        val fatGPer100g: Double,
    )

    val ITEMS: List<SeedItem> = listOf(
        SeedItem(1L, null, "Banana", null, 89.0, 1.1, 23.0, 0.3),
        SeedItem(2L, null, "Chicken breast (cooked)", null, 165.0, 31.0, 0.0, 3.6),
        SeedItem(3L, null, "White rice (cooked)", null, 130.0, 2.7, 28.0, 0.3),
        SeedItem(4L, null, "Whole egg (cooked)", null, 155.0, 13.0, 1.1, 11.0),
        SeedItem(5L, null, "Greek yogurt (plain)", null, 59.0, 10.0, 3.6, 0.4),
    )

    fun seed(db: NutaruDatabase) {
        val q = db.productsQueries
        ITEMS.forEach { item ->
            q.insertSeedProduct(
                id = item.id,
                barcode = item.barcode,
                name = item.name,
                brand = item.brand,
                kcal_per_100g = item.kcalPer100g,
                protein_g_per_100g = item.proteinGPer100g,
                carbs_g_per_100g = item.carbsGPer100g,
                fat_g_per_100g = item.fatGPer100g,
                source_id = null,
                pack_id = null,
            )
        }
    }
}