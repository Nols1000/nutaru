package com.github.nols1000.nutaru.pack

/**
 * Pure-Kotlin UTF-8 encode/decode — no `Charsets`/`String(bytes, charset)`,
 * which are JVM-only and absent from KMP `commonMain`.
 *
 * Pack product names span every locale the catalog ships (JP, EU-mix, …), so the
 * codec handles the full Unicode range including surrogate pairs. It matches the
 * JVM `String.toByteArray(UTF_8)` / `String(bytes, UTF_8)` contract that the
 * pack compiler emits, so the KMP twin produces byte-identical pack bytes.
 *
 * Lone surrogates are replaced with U+FFFD on decode (matches the JVM lenient
 * path) and encoded as U+FFFD on encode — pack data is well-formed text.
 */
internal object Utf8 {

    fun encode(s: String, sink: (Byte) -> Unit) {
        var i = 0
        while (i < s.length) {
            val c = s[i].code
            when {
                c <= 0x7F -> {
                    sink(c.toByte())
                    i++
                }
                c <= 0x7FF -> {
                    sink((0xC0 or (c ushr 6)).toByte())
                    sink((0x80 or (c and 0x3F)).toByte())
                    i++
                }
                c in 0xD800..0xDBFF && i + 1 < s.length && s[i + 1].code in 0xDC00..0xDFFF -> {
                    val low = s[i + 1].code
                    val cp = 0x10000 + ((c - 0xD800) shl 10) + (low - 0xDC00)
                    sink((0xF0 or (cp ushr 18)).toByte())
                    sink((0x80 or ((cp ushr 12) and 0x3F)).toByte())
                    sink((0x80 or ((cp ushr 6) and 0x3F)).toByte())
                    sink((0x80 or (cp and 0x3F)).toByte())
                    i += 2
                }
                c in 0xD800..0xDFFF -> {
                    // Lone surrogate — replace with U+FFFD (EF BF BD).
                    sink(0xEF.toByte()); sink(0xBF.toByte()); sink(0xBD.toByte())
                    i++
                }
                else -> {
                    sink((0xE0 or (c ushr 12)).toByte())
                    sink((0x80 or ((c ushr 6) and 0x3F)).toByte())
                    sink((0x80 or (c and 0x3F)).toByte())
                    i++
                }
            }
        }
    }

    fun encodeToByteArray(s: String): ByteArray {
        // Worst case: 4 bytes/char; size up front, trim at the end.
        val buf = ByteArray(s.length * 4)
        var n = 0
        encode(s) { b -> buf[n++] = b }
        return if (n == buf.size) buf else buf.copyOf(n)
    }

    fun decode(bytes: ByteArray, offset: Int, length: Int): String {
        val sb = StringBuilder(length)
        var i = offset
        val end = offset + length
        while (i < end) {
            val b0 = bytes[i].toInt() and 0xFF
            when {
                b0 <= 0x7F -> {
                    sb.append(b0.toChar())
                    i += 1
                }
                b0 and 0xE0 == 0xC0 && i + 1 < end -> {
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    if (b1 and 0xC0 != 0x80) { sb.append(REPLACEMENT); i += 1; continue }
                    sb.append((((b0 and 0x1F) shl 6) or (b1 and 0x3F)).toChar())
                    i += 2
                }
                b0 and 0xF0 == 0xE0 && i + 2 < end -> {
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    val b2 = bytes[i + 2].toInt() and 0xFF
                    if (b1 and 0xC0 != 0x80 || b2 and 0xC0 != 0x80) { sb.append(REPLACEMENT); i += 1; continue }
                    val cp = (((b0 and 0x0F) shl 12) or ((b1 and 0x3F) shl 6) or (b2 and 0x3F))
                    sb.append(cp.toChar())
                    i += 3
                }
                b0 and 0xF8 == 0xF0 && i + 3 < end -> {
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    val b2 = bytes[i + 2].toInt() and 0xFF
                    val b3 = bytes[i + 3].toInt() and 0xFF
                    if (b1 and 0xC0 != 0x80 || b2 and 0xC0 != 0x80 || b3 and 0xC0 != 0x80) {
                        sb.append(REPLACEMENT); i += 1; continue
                    }
                    val cp = (((b0 and 0x07) shl 18) or ((b1 and 0x3F) shl 12) or ((b2 and 0x3F) shl 6) or (b3 and 0x3F))
                    // Encode supplementary codepoint as a UTF-16 surrogate pair.
                    val v = cp - 0x10000
                    sb.append((0xD800 + (v ushr 10)).toChar())
                    sb.append((0xDC00 + (v and 0x3FF)).toChar())
                    i += 4
                }
                else -> {
                    sb.append(REPLACEMENT)
                    i += 1
                }
            }
        }
        return sb.toString()
    }

    private const val REPLACEMENT = '\uFFFD'
}
