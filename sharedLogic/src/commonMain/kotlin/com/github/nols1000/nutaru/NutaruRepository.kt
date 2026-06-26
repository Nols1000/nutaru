package com.github.nols1000.nutaru

import com.github.nols1000.nutaru.db.Log_entries
import com.github.nols1000.nutaru.db.NutaruDatabase
import com.github.nols1000.nutaru.db.Packs
import com.github.nols1000.nutaru.db.Products
import com.github.nols1000.nutaru.db.SearchProductsFts
import com.github.nols1000.nutaru.db.SelectLogEntriesForDayWithProduct
import com.github.nols1000.nutaru.pack.CatalogEntry
import com.github.nols1000.nutaru.pack.ChecksumMismatchException
import com.github.nols1000.nutaru.pack.ImportResult
import com.github.nols1000.nutaru.pack.PackImporter

/**
 * Single access point for the "log food → dashboard" + onboarding flows.
 *
 * Wraps [NutaruDatabase] so the UI talks to one domain-shaped seam instead of
 * SQLDelight query objects. Reads return the pure [MacroTotals]/[ProductMacros]
 * / [LoggedEntry]/[Profile]/[MacroTarget] types so the rollup + target math can
 * be tested with no DB at all.
 *
 * All methods are synchronous: SQLDelight calls are blocking and the call site
 * (the UI layer) is responsible for dispatching off the main thread. Keeping
 * suspend out avoids an `IO` dispatcher dependency that would otherwise need
 * platform expect/actual wiring.
 */
class NutaruRepository(private val db: NutaruDatabase) {

    fun seedFoods() = FoodSeed.seed(db)

    fun allFoods(): List<ProductMacros> =
        db.productsQueries.selectAllProducts().executeAsList().map { it.toMacros() }

    fun dayTotal(dayStartMillis: Long, dayEndMillis: Long): MacroTotals {
        val rows = db.log_entriesQueries.selectLogEntriesForDay(dayStartMillis, dayEndMillis).executeAsList()
        val products = db.productsQueries.selectAllProducts().executeAsList().associateBy(
            keySelector = { it.id },
            valueTransform = { it.toMacros() },
        )
        return MacroRollup.dayTotal(
            entries = rows.map { it.toLogged() },
            productsById = products,
            dayStartMillis = dayStartMillis,
            dayEndMillis = dayEndMillis,
        )
    }

    fun logFood(
        productId: Long,
        quantity: Double,
        unit: String,
        quantityGrams: Double,
        mealType: String,
        source: String,
        timestampMillis: Long,
        notes: String? = null,
    ) {
        db.log_entriesQueries.insertLogEntry(
            product_id = productId,
            quantity = quantity,
            unit = unit,
            quantity_grams = quantityGrams,
            meal_type = mealType,
            source = source,
            timestamp = timestampMillis,
            notes = notes,
        )
    }

    /** Day's entries joined with product name + per-100g, for diary grouping. */
    fun entriesForDay(dayStartMillis: Long, dayEndMillis: Long): List<DiaryEntry> =
        db.log_entriesQueries.selectLogEntriesForDayWithProduct(dayStartMillis, dayEndMillis)
            .executeAsList()
            .map { it.toDiaryEntry() }

    /** Hard-deletes one entry (issue-05, criterion 8). No soft-delete column. */
    fun deleteEntry(id: Long) {
        db.log_entriesQueries.deleteLogEntry(id)
    }

    /** Edits an existing entry in place (issue-05, criterion 7). Product,
     *  quantity/unit, grams, meal type, and timestamp are all editable. */
    fun updateEntry(
        id: Long,
        productId: Long,
        quantity: Double,
        unit: String,
        quantityGrams: Double,
        mealType: String,
        timestampMillis: Long,
    ) {
        db.log_entriesQueries.updateLogEntry(
            id = id,
            product_id = productId,
            quantity = quantity,
            unit = unit,
            quantity_grams = quantityGrams,
            meal_type = mealType,
            timestamp = timestampMillis,
        )
    }

    // --- Onboarding: profile, weight, targets ---

    /** True once the singleton profile row exists — the App router uses this to
     *  decide between onboarding and Home. */
    fun hasProfile(): Boolean =
        db.profileQueries.selectProfile().executeAsOneOrNull() != null

    /** The onboarding profile. Null if no profile row OR no weight row yet
     *  (weight lives in `weight_entries`; without it the BMR calc has no input). */
    fun profile(): Profile? {
        val row = db.profileQueries.selectProfile().executeAsOneOrNull() ?: return null
        val weight = db.weight_entriesQueries.selectLatestWeight().executeAsOneOrNull()?.weight_kg ?: return null
        return Profile(
            ageYears = row.age.toInt(),
            sex = Sex.valueOf(row.sex.uppercase()),
            heightCm = row.height_cm.toInt(),
            weightKg = weight,
            goal = Goal.valueOf(row.goal.uppercase()),
            activity = ActivityLevel.valueOf(row.activity_level.uppercase()),
        )
    }

    /** Persists the singleton profile row + the first weight entry. Idempotent
     *  on the profile (upsert); each call adds a weight row so trend is kept. */
    fun saveOnboardingProfile(profile: Profile, nowMillis: Long) {
        db.profileQueries.upsertProfile(
            id = 1L,
            age = profile.ageYears.toLong(),
            sex = profile.sex.name.lowercase(),
            height_cm = profile.heightCm.toLong(),
            goal = profile.goal.name.lowercase(),
            activity_level = profile.activity.name.lowercase(),
            created_at = nowMillis,
            updated_at = nowMillis,
            display_name = null,
        )
        db.weight_entriesQueries.insertWeightEntry(
            weight_kg = profile.weightKg,
            timestamp = nowMillis,
        )
    }

    fun latestWeightKg(): Double? =
        db.weight_entriesQueries.selectLatestWeight().executeAsOneOrNull()?.weight_kg

    fun logWeight(weightKg: Double, timestampMillis: Long) {
        db.weight_entriesQueries.insertWeightEntry(weight_kg = weightKg, timestamp = timestampMillis)
    }

    /** Persists a daily target with [effectiveFromMillis] as the day start from
     *  which it applies. `source` is "algorithm" (calculated) or "manual"
     *  (user-overridden). */
    fun saveTarget(
        target: MacroTarget,
        effectiveFromMillis: Long,
        source: String,
        createdAtMillis: Long,
    ) {
        db.targetsQueries.insertTarget(
            kcal = target.kcal,
            protein_g = target.proteinG,
            carbs_g = target.carbsG,
            fat_g = target.fatG,
            effective_from = effectiveFromMillis,
            source = source,
            created_at = createdAtMillis,
        )
    }

    /** The target effective on [dateMillis]: the newest row whose
     *  `effective_from` is <= the query date. Null if no target exists yet. */
    fun targetFor(dateMillis: Long): MacroTarget? {
        val row = db.targetsQueries.selectTargetEffectiveOnOrBefore(dateMillis).executeAsOneOrNull()
            ?: return null
        return MacroTarget(
            kcal = row.kcal,
            proteinG = row.protein_g,
            carbsG = row.carbs_g,
            fatG = row.fat_g,
        )
    }

    // --- Onboarding: recovery mnemonic acknowledgment ---

    /** True once the user has confirmed they saved the mnemonic in the reveal
     *  step. The App router uses this to decide between the forced reveal
     *  screen and Home on relaunch. Null/0 rows read as not-yet-acknowledged. */
    fun isMnemonicAcknowledged(): Boolean =
        db.settingsQueries.selectMnemonicAcknowledged().executeAsOneOrNull() == 1L

    /** Persist the mnemonic acknowledgment so relaunch goes straight to Home. */
    fun acknowledgeMnemonic() {
        db.settingsQueries.upsertMnemonicAcknowledged(mnemonic_acknowledged = 1L)
    }

    // --- Packs: catalog, install, search, uninstall ---

    /** All installed packs, newest-imported first. Drives the Settings → Packs
     *  list and the Onboarding "already installed" check. */
    fun installedPacks(): List<InstalledPack> =
        db.packsQueries.selectAllPacks().executeAsList().map { it.toInstalled() }

    /** True when [packId] is already installed (used by the onboarding one-tap
     *  install to skip a redundant re-import). */
    fun isPackInstalled(packId: String): Boolean =
        db.packsQueries.selectPackById(packId).executeAsOneOrNull() != null

    /** Catalog-driven import: delegates to [PackImporter] with the repo's clock.
     *  Verifies SHA-256 first; throws [ChecksumMismatchException] on mismatch. */
    fun importPack(
        packBytes: ByteArray,
        entry: CatalogEntry,
        nowMillis: Long,
    ): ImportResult = PackImporter.importPack(db, packBytes, entry, nowMillis)

    /** Side-loaded import (OS file picker): no checksum to verify. */
    fun importSideLoadedPack(
        packBytes: ByteArray,
        fileName: String,
        nowMillis: Long,
    ): ImportResult = PackImporter.importSideLoadedPack(db, packBytes, fileName, nowMillis)

    /**
     * Searches installed products by name/brand via the FTS5 index. Query terms
     * are quoted+prefixed here (not in SQL) so raw user input can't inject FTS5
     * query syntax. Returns at most [limit] matches ranked by relevance.
     *
     * The post-install "products from pack searchable in logging flow"
     * acceptance criterion runs through this.
     */
    fun searchFoods(query: String, limit: Long = 50L): List<ProductMacros> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val ftsQuery = buildFtsQuery(trimmed)
        return db.products_ftsQueries.searchProductsFts(query = ftsQuery, limit = limit)
            .executeAsList()
            .map { it.toMacros() }
    }

    /**
     * Uninstalls [packId]: deletes every product whose `pack_id` matches, prunes
     * their FTS5 rows, and removes the `packs` row. The `sources` row is dropped
     * only if no remaining product references it (issue-07 criterion 7: "pack's
     * source row removed if no other packs reference it"). Seed foods
     * (`pack_id = NULL`) and other packs' rows are left intact.
     */
    fun uninstallPack(packId: String) {
        db.transaction {
            // Collect the source ids this pack's products used, then drop the
            // pack's products + FTS rows.
            val packSourceIds = db.productsQueries.sourcesForPack(packId).executeAsList()
            db.products_ftsQueries.deleteProductFtsByPack(packId)
            db.productsQueries.deleteProductsByPack(packId)
            db.packsQueries.deletePack(packId)
            // Reclaim a source row only if nothing references it anymore.
            for (sourceId in packSourceIds) {
                val refs = db.sourcesQueries.countProductsBySource(sourceId).executeAsOne()
                if (refs == 0L) {
                    db.sourcesQueries.deleteSource(sourceId)
                }
            }
        }
    }

    private fun Packs.toInstalled() = InstalledPack(
        id = id,
        name = name,
        version = version,
        region = region,
        itemCount = item_count.toInt(),
        byteSize = byte_size,
        importedAtMillis = imported_at,
        sha256 = sha256,
        license = license,
        attribution = attribution,
    )

    private fun SearchProductsFts.toMacros() = ProductMacros(
        id = id,
        name = name,
        kcalPer100g = kcal_per_100g,
        proteinGPer100g = protein_g_per_100g,
        carbsGPer100g = carbs_g_per_100g,
        fatGPer100g = fat_g_per_100g,
    )

    /** Builds an FTS5 query that ANDs each whitespace-split term as a prefix
     *  token ("term"*). Quoting prevents FTS5-syntax injection from raw input;
     *  the prefix star keeps "chee" matching "cheerios". */
    private fun buildFtsQuery(input: String): String =
        input.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { term -> "\"${term.replace("\"", "\"\"")}\"*" }

    private fun Products.toMacros() = ProductMacros(
        id = id,
        name = name,
        kcalPer100g = kcal_per_100g,
        proteinGPer100g = protein_g_per_100g,
        carbsGPer100g = carbs_g_per_100g,
        fatGPer100g = fat_g_per_100g,
    )

    private fun Log_entries.toLogged() = LoggedEntry(
        id = id,
        productId = product_id,
        quantityGrams = quantity_grams,
        timestampMillis = timestamp,
    )

    private fun SelectLogEntriesForDayWithProduct.toDiaryEntry() = DiaryEntry(
        id = id,
        productId = product_id,
        name = product_name,
        quantity = quantity,
        unit = unit,
        quantityGrams = quantity_grams,
        mealType = meal_type,
        timestampMillis = timestamp,
        kcalPer100g = kcal_per_100g,
        proteinGPer100g = protein_g_per_100g,
        carbsGPer100g = carbs_g_per_100g,
        fatGPer100g = fat_g_per_100g,
    )
}