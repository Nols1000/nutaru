package com.github.nols1000.nutaru.packcompiler

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * nutaru pack binary format — see `docs/pack-compiler-spec.md`.
 *
 * Pure-Kotlin codec for the length-prefixed record stream. The frame is:
 *   header(20) + N × [4-byte payload_len][payload]
 * A reader can skip any record by reading the 4-byte length and advancing,
 * which is what makes the file mmap-able and iterable without a full load.
 *
 * The encoder is deterministic: `name_i18n` and `categories` are emitted in
 * sorted order, so the same input always produces byte-identical output. That
 * invariant is what lets the 100-row golden test pin a SHA-256 instead of
 * committing a fragile binary fixture.
 */
object PackFormat {

    const val MAGIC = "NUTARUPK"
    const val VERSION = 1
    const val HEADER_BYTES = 20

    /** ODbL provenance, embedded in every record per the issue's licensing
     *  requirement. The OFF source row in the app `sources` table is id 1. */
    const val SOURCE_ID_OFF = 1L
    const val LICENSE_ODBL = "ODbL 1.0"
    const val ATTRIBUTION_OFF = "Open Food Facts — https://world.openfoodfacts.org"
}

/** One product record. The shape the compiler emits and the import runtime
 *  decodes; fields mirror the app `products` table plus the transport-only
 *  `license` / `attribution` / `sourceId` provenance fields. */
data class Product(
    val id: Long,
    val barcode: String?,
    val nameI18n: Map<String, String>,
    val brand: String?,
    val categories: List<String>,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbsGPer100g: Double,
    val fatGPer100g: Double,
    val servings: List<Serving>,
    val ingredients: String?,
    val sourceId: Long,
    val license: String,
    val attribution: String,
) {
    init {
        require(nameI18n.isNotEmpty()) { "Product $id must have at least one name." }
    }
}

data class Serving(val unit: String, val grams: Double)

/** Pack-level outcome of a compile: everything a catalog manifest entry needs. */
data class PackResult(
    val id: String,
    val name: String,
    val version: String,
    val region: String,
    val itemCount: Int,
    val byteSize: Long,
    val sha256: String,
    val url: String,
    val license: String,
    val attribution: String,
)

/** Writes the header + records to [out]. Deterministic for a given record list:
 *  caller passes records with `nameI18n` / `categories` already in the desired
 *  order; [writeRecord] does not re-sort (sorting happens at construction in
 *  [product] / [OffReader] so the in-memory model is the single source of
 *  truth for ordering). */
class PackWriter(out: OutputStream) : AutoCloseable {
    private val data = DataOutputStream(out)

    fun writeHeader(recordCount: Int) {
        data.writeBytes(PackFormat.MAGIC) // 8 bytes, ASCII
        data.writeInt(PackFormat.VERSION)
        data.writeInt(recordCount)
        data.writeInt(0) // reserved
    }

    fun writeRecord(p: Product) {
        val payload = encodePayload(p)
        data.writeInt(payload.size)
        data.write(payload)
    }

    override fun close() = data.close()
}

/** Reads the header + records from [input]. Streaming — decodes one record per
 *  [next] call, never holds the whole file. Throws [IOException] on a truncated
 *  frame or magic mismatch so the import runtime can fail the pack fast. */
class PackReader(input: InputStream) : AutoCloseable, Iterator<Product> {
    private val data = DataInputStream(input)
    private var remaining: Int
    /** Total records declared in the header (independent of how far iteration has progressed). */
    val recordCount: Int

    init {
        val magic = ByteArray(8)
        data.readFully(magic)
        val magicStr = magic.toString(Charsets.US_ASCII)
        require(magicStr == PackFormat.MAGIC) {
            "Bad pack magic: expected ${PackFormat.MAGIC}, got $magicStr"
        }
        val version = data.readInt()
        require(version == PackFormat.VERSION) {
            "Unsupported pack version $version (supported ${PackFormat.VERSION})"
        }
        recordCount = data.readInt()
        remaining = recordCount
        data.readInt() // reserved
    }

    override fun hasNext(): Boolean = remaining > 0

    override fun next(): Product {
        if (!hasNext()) throw NoSuchElementException("Pack records exhausted.")
        val len = data.readInt()
        require(len >= 0) { "Negative record length: $len" }
        val payload = ByteArray(len)
        data.readFully(payload)
        remaining--
        return decodePayload(payload)
    }

    /** Drains the remaining records into a list. Convenience for tests + the
     *  compiler's own validation; the import runtime iterates one at a time. */
    fun toList(): List<Product> = ArrayList<Product>(recordCount).apply {
        while (hasNext()) add(next())
    }

    override fun close() = data.close()
}

// --- payload encode/decode ---------------------------------------------------

private fun encodePayload(p: Product): ByteArray {
    val sink = java.io.ByteArrayOutputStream()
    val d = DataOutputStream(sink)
    d.writeLong(p.id)
    writeString(d, p.barcode)
    writeStringMap(d, p.nameI18n)
    writeString(d, p.brand)
    writeStringList(d, p.categories)
    d.writeDouble(p.kcalPer100g)
    d.writeDouble(p.proteinGPer100g)
    d.writeDouble(p.carbsGPer100g)
    d.writeDouble(p.fatGPer100g)
    writeServings(d, p.servings)
    writeString(d, p.ingredients)
    d.writeLong(p.sourceId)
    writeString(d, p.license)
    writeString(d, p.attribution)
    d.flush()
    return sink.toByteArray()
}

private fun decodePayload(b: ByteArray): Product {
    val d = DataInputStream(java.io.ByteArrayInputStream(b))
    val id = d.readLong()
    val barcode = readString(d)
    val nameI18n = readStringMap(d)
    val brand = readString(d)
    val categories = readStringList(d)
    val kcal = d.readDouble()
    val protein = d.readDouble()
    val carbs = d.readDouble()
    val fat = d.readDouble()
    val servings = readServings(d)
    val ingredients = readString(d)
    val sourceId = d.readLong()
    val license = readString(d) ?: error("license is required (record $id)")
    val attribution = readString(d) ?: error("attribution is required (record $id)")
    return Product(id, barcode, nameI18n, brand, categories, kcal, protein, carbs, fat, servings, ingredients, sourceId, license, attribution)
}

private fun writeString(d: DataOutputStream, s: String?) {
    if (s == null) {
        d.writeShort(-1)
    } else {
        val bytes = s.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 0x7FFF) { "String too long (${bytes.size} > 32767 bytes): ${s.take(40)}…" }
        d.writeShort(bytes.size)
        d.write(bytes)
    }
}

private fun readString(d: DataInputStream): String? {
    val len = d.readShort().toInt()
    if (len == -1) return null
    require(len >= 0) { "Invalid string length: $len" }
    val bytes = ByteArray(len)
    d.readFully(bytes)
    return bytes.toString(Charsets.UTF_8)
}

private fun writeStringList(d: DataOutputStream, xs: List<String>) {
    d.writeInt(xs.size)
    xs.forEach { writeString(d, it) }
}

private fun readStringList(d: DataInputStream): List<String> {
    val n = d.readInt()
    require(n >= 0) { "Invalid list length: $n" }
    return List(n) { readString(d)!! }
}

private fun writeStringMap(d: DataOutputStream, m: Map<String, String>) {
    d.writeInt(m.size)
    m.forEach { (k, v) -> writeString(d, k); writeString(d, v) }
}

private fun readStringMap(d: DataInputStream): Map<String, String> {
    val n = d.readInt()
    require(n >= 0) { "Invalid map length: $n" }
    val out = LinkedHashMap<String, String>(n)
    repeat(n) { out[readString(d)!!] = readString(d)!! }
    return out
}

private fun writeServings(d: DataOutputStream, xs: List<Serving>) {
    d.writeInt(xs.size)
    xs.forEach { writeString(d, it.unit); d.writeDouble(it.grams) }
}

private fun readServings(d: DataInputStream): List<Serving> {
    val n = d.readInt()
    require(n >= 0) { "Invalid servings length: $n" }
    return List(n) { Serving(readString(d)!!, d.readDouble()) }
}
