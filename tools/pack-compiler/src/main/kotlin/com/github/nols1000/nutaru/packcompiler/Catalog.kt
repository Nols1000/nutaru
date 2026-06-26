package com.github.nols1000.nutaru.packcompiler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * `catalog.json` manifest — see `docs/pack-compiler-spec.md`.
 *
 * One [Catalog] holds a schema tag, an `updated` timestamp, and a list of
 * [Entry]s (one per pack). [upsert] replaces an entry with the same `id` or
 * appends a new one, so the compiler can build the 6 starter packs into a
 * single manifest across separate `compile` invocations.
 *
 * kotlinx.serialization-json handles the JSON; the model is the single source
 * of truth for the manifest shape, so the spec sample and the emitted bytes
 * can't drift.
 */
@Serializable
data class Catalog(
    val schema: String = SCHEMA,
    val updated: String,
    val packs: List<Entry> = emptyList(),
) {
    fun upsert(entry: Entry): Catalog {
        val replaced = packs.map { if (it.id == entry.id) entry else it }
        val next = if (replaced.any { it.id == entry.id }) replaced else replaced + entry
        return copy(packs = next)
    }

    companion object {
        const val SCHEMA = "nutaru.catalog/v1"
    }
}

@Serializable
data class Entry(
    val id: String,
    val name: String,
    val version: String,
    val region: String,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("byte_size") val byteSize: Long,
    val sha256: String,
    val url: String,
    val license: String,
    val attribution: String,
)

fun PackResult.toEntry(): Entry = Entry(
    id = id,
    name = name,
    version = version,
    region = region,
    itemCount = itemCount,
    byteSize = byteSize,
    sha256 = sha256,
    url = url,
    license = license,
    attribution = attribution,
)

private val catalogJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Reads a catalog from [path], or returns an empty one if the file is absent. */
fun readCatalog(path: Path): Catalog =
    if (Files.exists(path)) catalogJson.decodeFromString(Files.readString(path))
    else Catalog(updated = nowIso())

/** Writes [catalog] to [path] as pretty JSON with a trailing newline. */
fun writeCatalog(catalog: Catalog, path: Path) {
    Files.writeString(path, catalogJson.encodeToString(catalog) + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}

/** Best-effort ISO-8601 UTC timestamp. java.time is JVM stdlib — no dep. */
fun nowIso(): String =
    java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toInstant().toString()
