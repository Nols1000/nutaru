package com.github.nols1000.nutaru.packcompiler

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden test — the "100-row OFF slice -> expected pack bytes golden"
 * acceptance criterion. Loads the shipped `off-sample-100.ndjson` fixture (a
 * deterministic 100-row slice of OFF-shaped NDJSON, mixed regions), compiles a
 * pack from the US-filtered rows, and pins the output's SHA-256.
 *
 * Because the encoder is deterministic (sorted name_i18n / categories) and the
 * fixture is committed, the SHA-256 is a stable golden: any change to the
 * format, field order, or encoding that alters the bytes fails this test. A
 * pinned hash is smaller and less fragile than committing a binary pack file.
 *
 * If the format intentionally changes, update [EXPECTED_US_SHA256] after
 * re-running this test and eyeballing the diff.
 */
class GoldenPackTest {

    private val fixture: Path = Paths.get(
        GoldenPackTest::class.java.getResource("/off-sample-100.ndjson")!!.toURI()
    )

    @Test
    fun compiles_us_filtered_pack_with_pinned_sha256() {
        val tmpDir = Files.createTempDirectory("pack-compiler-golden")
        val packFile = tmpDir.resolve("us.pack")
        try {
            val meta = PackMeta(
                id = "us",
                name = "US golden sample",
                version = "1.0.0",
                region = "US",
                url = "https://example.com/us.pack",
            )
            val result = Files.newInputStream(fixture).use { src ->
                PackCompiler.compileFromOff(src, packFile, meta)
            }

            // 100 fixture rows; every 8th (i % 8 == 0) carries en:united-states -> 13 rows.
            assertEquals(13, result.itemCount, "US filter should keep 13 of 100 fixture rows.")
            // Header + 13 length-prefixed records were written.
            assertTrue(result.byteSize > PackFormat.HEADER_BYTES + 13L * 4)
            // Pack file on disk matches the reported byte size.
            assertEquals(result.byteSize, Files.size(packFile))
            // Pinned golden. Update intentionally alongside a format change.
            assertEquals(EXPECTED_US_SHA256, result.sha256, "Pack SHA-256 drifted from golden — see test doc.")

            // And the pack round-trips through the reader cleanly.
            val decoded = Files.newInputStream(packFile).use { src ->
                PackReader(src).use { it.toList() }
            }
            assertEquals(result.itemCount, decoded.size)
            assertTrue(decoded.all { it.license == PackFormat.LICENSE_ODBL && it.attribution == PackFormat.ATTRIBUTION_OFF })
        } finally {
            Files.walk(tmpDir).sorted(reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private companion object {
        // Pinned golden SHA-256 of the US-filtered pack built from
        // /off-sample-100.ndjson. Drift = an unintended format/encoding change.
        const val EXPECTED_US_SHA256 = "d7114303ef78808a26bbcd8ff78b17cb251e8dcb4a6be6cc0ab331bf48e06334"
    }
}
