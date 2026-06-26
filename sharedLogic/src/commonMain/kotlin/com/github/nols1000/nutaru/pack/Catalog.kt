package com.github.nols1000.nutaru.pack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `catalog.json` manifest — the project-curated list of installable packs hosted
 * at `nutaru.app/catalog.json` (see `docs/issue-06-pack-compiler-catalog-hosting.md`).
 *
 * This is the KMP mirror of the compiler's `Catalog` model (`tools/pack-compiler`);
 * kotlinx.serialization with `ignoreUnknownKeys` keeps the two decoupled so adding
 * a manifest field doesn't break an older app. The model is the single source of
 * truth for the manifest shape the import runtime consumes.
 */
@Serializable
data class Catalog(
    val schema: String = SCHEMA,
    val updated: String = "",
    val packs: List<CatalogEntry> = emptyList(),
) {
    companion object {
        const val SCHEMA = "nutaru.catalog/v1"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
        fun decode(s: String): Catalog = json.decodeFromString(s)
    }
}

/** One installable pack in the catalog. Field names match the manifest JSON. */
@Serializable
data class CatalogEntry(
    val id: String,
    val name: String,
    val version: String,
    val region: String = "",
    @SerialName("item_count") val itemCount: Int,
    @SerialName("byte_size") val byteSize: Long,
    val sha256: String,
    val url: String,
    val license: String = "",
    val attribution: String = "",
)

/**
 * Thrown when a downloaded pack's SHA-256 does not match the catalog entry.
 * The import runtime aborts on this (issue-07 criterion 3) — supply-chain
 * integrity: a tampered or truncated pack never reaches the products table.
 */
class ChecksumMismatchException(
    val expected: String,
    val actual: String,
) : RuntimeException("Pack SHA-256 mismatch: expected $expected, got $actual")
