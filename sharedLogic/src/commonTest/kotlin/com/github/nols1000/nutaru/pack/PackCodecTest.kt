package com.github.nols1000.nutaru.pack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pack codec round-trip — the parse half of issue-07 criterion 9
 * ("FlatBuffer stream parse → SQL bulk insert round-trip").
 *
 * The pack transport format is a length-prefixed record stream (see
 * `docs/pack-compiler-spec.md`); [PackCodec] is the pure-KMP codec the import
 * runtime decodes with. This test pins the framing invariants the import path
 * depends on — header magic/version/record_count, one-record-at-a-time
 * iteration, and field-for-field decode fidelity across nullable strings,
 * multi-lang names, empty lists, and servings.
 *
 * The compiler (`tools/pack-compiler`) emits the same format from a JVM-only
 * codec; a separate JVM test reads the real `public/us.pack` to prove the two
 * implementations agree byte-for-byte.
 */
class PackCodecTest {

    @Test
    fun round_trips_a_varied_set_of_products() {
        val products = listOf(
            Product(
                id = 1L,
                barcode = "0049000002871",
                nameI18n = linkedMapOf("en" to "Cheerios", "fr" to "Cheerios FR"),
                brand = "General Mills",
                categories = listOf("breakfast-cereals", "oat-cereal"),
                kcalPer100g = 366.0,
                proteinGPer100g = 7.2,
                carbsGPer100g = 73.2,
                fatGPer100g = 5.4,
                servings = listOf(Serving("1 cup (28g)", 28.0)),
                ingredients = "Whole grain oats, sugar, oat bran",
                sourceId = PackFormat.SOURCE_ID_OFF,
                license = PackFormat.LICENSE_ODBL,
                attribution = PackFormat.ATTRIBUTION_OFF,
            ),
            Product(
                id = 2L,
                barcode = null,
                nameI18n = linkedMapOf("en" to "Generic tap water"),
                brand = null,
                categories = emptyList(),
                kcalPer100g = 0.0,
                proteinGPer100g = 0.0,
                carbsGPer100g = 0.0,
                fatGPer100g = 0.0,
                servings = emptyList(),
                ingredients = null,
                sourceId = PackFormat.SOURCE_ID_OFF,
                license = PackFormat.LICENSE_ODBL,
                attribution = PackFormat.ATTRIBUTION_OFF,
            ),
        )

        val bytes = PackCodec.encode(products)

        // Header invariants: magic, version, count, reserved.
        assertEquals(PackFormat.MAGIC, asciiString(bytes, 0, 8))
        assertEquals(PackFormat.VERSION, readBeInt(bytes, 8))
        assertEquals(products.size, readBeInt(bytes, 12))
        assertEquals(0, readBeInt(bytes, 16))

        val reader = PackCodec.reader(bytes)
        assertEquals(products.size, reader.recordCount)
        val decoded = reader.toList()
        assertEquals(products.size, decoded.size)
        products.forEachIndexed { i, expected ->
            assertEquals(expected, decoded[i], "Record $i mismatch")
        }
        // Iterator is exhausted exactly at recordCount.
        assertFailsWith<NoSuchElementException> { reader.next() }
    }

    @Test
    fun nullable_string_round_trips_as_null_and_empty_distinctly() {
        val products = listOf(
            Product(1L, null, linkedMapOf("en" to "A"), null, emptyList(), 0.0, 0.0, 0.0, 0.0, emptyList(), null, 1L, "L", "S"),
            Product(2L, "", linkedMapOf("en" to "B"), "", emptyList(), 0.0, 0.0, 0.0, 0.0, emptyList(), "", 1L, "L", "S"),
        )
        val decoded = PackCodec.reader(PackCodec.encode(products)).toList()
        assertNull(decoded[0].barcode)
        assertEquals("", decoded[1].barcode)
        assertTrue(decoded[0].brand == null && decoded[1].brand == "")
    }

    @Test
    fun encode_is_deterministic_so_a_pinned_sha256_is_stable() {
        val products = listOf(
            Product(1L, "1", linkedMapOf("en" to "A"), "B", listOf("c"), 1.0, 2.0, 3.0, 4.0, listOf(Serving("g", 1.0)), "i", 1L, "L", "S"),
        )
        val a = PackCodec.encode(products)
        val b = PackCodec.encode(products)
        assertTrue(a.contentEquals(b), "Same input must produce byte-identical output.")
    }

    @Test
    fun reader_rejects_bad_magic_and_unknown_version() {
        val badVersion = ByteArray(PackFormat.HEADER_BYTES)
        ascii("NUTARUPK").copyInto(badVersion, 0)
        badVersion[11] = 999.toByte() // version = 999 (high byte)
        assertFailsWith<IllegalArgumentException> { PackCodec.reader(badVersion).toList() }

        val badMagic = ascii("NOTARUPK") + ByteArray(12)
        assertFailsWith<IllegalArgumentException> { PackCodec.reader(badMagic).toList() }
    }

    private fun readBeInt(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 24) or
            ((b[off + 1].toInt() and 0xFF) shl 16) or
            ((b[off + 2].toInt() and 0xFF) shl 8) or
            (b[off + 3].toInt() and 0xFF)

    // ASCII helpers — pack magic is 7-bit; avoids JVM-only Charsets in commonTest.
    private fun ascii(s: String): ByteArray = ByteArray(s.length) { i -> s[i].code.toByte() }
    private fun asciiString(b: ByteArray, off: Int, len: Int): String =
        buildString { for (i in off until off + len) append((b[i].toInt() and 0xFF).toChar()) }
}
