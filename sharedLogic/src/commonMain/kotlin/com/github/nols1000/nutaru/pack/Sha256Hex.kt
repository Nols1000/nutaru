package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.crypto.Sha256

/**
 * SHA-256 of [bytes] as a lowercase hex string — the format the catalog manifest
 * pins (see `public/catalog.json`). Reuses the pure-KMP [Sha256] primitive so
 * checksum verification works on every target with no platform crypto binding.
 */
internal fun sha256Hex(bytes: ByteArray): String {
    val digest = Sha256.digest(bytes)
    val sb = StringBuilder(digest.size * 2)
    for (b in digest) {
        val v = b.toInt() and 0xFF
        sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
    }
    return sb.toString()
}

private val HEX = "0123456789abcdef".toCharArray()
