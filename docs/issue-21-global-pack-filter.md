---
title: "Global pack filter"
type: issue
status: ready-for-agent
created: 2026-06-26
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Implement a real `GLOBAL` region filter in `OffReader` so `pack-compiler compile --region GLOBAL` produces the "Global brands starter pack" the spec names, instead of every product with nutrition fields.

Today `OffReader.REGION_TAGS["GLOBAL"]` is `null` (`tools/pack-compiler/src/main/kotlin/com/github/nols1000/nutaru/packcompiler/OffReader.kt:42`), which `regionMatches` treats as "no country filter" — it keeps every product. The inline comment admits this is only moot for the `demo` path. On the real Open Food Facts export this would keep millions of rows, and `PackCompiler.writePack` materializes `products.toList()` (`tools/pack-compiler/src/main/kotlin/com/github/nols1000/nutaru/packcompiler/PackCompiler.kt:30`), so a real `--region GLOBAL` run either OOMs or emits a hundreds-of-MB pack that is not a "global brands" pack at all.

This is why the `packs-publish` workflow (`.github/workflows/packs-publish.yml`) ships only the 5 regional packs (US, EU-MIX, UK, JP, BR) and omits GLOBAL. Once this issue lands, GLOBAL gets added back to the workflow's `regions` list and the catalog returns to 6 entries.

Define what "global brand" means, then implement it as a filter `OffReader.regionMatches` can apply. Candidate definitions (pick one, justify it in the issue resolution):

- A product whose `brands` is non-empty **and** whose brand appears across `>= N` distinct `countries_tags` values in the export (brand-dominant SKUs). Needs a pre-pass or a curated top-brands allowlist, since `OffReader` streams row-by-row and can't see cross-row counts without one.
- A curated allowlist of top global brands (e.g. Coca-Cola, Nestle, PepsiCo, Unilever, Mondelez, Danone, Kellogg's, Mars, General Mills, Kraft Heinz). Simple, deterministic, no pre-pass, but needs a maintained list and a defined refresh policy.
- Products with no `countries_tags` (truly uncategorized) — likely noisy/empty, probably not the intent.

## Acceptance criteria

- [ ] `GLOBAL` filter semantics defined and documented inline in `OffReader` (what counts as a "global brand" SKU)
- [ ] `OffReader.REGION_TAGS["GLOBAL"]` no longer `null`; `regionMatches("GLOBAL", ...)` applies the real filter
- [ ] `compile --region GLOBAL` against the real OFF JSONL export produces a pack sized comparably to the regional packs (~50–100k items, tens of MB), no OOM
- [ ] `GLOBAL` added back to the `regions` array in `.github/workflows/packs-publish.yml` with id `global` and name `Global brands starter pack`
- [ ] Catalog entry for `global` carries the correct name, region `GLOBAL`, and a Releases download URL
- [ ] A test in `tools/pack-compiler/src/test/.../` covers the GLOBAL filter logic (unit test on `regionMatches`/`parseProduct` with synthetic rows, or a golden-pack slice)

## Blocked by

None — the pack compiler ships with `docs/issue-06-pack-compiler-catalog-hosting.md`; this extends its `GLOBAL` region.

## Notes

- If the chosen definition needs cross-row aggregation (e.g. brand-to-country counts), prefer a small pre-pass or a committed allowlist over loading the whole export into memory — keep the compiler's streaming property intact.
- The `demo` path (`DemoData.regions`) already has a GLOBAL entry with synthetic products; make sure `demo --region GLOBAL` still works after the filter change, or update `DemoData` to match the new semantics.
- The pack-compiler spec (`docs/pack-compiler-spec.md`) lists `global` among the 6 starter pack ids; this issue is what makes that spec entry real.
