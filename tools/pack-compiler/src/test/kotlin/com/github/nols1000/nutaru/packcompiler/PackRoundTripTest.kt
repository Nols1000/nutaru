package com.github.nols1000.nutaru.packcompiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pack file round-trip — the "commonTest equivalent (CLI test)" acceptance
 * criterion. Encodes a representative set of [Product]s (covering nullable
 * strings, empty lists, multi-lang names, servings, missing ingredients) into a
 * pack via [PackWriter], reads it back via [PackReader], and asserts the
 * decoded records equal the input field-for-field.
 *
 * Also pins the framing invariants the import runtime depends on: header
 * magic/version/record_count, and that iteration stops at exactly record_count
 * records (no trailing bytes consumed).
 */
class PackRoundTripTest {

    @Test
    fun round_trips_a_varied_set_of_products() {
        val products = listOf(
            Product(
                id = 1L,
                barcode = "0049000002871",
                nameI18n = sortedMapOf("en" to "Cheerios", "fr" to "Cheerios FR"),
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
                nameI18n = sortedMapOf("en" to "Generic tap water"),
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
            Product(
                id = 3L,
                barcode = "",
                nameI18n = sortedMapOf("en" to "Empty barcode", "de" to "Leerer Barcode"),
                brand = "",
                categories = listOf("test"),
                kcalPer100g = 1.5,
                proteinGPer100g = 0.1,
                carbsGPer100g = 0.2,
                fatGPer100g = 0.3,
                servings = listOf(Serving("100g", 100.0), Serving("1 slice (30g)", 30.0)),
                ingredients = "",
                sourceId = 42L,
                license = "Custom License 2.0",
                attribution = "Custom Source",
            ),
        )

        val baos = java.io.ByteArrayOutputStream()
        PackWriter(baos).use { w ->
            w.writeHeader(products.size)
            products.forEach { w.writeRecord(it) }
        }
        val bytes = baos.toByteArray()

        // Header invariants: magic, version, count, reserved.
        assertEquals(PackFormat.MAGIC, bytes.copyOfRange(0, 8).toString(Charsets.US_ASCII))
        val headerInt = { off: Int -> ((bytes[off].toInt() and 0xFF) shl 24) or ((bytes[off + 1].toInt() and 0xFF) shl 16) or ((bytes[off + 2].toInt() and 0xFF) shl 8) or (bytes[off + 3].toInt() and 0xFF) }
        assertEquals(PackFormat.VERSION, headerInt(8))
        assertEquals(products.size, headerInt(12))
        assertEquals(0, headerInt(16))

        PackReader(java.io.ByteArrayInputStream(bytes)).use { reader ->
            assertEquals(products.size, reader.recordCount)
            val decoded = reader.toList()
            assertEquals(products.size, decoded.size)
            products.forEachIndexed { i, expected ->
                assertEquals(expected, decoded[i], "Record $i mismatch")
            }
            // Iterator is exhausted exactly at recordCount — no extra records.
            assertFailsWith<NoSuchElementException> { reader.next() }
        }
    }

    @Test
    fun reader_rejects_bad_magic_and_unknown_version() {
        // Version 999 header on a valid magic.
        val bad = ByteArray(PackFormat.HEADER_BYTES)
        "NUTARUPK".toByteArray(Charsets.US_ASCII).copyInto(bad, 0)
        bad[11] = 999.toByte() // version = 999 (high byte)
        assertFailsWith<IllegalArgumentException> { PackReader(java.io.ByteArrayInputStream(bad)).use { } }

        // Wrong magic.
        val badMagic = "NOTARUPK".toByteArray(Charsets.US_ASCII) + ByteArray(12)
        assertFailsWith<IllegalArgumentException> { PackReader(java.io.ByteArrayInputStream(badMagic)).use { } }
    }

    @Test
    fun nullable_string_round_trips_as_null_and_empty_distinctly() {
        val products = listOf(
            Product(1L, null, sortedMapOf("en" to "A"), null, emptyList(), 0.0, 0.0, 0.0, 0.0, emptyList(), null, 1L, "L", "S"),
            Product(2L, "", sortedMapOf("en" to "B"), "", emptyList(), 0.0, 0.0, 0.0, 0.0, emptyList(), "", 1L, "L", "S"),
        )
        val baos = java.io.ByteArrayOutputStream()
        PackWriter(baos).use { w -> w.writeHeader(products.size); products.forEach { w.writeRecord(it) } }
        val decoded = PackReader(java.io.ByteArrayInputStream(baos.toByteArray())).use { it.toList() }
        assertNull(decoded[0].barcode)
        assertEquals("", decoded[1].barcode)
        assertTrue(decoded[0].brand == null && decoded[1].brand == "")
    }
}
