# nutaru Pack Format + Catalog Manifest Spec

Defines the offline pack transport format produced by `tools/pack-compiler/` and
consumed by the app-side pack import runtime (issue #07). One file, two
sections: the **pack binary format** and the **catalog manifest**.

## Design goals

- **mmap-able, iterable without full load.** A length-prefixed record stream: a
  fixed header, then `[len][payload]` per record. A reader (or mmap view) can
  skip records without decoding them, so low-RAM devices stream 50–100k items
  without loading the file.
- **No codegen, no transport-only runtime dependency.** The codec is pure
  Kotlin (~150 LOC) shared by the JVM CLI and, via the spec, re-implemented in
  `sharedLogic/commonMain` by the import runtime. Kotlin stdlib only.
- **Self-describing provenance.** Every record carries `license` + `attribution`
  text so ODbL requirements survive export, transport, and import unchanged.

> TODO: The PRD names FlatBuffers as the transport format. This spec uses a
> length-prefixed binary record stream instead because it delivers the required
> properties (mmap-able, streamable, no full load) with zero codegen and zero
> transport runtime deps, and the codec is small enough to pin with a round-trip
> test. Ceiling: no zero-copy field access and no automatic schema evolution —
> fields are read in fixed order, so adding a field bumps `version` and the
> reader must branch on it. Upgrade path: if field access patterns on device
> ever demand zero-copy reads, wrap each record's payload in a FlatBuffer and
> keep the outer length-prefix frame; the frame is the part the import runtime
> depends on, the payload encoding is swappable per version.

## Pack binary format

All multi-byte integers are **big-endian**. Integers are unsigned unless noted.

### File layout

```
+----------------------+  offset 0
| Header (20 bytes)    |
+----------------------+
| Record 0             |   [4-byte payload_len][payload_len bytes]
| Record 1             |
| ...                  |
| Record N-1           |
+----------------------+  offset = 20 + sum(payload_len + 4)
```

### Header (20 bytes)

| Field         | Bytes | Type    | Value                                   |
|---------------|-------|---------|-----------------------------------------|
| `magic`       | 8     | bytes   | `NUTARUPK` (0x4E5554415255504B)         |
| `version`     | 4     | uint32  | 1                                       |
| `record_count`| 4     | uint32  | number of records that follow           |
| `reserved`    | 4     | uint32  | 0 (alignment / future use)              |

A reader validates `magic`, checks `version` is one it supports, then loops
`record_count` times reading a 4-byte length + that many payload bytes. The
import runtime streams records one at a time and bulk-inserts each into the
SQLDelight `products` table; the pack file itself is never held in memory.

### Record payload — Product

Fields are encoded in the fixed order below. "string" and "list<T>" use the
encodings defined after the table.

| Field                    | Encoding      | Notes                                              |
|--------------------------|---------------|----------------------------------------------------|
| `id`                     | int64 (8)     | OFF `_id` (stable within a pack).                  |
| `barcode`                | string        | OFF `code`. Nullable.                              |
| `name_i18n`              | map<string,string> | lang code -> name. At least one entry; `en` fallback. |
| `brand`                  | string        | OFF `brands`. Nullable.                            |
| `categories`             | list<string>  | OFF `categories_tags` (or split `categories`).     |
| `kcal_per_100g`          | double (8)    | OFF `nutriments.energy-kcal_100g`.                 |
| `protein_g_per_100g`     | double (8)    | OFF `nutriments.proteins_100g`.                    |
| `carbs_g_per_100g`       | double (8)    | OFF `nutriments.carbohydrates_100g`.               |
| `fat_g_per_100g`         | double (8)    | OFF `nutriments.fat_100g`.                         |
| `servings`               | list<serving> | Each: `{ unit: string, grams: double }`.           |
| `ingredients`            | string        | OFF `ingredients_text`. Nullable.                  |
| `source_id`              | int64 (8)     | Reference into the app `sources` table (OFF = 1).  |
| `license`                | string        | e.g. `ODbL 1.0`.                                   |
| `attribution`            | string        | e.g. `Open Food Facts — https://world.openfoodfacts.org`. |

### Primitive encodings

- **string** (nullable): `[2-byte length, int16 signed][UTF-8 bytes]`.
  `length == -1` -> null. `length == 0` -> empty string. Else `length` UTF-8
  bytes. (2-byte length caps a single string at 32 KiB — adequate for product
  names, brand lists, and ingredients text in OFF's corpus. TODO: bump to int32
  if a field ever exceeds it.)
- **list&lt;T&gt;**: `[4-byte count, uint32][count × item]`.
- **map&lt;string,string&gt;**: `[4-byte count, uint32][count × (key string + value string)]`.
- **double**: IEEE 754 big-endian (8 bytes).
- **int64 / int32 / uint32**: big-endian, two's complement for signed.

### Determinism

The encoder writes fields in the declared order with no padding, no field tags,
no maps-of-unknown-order: `name_i18n` and `categories` entries are emitted in
sorted key/string order so a given input always produces byte-identical output.
This is what makes the 100-row golden test a pinned SHA-256 rather than a
fragile committed binary.

## Catalog manifest (`catalog.json`)

Published at `nutaru.app/catalog.json` via GitHub Pages. Plain HTTPS GET, no
auth. One entry per pack.

```json
{
  "schema": "nutaru.catalog/v1",
  "updated": "2026-06-26T00:00:00Z",
  "packs": [
    {
      "id": "us",
      "name": "US starter pack",
      "version": "1.0.0",
      "region": "US",
      "item_count": 52103,
      "byte_size": 28345611,
      "sha256": "9f2a...e1c4",
      "url": "https://github.com/nutaru/nutaru/releases/download/packs-v1/us.pack",
      "license": "ODbL 1.0",
      "attribution": "Open Food Facts — https://world.openfoodfacts.org"
    }
  ]
}
```

| Field        | Type   | Notes                                                          |
|--------------|--------|----------------------------------------------------------------|
| `schema`     | string | `nutaru.catalog/v1`. Bumped on breaking manifest shape change. |
| `updated`    | string | ISO-8601 UTC timestamp of this manifest build.                 |
| `id`         | string | Stable pack id (`us`, `eu-mix`, `uk`, `jp`, `br`, `global`).    |
| `name`       | string | Human-readable.                                                |
| `version`    | string | Semver. Bumped per re-export.                                  |
| `region`     | string | Region tag the compiler filtered on.                           |
| `item_count` | int    | `record_count` from the pack header.                           |
| `byte_size`  | int    | Pack file size in bytes.                                       |
| `sha256`     | string | Hex SHA-256 of the pack file. App verifies post-download.      |
| `url`        | string | HTTPS download URL (GitHub Releases / Hugging Face).           |
| `license`    | string | Carried into the app `sources` row on import.                  |
| `attribution`| string | Carried into the app `sources` row on import.                  |

## CLI

```
pack-compiler compile \
  --input <off-export.ndjson> \
  --region <US|EU-MIX|UK|JP|BR|GLOBAL> \
  --out <pack-file> \
  --version <semver> \
  --url <download-url> \
  [--source-id 1] \
  [--manifest <catalog.json>]
```

`compile` reads the OFF newline-delimited JSON export, filters to products
whose `countries_tags` match `--region`, encodes a pack, computes SHA-256 +
byte size + item count, and prints a manifest entry as JSON. With `--manifest`,
it upserts the entry into the catalog file.

```
pack-compiler demo --out-dir <dir>
```

`demo` builds 6 small synthetic packs (one per starter region) and writes
`<dir>/catalog.json` + `<dir>/<id>.pack`. Used to stand up the catalog structure
and GitHub Pages source without the multi-GB OFF export; production replaces
these with `compile` runs against the real export. See `docs/issue-06...md`.

## Hosting (release-time, human/CI step)

1. Run `compile` per region against the OFF bulk export -> 6 `.pack` files.
2. Upload the 6 `.pack` files to a GitHub Release tagged `packs-v1` (free tier,
   2 GB per asset, plenty for 25–50 MB packs). Hugging Face dataset is the
   fallback if GitHub Releases size limits ever bind.
3. `demo` or `compile --manifest` produces `catalog.json`; commit it to the
   GitHub Pages source dir (`public/`) and push — Pages serves it at
   `nutaru.app/catalog.json` once the custom domain is wired.

> TODO: the two steps above need hosting credentials + the `nutaru.app` domain
> wired to GitHub Pages. The code (`compile`, `demo`, manifest writer) and the
> `public/catalog.json` committed by `demo` are the code deliverable; the
> upload + DNS steps are run by a human or CI on release.
