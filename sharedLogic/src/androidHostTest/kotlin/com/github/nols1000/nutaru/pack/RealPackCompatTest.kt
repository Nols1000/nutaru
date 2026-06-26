package com.github.nols1000.nutaru.pack

import com.github.nols1000.nutaru.crypto.Sha256
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-compat with the shipped catalog + packs — proves the KMP [PackCodec]
 * decodes the exact bytes the compiler (`tools/pack-compiler`) emits, and the
 * SHA-256 the import runtime computes matches `public/catalog.json`.
 *
 * This is the app-side half of issue-06's "pack file round-trips through parser
 * cleanly" criterion: the compiler has its own JVM codec; this test pins that
 * the KMP twin agrees on the real, shipped `public/us.pack`. Lives in
 * `androidHostTest` (JVM host) because it reads repo files via `java.nio.file`.
 *
 * The pack file is tiny (4 demo rows) — the real 50–100k packs use the same
 * format, so a clean decode here is sufficient proof of format agreement.
 */
class RealPackCompatTest {

    private val repoRoot = Paths.get(".").toAbsolutePath()
        .takeIf { it.resolve("public/us.pack").toFile().exists() }
        ?: Paths.get("..").toAbsolutePath().normalize()

    private val usPack = repoRoot.resolve("public/us.pack")
    private val catalogJson = repoRoot.resolve("public/catalog.json")

    @Test
    fun kmp_codec_decodes_shipped_us_pack_and_sha256_matches_catalog() {
        assertTrue(Files.exists(usPack), "Missing shipped pack: $usPack")
        val bytes = Files.readAllBytes(usPack)

        // Decode with the KMP codec (the import runtime's parser).
        val products = PackCodec.reader(bytes).toList()
        assertTrue(products.isNotEmpty(), "us.pack decoded to zero products.")
        assertTrue(products.all { it.license == PackFormat.LICENSE_ODBL }, "Every record carries ODbL provenance.")
        assertTrue(products.all { it.attribution == PackFormat.ATTRIBUTION_OFF }, "Every record carries OFF attribution.")

        // SHA-256 the import runtime computes == the manifest entry.
        val manifest = Catalog.decode(String(Files.readAllBytes(catalogJson), Charsets.UTF_8))
        val usEntry = manifest.packs.first { it.id == "us" }
        val actualSha = sha256Hex(bytes)
        assertEquals(usEntry.sha256, actualSha, "Computed SHA-256 must match catalog.json entry for 'us'.")
        assertEquals(usEntry.itemCount, products.size, "Manifest item_count must match decoded record count.")

        // Cross-check against the pure-KMP Sha256 primitive directly — the hex
        // helper builds on it, so this guards against a hex-encoding bug.
        val direct = Sha256.digest(bytes).toHex()
        assertEquals(actualSha, direct)
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append("0123456789abcdef"[v ushr 4]).append("0123456789abcdef"[v and 0x0F])
        }
        return sb.toString()
    }
}
