package com.github.nols1000.nutaru.pack

/**
 * Constants for the nutaru pack transport format — see `docs/pack-compiler-spec.md`.
 *
 * The format is a length-prefixed record stream: `header(20) + N × [4-byte
 * payload_len][payload]`. A reader can skip any record by reading the 4-byte
 * length and advancing, which is what makes the file iterable without a full
 * load. These constants must match the compiler in `tools/pack-compiler`.
 */
object PackFormat {
    const val MAGIC = "NUTARUPK"
    const val VERSION = 1
    const val HEADER_BYTES = 20

    /** ODbL provenance, embedded in every record. The OFF source row in the
     *  app `sources` table is id 1. */
    const val SOURCE_ID_OFF = 1L
    const val LICENSE_ODBL = "ODbL 1.0"
    const val ATTRIBUTION_OFF = "Open Food Facts — https://world.openfoodfacts.org"
}

/** One product record — the shape the compiler emits and the import runtime
 *  decodes. Fields mirror the app `products` table plus the transport-only
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

/**
 * Pure-KMP codec for the pack format — the import runtime's parse seam.
 *
 * The compiler (`tools/pack-compiler`) ships a JVM-only encoder/decoder built
 * on `DataInputStream`; this is the KMP twin so `commonMain` (and `commonTest`)
 * can decode pack bytes with no platform I/O. A JVM test reads the real
 * `public/us.pack` to prove the two agree byte-for-byte.
 *
 * All primitives are big-endian, matching `DataOutputStream`. Strings are
 * 2-byte length-prefixed (-1 / 0xFFFF = null); the 0x7FFF byte cap matches the
 * compiler so a record encoded by either side decodes by the other.
 */
object PackCodec {

    /** Encodes [products] as a complete pack byte stream. Deterministic for a
     *  given record list: callers pass `nameI18n` / `categories` already in the
     *  desired order (the compiler sorts them). */
    fun encode(products: List<Product>): ByteArray {
        val sink = ByteSink()
        sink.writeAscii(PackFormat.MAGIC)
        sink.writeInt(PackFormat.VERSION)
        sink.writeInt(products.size)
        sink.writeInt(0) // reserved
        products.forEach { p ->
            val payload = encodePayload(p)
            sink.writeInt(payload.size)
            sink.write(payload)
        }
        return sink.toByteArray()
    }

    /** Opens a streaming [PackReader] over [bytes]; decodes one record per
     *  [PackReader.next] call, never holds the decoded list. */
    fun reader(bytes: ByteArray): PackReader = PackReader(bytes)

    private fun encodePayload(p: Product): ByteArray {
        val d = ByteSink()
        d.writeLong(p.id)
        d.writeString(p.barcode)
        d.writeStringMap(p.nameI18n)
        d.writeString(p.brand)
        d.writeStringList(p.categories)
        d.writeDouble(p.kcalPer100g)
        d.writeDouble(p.proteinGPer100g)
        d.writeDouble(p.carbsGPer100g)
        d.writeDouble(p.fatGPer100g)
        d.writeServings(p.servings)
        d.writeString(p.ingredients)
        d.writeLong(p.sourceId)
        d.writeString(p.license)
        d.writeString(p.attribution)
        return d.toByteArray()
    }
}

/** Streaming decoder over a backing [ByteArray]. One record per [next]; throws
 *  on a truncated frame or magic/version mismatch so the import runtime can
 *  fail the pack fast. */
class PackReader internal constructor(private val bytes: ByteArray) : Iterator<Product> {
    private var pos = 0
    private var remaining: Int
    /** Total records declared in the header. */
    val recordCount: Int

    init {
        require(bytes.size >= PackFormat.HEADER_BYTES) { "Truncated pack header (${bytes.size} bytes)." }
        val magic = asciiString(bytes, 0, 8)
        require(magic == PackFormat.MAGIC) { "Bad pack magic: expected ${PackFormat.MAGIC}, got $magic" }
        val version = readBeInt(8)
        require(version == PackFormat.VERSION) { "Unsupported pack version $version (supported ${PackFormat.VERSION})" }
        recordCount = readBeInt(12)
        remaining = recordCount
        pos = PackFormat.HEADER_BYTES
        readBeInt(16) // reserved
    }

    override fun hasNext(): Boolean = remaining > 0

    override fun next(): Product {
        if (!hasNext()) throw NoSuchElementException("Pack records exhausted.")
        val len = readBeInt()
        require(len >= 0) { "Negative record length: $len" }
        val payload = readBytes(len)
        remaining--
        return decodePayload(payload)
    }

    fun toList(): List<Product> = ArrayList<Product>(recordCount).apply {
        while (hasNext()) add(next())
    }

    private fun decodePayload(b: ByteArray): Product {
        val c = ByteCursor(b)
        val id = c.readLong()
        val barcode = c.readString()
        val nameI18n = c.readStringMap()
        val brand = c.readString()
        val categories = c.readStringList()
        val kcal = c.readDouble()
        val protein = c.readDouble()
        val carbs = c.readDouble()
        val fat = c.readDouble()
        val servings = c.readServings()
        val ingredients = c.readString()
        val sourceId = c.readLong()
        val license = c.readString() ?: error("license is required (record $id)")
        val attribution = c.readString() ?: error("attribution is required (record $id)")
        return Product(id, barcode, nameI18n, brand, categories, kcal, protein, carbs, fat, servings, ingredients, sourceId, license, attribution)
    }

    private fun readBeInt(): Int {
        val v = readBeInt(pos)
        pos += 4
        return v
    }

    private fun readBeInt(off: Int): Int =
        ((bytes[off].toInt() and 0xFF) shl 24) or
            ((bytes[off + 1].toInt() and 0xFF) shl 16) or
            ((bytes[off + 2].toInt() and 0xFF) shl 8) or
            (bytes[off + 3].toInt() and 0xFF)

    private fun readBytes(n: Int): ByteArray {
        require(pos + n <= bytes.size) { "Truncated record: need $n bytes at $pos, have ${bytes.size - pos}." }
        val out = bytes.copyOfRange(pos, pos + n)
        pos += n
        return out
    }
}

// --- big-endian sink + cursor (pure-KMP twin of java.io.DataOutputStream) ----

private class ByteSink {
    private var buf = ByteArray(64)
    private var size = 0

    fun toByteArray(): ByteArray = buf.copyOf(size)

    private fun ensure(n: Int) {
        if (size + n <= buf.size) return
        var cap = buf.size
        while (cap < size + n) cap *= 2
        buf = buf.copyOf(cap)
    }

    fun writeByte(v: Int) {
        ensure(1)
        buf[size++] = v.toByte()
    }

    fun write(b: ByteArray) {
        ensure(b.size)
        b.copyInto(buf, size)
        size += b.size
    }

    fun writeAscii(s: String) = write(asciiBytes(s))

    fun writeInt(v: Int) {
        writeByte((v ushr 24) and 0xFF)
        writeByte((v ushr 16) and 0xFF)
        writeByte((v ushr 8) and 0xFF)
        writeByte(v and 0xFF)
    }

    fun writeLong(v: Long) {
        writeInt((v ushr 32).toInt())
        writeInt(v.toInt())
    }

    fun writeDouble(v: Double) = writeLong(v.toRawBits())

    fun writeShort(v: Int) {
        writeByte((v ushr 8) and 0xFF)
        writeByte(v and 0xFF)
    }

    fun writeString(s: String?) {
        if (s == null) {
            writeShort(-1)
        } else {
            val b = Utf8.encodeToByteArray(s)
            require(b.size <= 0x7FFF) { "String too long (${b.size} > 32767 bytes): ${s.take(40)}…" }
            writeShort(b.size)
            write(b)
        }
    }

    fun writeStringList(xs: List<String>) {
        writeInt(xs.size)
        xs.forEach { writeString(it) }
    }

    fun writeStringMap(m: Map<String, String>) {
        writeInt(m.size)
        m.forEach { (k, v) -> writeString(k); writeString(v) }
    }

    fun writeServings(xs: List<Serving>) {
        writeInt(xs.size)
        xs.forEach { writeString(it.unit); writeDouble(it.grams) }
    }
}

private class ByteCursor(private val b: ByteArray) {
    private var pos = 0
    fun readInt(): Int {
        val v = ((b[pos].toInt() and 0xFF) shl 24) or
            ((b[pos + 1].toInt() and 0xFF) shl 16) or
            ((b[pos + 2].toInt() and 0xFF) shl 8) or
            (b[pos + 3].toInt() and 0xFF)
        pos += 4
        return v
    }
    fun readLong(): Long = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFFFFFFFFL)
    fun readDouble(): Double = Double.fromBits(readLong())
    fun readShort(): Int {
        val v = ((b[pos].toInt() and 0xFF) shl 8) or (b[pos + 1].toInt() and 0xFF)
        pos += 2
        return v.toShort().toInt() // keep sign so 0xFFFF (null sentinel) reads as -1
    }
    fun readBytes(n: Int): ByteArray {
        val out = b.copyOfRange(pos, pos + n)
        pos += n
        return out
    }
    fun readString(): String? {
        val len = readShort()
        if (len == -1) return null
        require(len >= 0) { "Invalid string length: $len" }
        return Utf8.decode(readBytes(len), 0, len)
    }
    fun readStringList(): List<String> {
        val n = readInt()
        require(n >= 0) { "Invalid list length: $n" }
        return List(n) { readString()!! }
    }
    fun readStringMap(): Map<String, String> {
        val n = readInt()
        require(n >= 0) { "Invalid map length: $n" }
        val out = LinkedHashMap<String, String>(n)
        repeat(n) { out[readString()!!] = readString()!! }
        return out
    }
    fun readServings(): List<Serving> {
        val n = readInt()
        require(n >= 0) { "Invalid servings length: $n" }
        return List(n) { Serving(readString()!!, readDouble()) }
    }
}

// --- ASCII helpers (pack magic is 7-bit ASCII; no Charsets dependency) --------

private fun asciiBytes(s: String): ByteArray {
    val out = ByteArray(s.length)
    for (i in s.indices) out[i] = s[i].code.toByte()
    return out
}

private fun asciiString(b: ByteArray, offset: Int, length: Int): String {
    val sb = StringBuilder(length)
    for (i in offset until offset + length) sb.append((b[i].toInt() and 0xFF).toChar())
    return sb.toString()
}
