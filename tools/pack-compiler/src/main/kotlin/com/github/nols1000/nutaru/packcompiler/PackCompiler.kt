package com.github.nols1000.nutaru.packcompiler

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * Orchestrates one pack build: stream [products] into a pack file, then return
 * a [PackResult] carrying the manifest fields (SHA-256, byte size, item count).
 *
 * The SHA-256 is computed in-stream over the pack bytes via a
 * [DigestOutputStream] wrapper so the compiler never reads the file back —
 * one pass writes the pack and yields the checksum.
 */
object PackCompiler {

    /** Writes [products] to [out] (the raw pack stream) and returns manifest
     *  fields. The caller owns [out] (close responsibility is the caller's). */
    fun writePack(products: Sequence<Product>, out: OutputStream, meta: PackMeta): PackResult {
        val sha = MessageDigest.getInstance("SHA-256")
        val counting = CountingOutputStream(out)
        val digesting = java.security.DigestOutputStream(counting, sha)
        // Materialize once so the header's record_count is known up front.
        // A pack is 25–50 MB; the Product list is the same data the writer
        // emits anyway, so buffering it once is fine for the compiler. The
        // import runtime streams without buffering — see spec.
        val materialized = products.toList()
        PackWriter(digesting).use { w ->
            w.writeHeader(materialized.size)
            materialized.forEach { w.writeRecord(it) }
        }
        digesting.flush()
        return PackResult(
            id = meta.id,
            name = meta.name,
            version = meta.version,
            region = meta.region,
            itemCount = materialized.size,
            byteSize = counting.bytesWritten,
            sha256 = sha.digest().joinToString("") { "%02x".format(it) },
            url = meta.url,
            license = meta.license,
            attribution = meta.attribution,
        )
    }

    /** Convenience: write to [packFile] (creating/truncating), return result. */
    fun compileToFile(products: Sequence<Product>, packFile: Path, meta: PackMeta): PackResult {
        Files.newOutputStream(packFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
            return writePack(products, out, meta)
        }
    }

    /** Compile straight from an OFF export stream, applying [PackMeta.region]. */
    fun compileFromOff(offExport: InputStream, packFile: Path, meta: PackMeta): PackResult =
        compileToFile(OffReader.read(offExport, meta.region), packFile, meta)
}

/** Identifying + manifest metadata for one pack build. */
data class PackMeta(
    val id: String,
    val name: String,
    val version: String,
    val region: String,
    val url: String,
    val license: String = PackFormat.LICENSE_ODBL,
    val attribution: String = PackFormat.ATTRIBUTION_OFF,
)

/** OutputStream that counts bytes written — gives the manifest `byte_size`
 *  without a second file read. */
private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
    var bytesWritten: Long = 0L
    override fun write(b: Int) { delegate.write(b); bytesWritten++ }
    override fun write(b: ByteArray, off: Int, len: Int) { delegate.write(b, off, len); bytesWritten += len }
    override fun flush() = delegate.flush()
    override fun close() = delegate.close()
}
