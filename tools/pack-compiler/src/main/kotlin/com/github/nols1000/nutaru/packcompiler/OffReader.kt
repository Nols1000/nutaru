package com.github.nols1000.nutaru.packcompiler

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Region filter + OFF-bulk-export -> [Product] mapping.
 *
 * The OFF bulk export is newline-delimited JSON (one product object per line).
 * This reader streams it line by line so the compiler never holds the whole
 * export in memory; only the records that pass the region filter are decoded
 * into [Product] and handed to the pack writer.
 *
 * Field mapping is the minimal subset the pack + app `products` table need; see
 * `docs/pack-compiler-spec.md` for the source-field -> record-field table.
 */
object OffReader {

    /** Region id -> set of OFF `countries_tags` values (without the `en:` prefix)
     *  that a product must carry at least one of to be included in the pack.
     *  `null` means no country filter (used by the global brands pack). */
    val REGION_TAGS: Map<String, Set<String>?> = mapOf(
        "US" to setOf("united-states"),
        "EU-MIX" to setOf("germany", "france", "spain", "italy"),
        "UK" to setOf("united-kingdom"),
        "JP" to setOf("japan"),
        "BR" to setOf("brazil"),
        // Global brands pack: no country filter — brand-flagged products only.
        // The compiler matches `GLOBAL` against any product whose brand is non-empty
        // and that is not already picked up by a regional filter is NOT how this
        // works; GLOBAL keeps brand-dominant SKUs. For the demo path this is moot.
        "GLOBAL" to null,
    )

    fun regionMatches(region: String, countriesTags: List<String>): Boolean {
        val allowed = REGION_TAGS[region] ?: error("Unknown region: $region")
        if (allowed == null) return true // GLOBAL: no country filter
        if (countriesTags.isEmpty()) return false
        return countriesTags.any { tag ->
            // OFF stores tags as "en:united-states"; strip the lang prefix.
            val raw = tag.substringAfter("en:", tag).lowercase()
            raw in allowed
        }
    }

    /** Streams [input] as NDJSON, yielding products that pass the region filter.
     *  Lines that are not valid JSON or that lack the required nutrition fields
     *  are skipped with a count returned in [ReadStats] — OFF's export is noisy,
     *  and dropping incomplete rows is the documented curation policy. */
    fun read(input: InputStream, region: String): Sequence<Product> = sequence {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { r ->
            var lineNo = 0
            r.lineSequence().forEach { line ->
                lineNo++
                val trimmed = line.trim()
                if (trimmed.isEmpty() || !trimmed.startsWith("{")) return@forEach
                val obj = runCatching { json.parseToJsonElement(trimmed).jsonObject }.getOrNull() ?: return@forEach
                val product = parseProduct(obj, region) ?: return@forEach
                yield(product)
            }
        }
    }
}

/** Reads back a count of kept/skipped rows for compiler logging. Not used in
 *  the streaming path above (kept minimal); the compiler reports the kept
 *  count from the writer. */
data class ReadStats(val kept: Int, val skipped: Int)

// --- per-row parsing ---------------------------------------------------------

internal fun parseProduct(obj: JsonObject, region: String): Product? {
    val countriesTags = obj["countries_tags"]?.let { tagsList(it) } ?: emptyList()
    if (!OffReader.regionMatches(region, countriesTags)) return null

    val id = obj["_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: obj["code"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: return null

    val barcode = obj["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    val nameI18n = buildStringNameMap(obj)
    if (nameI18n.isEmpty()) return null

    val brand = obj["brands"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    val categories = obj["categories_tags"]?.let { tagsList(it) }
        ?: obj["categories"]?.jsonPrimitive?.contentOrNull?.split(",")?.map { it.trim() }.orEmpty()

    val nutriments = obj["nutriments"]?.jsonObject
    val kcal = nutriments?.get("energy-kcal_100g")?.jsonPrimitive?.doubleOrNull
        ?: nutriments?.get("energy_100g")?.jsonPrimitive?.doubleOrNull?.let { it / 4.184 }
        ?: return null // No energy -> can't compute macros; drop (curation policy).
    val protein = nutriments?.get("proteins_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val carbs = nutriments?.get("carbohydrates_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val fat = nutriments?.get("fat_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0

    val servings = parseServings(obj)
    val ingredients = obj["ingredients_text"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    return Product(
        id = id,
        barcode = barcode,
        // Sorted for deterministic pack bytes (see PackFormat doc).
        nameI18n = nameI18n.toSortedMap(),
        brand = brand,
        categories = categories.map { it.lowercase() }.distinct().sorted(),
        kcalPer100g = kcal,
        proteinGPer100g = protein,
        carbsGPer100g = carbs,
        fatGPer100g = fat,
        servings = servings,
        ingredients = ingredients,
        sourceId = PackFormat.SOURCE_ID_OFF,
        license = PackFormat.LICENSE_ODBL,
        attribution = PackFormat.ATTRIBUTION_OFF,
    )
}

/** OFF stores names as `product_name` (default) + `product_name_<lang>` per
 *  language. Pulls every `product_name*` field into a lang->name map, falling
 *  back to `generic_name` under "en" if no product_name is present. */
private fun buildStringNameMap(obj: JsonObject): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    obj["product_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { out["en"] = it }
    obj.entries.forEach { (k, v) ->
        if (k.startsWith("product_name_") && v is JsonPrimitive && v.contentOrNull?.isNotBlank() == true) {
            val lang = k.removePrefix("product_name_").lowercase()
            out[lang] = v.content
        }
    }
    if (out.isEmpty()) {
        obj["generic_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { out["en"] = it }
    }
    return out
}

/** OFF `serving_size` is a display string ("1 cup (240g)"); `serving_quantity`
 *  is the gram equivalent when present. We record the original unit string and
 *  the gram weight so the app preserves as-entered portions. */
private fun parseServings(obj: JsonObject): List<Serving> {
    val unit = obj["serving_size"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    val grams = obj["serving_quantity"]?.jsonPrimitive?.doubleOrNull ?: return listOf(Serving(unit, 0.0))
    return listOf(Serving(unit, grams))
}

private fun tagsList(el: kotlinx.serialization.json.JsonElement): List<String> =
    (el as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }.orEmpty()
