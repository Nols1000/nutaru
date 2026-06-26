package com.github.nols1000.nutaru.packcompiler

import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * pack-compiler CLI entry point.
 *
 *   pack-compiler compile --input <off.ndjson> --region US --out us.pack \
 *       --version 1.0.0 --url <url> [--manifest catalog.json]
 *   pack-compiler demo   --out-dir <dir>
 *
 * `compile` produces one pack file from an OFF export and prints (and
 * optionally upserts) a manifest entry. `demo` builds the 6 synthetic starter
 * packs + catalog.json into a dir so the GitHub Pages source can be stood up
 * without the multi-GB OFF export.
 *
 * Arg parsing is a hand-rolled `--key value` loop: the surface is two commands
 * with a handful of flags, so bringing in a CLI library would be more code than
 * it saves. Errors exit non-zero with a one-line message; the Gradle
 * `application` plugin wires `mainClass` to this file.
 */
fun main(args: Array<String>) {
    val cmd = args.firstOrNull() ?: return usage(exit = 2)
    val flags = parseFlags(args.drop(1))
    try {
        when (cmd) {
            "compile" -> compile(flags)
            "demo" -> demo(flags)
            "-h", "--help", "help" -> usage(exit = 0)
            else -> { println("Unknown command: $cmd"); usage(exit = 2) }
        }
    } catch (e: IllegalArgumentException) {
        System.err.println("error: ${e.message}")
        exit(2)
    } catch (e: Exception) {
        System.err.println("error: ${e.message}")
        e.printStackTrace(System.err)
        exit(1)
    }
}

private fun compile(flags: Map<String, String>) {
    val input = flags.req("input").let { Paths.get(it) }
    val region = flags.req("region")
    require(region in OffReader.REGION_TAGS) { "Unknown region: $region (known: ${OffReader.REGION_TAGS.keys})" }
    val out = Paths.get(flags.req("out"))
    val version = flags.req("version")
    val url = flags.req("url")
    val id = flags.get("id") ?: out.fileName.toString().removeSuffix(".pack")
    val name = flags.get("name") ?: "$region starter pack"
    val meta = PackMeta(id, name, version, region, url)

    Files.newInputStream(input).use { src ->
        val result = PackCompiler.compileFromOff(src, out, meta)
        println(JSON.encodeToString(result.toEntry()))
        flags.get("manifest")?.let { manifestPath ->
            val catalog = readCatalog(Paths.get(manifestPath)).upsert(result.toEntry()).copy(updated = nowIso())
            writeCatalog(catalog, Paths.get(manifestPath))
            println("manifest updated: $manifestPath")
        }
    }
}

private fun demo(flags: Map<String, String>) {
    val outDir = flags.req("out-dir").let { Paths.get(it) }
    Files.createDirectories(outDir)
    val updated = nowIso()
    val results = DemoData.regions.map { rp ->
        val packFile = outDir.resolve("${rp.meta.id}.pack")
        val result = PackCompiler.compileToFile(rp.products.asSequence(), packFile, rp.meta)
        println("built ${rp.meta.id}: ${result.itemCount} items, ${result.byteSize} bytes, sha256=${result.sha256}")
        result
    }
    writeCatalog(Catalog(updated = updated, packs = results.map { it.toEntry() }), outDir.resolve("catalog.json"))
    println("catalog written: ${outDir.resolve("catalog.json")}")
}

private fun usage(exit: Int): Nothing {
    println(
        """
        pack-compiler — nutaru pack builder (see docs/pack-compiler-spec.md)

          compile --input <off.ndjson> --region <US|EU-MIX|UK|JP|BR|GLOBAL> \
                  --out <pack-file> --version <semver> --url <download-url> \
                  [--id <pack-id>] [--name <name>] [--manifest <catalog.json>]
          demo    --out-dir <dir>        build 6 synthetic starter packs + catalog.json
        """.trimIndent()
    )
    exit(exit)
}

private fun parseFlags(args: List<String>): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        require(a.startsWith("--")) { "Expected --flag, got: $a" }
        val key = a.removePrefix("--")
        val value = if (i + 1 < args.size && !args[i + 1].startsWith("--")) { i++; args[i] } else ""
        out[key] = value
        i++
    }
    return out
}

private fun Map<String, String>.req(key: String): String =
    this[key] ?: throw IllegalArgumentException("missing --$key")

private val JSON = kotlinx.serialization.json.Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun exit(code: Int): Nothing { kotlin.system.exitProcess(code) }
