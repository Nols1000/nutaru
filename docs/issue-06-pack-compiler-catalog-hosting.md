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

## Notes

- CI publishing is wired in `.github/workflows/packs-publish.yml` (workflow_dispatch): downloads the OFF JSONL dump, runs `compile` per region, uploads `.pack` files to a GitHub Release (`packs-v1`, stable URLs), and deploys `catalog.json` to GitHub Pages via `actions/deploy-pages`. Prereq: repo Settings -> Pages -> Source = "GitHub Actions" (one-time human step).
- The workflow ships **5** regional packs (US, EU-MIX, UK, JP, BR). The 6th pack (`GLOBAL`) is split into `docs/issue-21-global-pack-filter.md` because `OffReader.REGION_TAGS["GLOBAL"] == null` keeps every product on real data and would OOM; the demo path hid this. The "6 starter packs built and uploaded" acceptance criterion is blocked on issue-21.
- Custom domain `nutaru.app` (DNS + Pages setting) remains a human TODO; until wired the manifest is at `<owner>.github.io/<repo>/catalog.json`. Pack download URLs point at the Release, so they are independent of the Pages domain.
- **Fixed 2026-06-27**: "Download and verify OFF dump" step failed with exit 1 and no message. Root cause: under `set -euo pipefail`, `grep " ${remote_name}\$" gz-sha256sum` exits 1 when the products dump isn't listed (OFF's `gz-sha256sum` currently only lists `openfoodfacts-mongodbdump.gz`), aborting the script before the `if [ -z "$expected" ]` warning branch. Appended `|| true` to the `grep | awk` pipeline so the documented best-effort warn-and-skip integrity check actually runs.
