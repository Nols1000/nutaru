package com.github.nols1000.nutaru.pack

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * UTF-8 codec regression guard — pins the codepoint-assembly that a Kotlin
 * infix-precedence bug silently broke (`a shl 12 or b shl 6` is NOT
 * `(a shl 12) or (b shl 6)`). The pack format carries international food names
 * (JP, EU-mix), so a wrong decoder would corrupt every non-ASCII product name
 * and the SHA-256 cross-check against the shipped packs would drift.
 *
 * Round-trips every interesting range through [Utf8] and through the full
 * [PackCodec] path (which is what actually exercises the bug), so a future
 * precedence slip fails here, not silently in production.
 */
class Utf8Test {

    @Test
    fun round_trips_ascii_latin_supplementary_and_emoji_through_the_codec() {
        val names = listOf(
            "Banana",                       // ASCII
            "Crème brûlée",                 // Latin-1 supplement (2-byte)
            "—",                            // U+2014 em-dash (3-byte) — the canary
            "sushi 寿司",                   // CJK (3-byte)
            "Donut 🍩",                     // supplementary plane (4-byte surrogate pair)
            "Mix 寿司 — 🍩 crème",          // all together
        )
        val products = names.mapIndexed { i, name ->
            Product(
                id = (i + 1).toLong(),
                barcode = null,
                nameI18n = linkedMapOf("en" to name),
                brand = null,
                categories = emptyList(),
                kcalPer100g = 0.0, proteinGPer100g = 0.0, carbsGPer100g = 0.0, fatGPer100g = 0.0,
                servings = emptyList(),
                ingredients = null,
                sourceId = 1L,
                license = "L",
                attribution = "A $name", // provenance also carries the unicode
            )
        }
        val decoded = PackCodec.reader(PackCodec.encode(products)).toList()
        assertEquals(products.size, decoded.size)
        products.forEachIndexed { i, expected ->
            assertEquals(expected.nameI18n["en"], decoded[i].nameI18n["en"], "name $i mismatch")
            assertEquals(expected.attribution, decoded[i].attribution, "attribution $i mismatch")
        }
    }

    @Test
    fun decode_replaces_truncated_sequences_with_replacement_char() {
        // 0xE2 is a 3-byte lead with no continuation bytes available (only 'X'
        // follows, which is not a valid continuation) → replacement, then 'X'.
        val truncated = byteArrayOf(0xE2.toByte(), 'X'.code.toByte())
        val s = Utf8.decode(truncated, 0, truncated.size)
        assertEquals('\uFFFD', s[0], "Truncated lead byte → replacement")
        assertEquals('X', s[1], "ASCII after the truncated sequence still decodes")
    }
}
