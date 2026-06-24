---
title: "Pack compiler + catalog hosting"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Offline Kotlin CLI tool that takes Open Food Facts bulk export as input and produces FlatBuffer pack files plus a catalog manifest entry per pack. Pack file format: length-prefixed Product records, mmap-able, iterable without full load. Each record carries id, barcode, name (i18n), brand, categories, nutrition per-100g, serving definitions (original unit + gram equivalent), ingredients, and a `source_id` reference. License and attribution embedded per record (ODbL for OFF-derived data). The compiler emits a catalog manifest entry per pack including: name, semver version, byte size, SHA-256 checksum, download URL. Initial catalog = 6 starter packs (US, EU-mix DE/FR/ES/IT, UK, JP, BR, global brands), 50–100k items each. Built packs hosted on GitHub Releases or Hugging Face dataset (free tier). Manifest published at `nutaru.app/catalog.json` via GitHub Pages.

## Acceptance criteria

- [ ] CLI invocation: `pack-compiler compile --input off-export.json --region US --out us.pack` produces a valid pack file
- [ ] Pack FlatBuffer schema validates; records stream via length-prefix without full load
- [ ] Each record carries license + attribution text per ODbL requirements
- [ ] Manifest entry includes name, version, byte size, SHA-256, download URL
- [ ] 6 starter packs built and uploaded to hosting
- [ ] Manifest file live at `nutaru.app/catalog.json`, fetchable via plain HTTPS GET
- [ ] Pack compiler ships with one sample test: 100-row OFF slice → expected pack bytes golden
- [ ] `commonTest` equivalent (CLI test): pack file round-trips through parser cleanly

## Blocked by

None — can start immediately. Parallel with #1.
