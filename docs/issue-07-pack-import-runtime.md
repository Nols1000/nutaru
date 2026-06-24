---
title: "Pack import runtime"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

App-side pack import pipeline. Catalog manifest fetched from `nutaru.app/catalog.json`. Browse UI shows available packs with name, description, byte size, license, attribution. Install triggers a background download with progress indication; UI remains usable during download and import. Post-download: SHA-256 verified against manifest entry (mismatch aborts install with clear error), FlatBuffer stream-parsed, bulk INSERT into `products` table, `products_fts` FTS5 index populated, `sources` table populated with attribution. Onboarding Pack step (placeholder in #3) now functional: locale-suggested pack with one-tap install, browse more, or skip. Settings → Packs management: list installed packs, uninstall (removes only matching `pack_id` products), update (re-import new version), import side-loaded pack file via OS file picker.

## Acceptance criteria

- [ ] Catalog manifest fetched over HTTPS, displayed in browse UI
- [ ] Install runs as background task with progress notification, app remains usable
- [ ] SHA-256 mismatch aborts install with clear error message
- [ ] Post-install: products from pack searchable in logging flow
- [ ] Onboarding Pack step suggests locale pack, one-tap install completes
- [ ] Skip path completes onboarding without installing any pack
- [ ] Settings: uninstall removes pack's products, reclaims space, pack's source row removed if no other packs reference it
- [ ] Settings: side-load pack via file picker works (validates checksum field if present)
- [ ] `commonTest`: FlatBuffer stream parse → SQL bulk insert round-trip on a small test pack
- [ ] `commonTest`: uninstall removes only matching `pack_id` rows, leaves other packs intact

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`
- `docs/issue-06-pack-compiler-catalog-hosting.md`
