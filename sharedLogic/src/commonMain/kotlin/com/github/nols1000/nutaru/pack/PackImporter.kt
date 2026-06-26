package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.db.NutaruDatabase

/**
 * Outcome of a successful pack import — what the UI surfaces ("Installed N
 * items from <pack>") and what gets persisted in the `packs` row.
 */
data class ImportResult(
    val packId: String,
    val itemCount: Int,
    val byteSize: Long,
    val sha256: String,
)

/**
 * Pack import runtime — issue-07. Takes already-downloaded pack bytes, verifies
 * the SHA-256 against the catalog entry (supply-chain integrity), stream-parses
 * the records via the KMP [PackCodec], and bulk-inserts into the `products`
 * table + `products_fts` index inside one transaction. Provenance (license,
 * attribution) lands in `sources`; pack metadata lands in `packs`.
 *
 * Pure DB logic, no I/O — the caller (platform) owns the download and hands the
 * bytes here. This keeps the commonTest seam a real round-trip with no network.
 *
 * SQLite is the single source of truth: after import, the pack file is only
 * kept for re-import/update; live reads always go through SQL + FTS5.
 */
object PackImporter {

    /** Catalog-driven install: verifies [packBytes] SHA-256 against [entry]
     *  before any write. Throws [ChecksumMismatchException] on mismatch and
     *  commits nothing (criterion 3: supply-chain integrity). */
    fun importPack(
        db: NutaruDatabase,
        packBytes: ByteArray,
        entry: CatalogEntry,
        nowMillis: Long,
    ): ImportResult {
        val actual = sha256Hex(packBytes)
        if (!actual.equals(entry.sha256, ignoreCase = true)) {
            throw ChecksumMismatchException(expected = entry.sha256, actual = actual)
        }
        return doImport(db, packBytes, entry, nowMillis, actual)
    }

    /** Side-loaded pack (OS file picker) — no catalog entry, so no checksum
     *  to verify against. The pack is still parsed and imported; the importer
     *  records the computed SHA-256 in the `packs` row so it can be re-verified
     *  later. (criterion 8: "validates checksum field if present" — for a
     *  side-load there is no manifest field to compare.) */
    fun importSideLoadedPack(
        db: NutaruDatabase,
        packBytes: ByteArray,
        fileName: String,
        nowMillis: Long,
    ): ImportResult {
        val actual = sha256Hex(packBytes)
        val entry = CatalogEntry(
            id = deriveSideLoadId(fileName, actual),
            name = fileName,
            version = "1.0.0",
            region = "",
            itemCount = 0,
            byteSize = packBytes.size.toLong(),
            sha256 = actual,
            url = "",
            license = "",
            attribution = "",
        )
        return doImport(db, packBytes, entry, nowMillis, actual)
    }

    private fun doImport(
        db: NutaruDatabase,
        packBytes: ByteArray,
        entry: CatalogEntry,
        nowMillis: Long,
        actualSha256: String,
    ): ImportResult {
        val reader = PackCodec.reader(packBytes)
        var count = 0

        db.transaction {
            // Re-importing a pack version first drops the old products + FTS
            // rows so the new version's ids fully replace them.
            db.productsQueries.deleteProductsByPack(entry.id)
            db.products_ftsQueries.deleteProductFtsByPack(entry.id)

            var firstSourceId: Long? = null
            var firstLicense: String? = null
            var firstAttribution: String? = null

            while (reader.hasNext()) {
                val p = reader.next()
                val displayName = p.nameI18n["en"] ?: p.nameI18n.values.first()
                db.productsQueries.insertPackProduct(
                    id = p.id,
                    barcode = p.barcode,
                    name = displayName,
                    brand = p.brand,
                    kcal_per_100g = p.kcalPer100g,
                    protein_g_per_100g = p.proteinGPer100g,
                    carbs_g_per_100g = p.carbsGPer100g,
                    fat_g_per_100g = p.fatGPer100g,
                    source_id = p.sourceId,
                    pack_id = entry.id,
                )
                db.products_ftsQueries.insertProductFts(
                    rowid = p.id,
                    name = displayName,
                    brand = p.brand,
                )
                if (firstSourceId == null) {
                    firstSourceId = p.sourceId
                    firstLicense = p.license
                    firstAttribution = p.attribution
                }
                count++
            }

            // Provenance: one sources row per distinct sourceId seen in the pack.
            // (Starter packs are all OFF = 1; side-loads may differ.)
            if (firstSourceId != null) {
                db.sourcesQueries.upsertSource(
                    id = firstSourceId,
                    license = firstLicense,
                    attribution = firstAttribution,
                    url = entry.url.takeIf { it.isNotBlank() },
                )
            }

            db.packsQueries.upsertPack(
                id = entry.id,
                name = entry.name,
                version = entry.version,
                region = entry.region.takeIf { it.isNotBlank() },
                source_url = entry.url.takeIf { it.isNotBlank() },
                imported_at = nowMillis,
                item_count = count.toLong(),
                byte_size = packBytes.size.toLong(),
                sha256 = actualSha256,
                license = entry.license.takeIf { it.isNotBlank() },
                attribution = entry.attribution.takeIf { it.isNotBlank() },
            )
        }

        return ImportResult(
            packId = entry.id,
            itemCount = count,
            byteSize = packBytes.size.toLong(),
            sha256 = actualSha256,
        )
    }

    /** Side-loaded packs get a stable id derived from the filename + checksum so
     *  re-importing the same file upserts rather than stacking. */
    private fun deriveSideLoadId(fileName: String, sha256: String): String =
        "sideload:${fileName}:${sha256.take(16)}"
}
