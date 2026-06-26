---
title: "Pack import runtime"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-26
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

App-side pack import pipeline. Catalog manifest fetched from `nutaru.app/catalog.json`. Browse UI shows available packs with name, description, byte size, license, attribution. Install triggers a background download with progress indication; UI remains usable during download and import. Post-download: SHA-256 verified against manifest entry (mismatch aborts install with clear error), FlatBuffer stream-parsed, bulk INSERT into `products` table, `products_fts` FTS5 index populated, `sources` table populated with attribution. Onboarding Pack step (placeholder in #3) now functional: locale-suggested pack with one-tap install, browse more, or skip. Settings → Packs management: list installed packs, uninstall (removes only matching `pack_id` products), update (re-import new version), import side-loaded pack file via OS file picker.

## Acceptance criteria

- [x] Catalog manifest fetched over HTTPS, displayed in browse UI
- [x] Install runs as background task with progress notification, app remains usable
- [x] SHA-256 mismatch aborts install with clear error message
- [x] Post-install: products from pack searchable in logging flow
- [x] Onboarding Pack step suggests locale pack, one-tap install completes
- [x] Skip path completes onboarding without installing any pack
- [x] Settings: uninstall removes pack's products, reclaims space, pack's source row removed if no other packs reference it
- [x] Settings: side-load pack via file picker works (validates checksum field if present)
- [x] `commonTest`: FlatBuffer stream parse → SQL bulk insert round-trip on a small test pack
- [x] `commonTest`: uninstall removes only matching `pack_id` rows, leaves other packs intact

## Implementation notes

- Schema v6 migration (`5.sqm`): `sources`, `packs`, `products_fts` (FTS5); `products.pack_id` widened INTEGER → TEXT to reference the catalog pack id directly. Standalone FTS5 (not external content) so DELETE-by-rowid is trivial on uninstall.
- `PackCodec` is a pure-KMP twin of the compiler's JVM codec (no `java.io`/`Charsets`); a pure-Kotlin UTF-8 codec (`Utf8`) replaces JVM-only string APIs so the parser runs on iOS/JS too. A precedence-bug regression guard (`Utf8Test`) pins the codepoint assembly.
- `PackImporter` verifies SHA-256 before any DB write (supply-chain integrity) and bulk-inserts in one transaction; re-import drops old products+FTS first (update path).
- `PackManager` orchestrates catalog→download(progress)→verify→import; `PackFetcher` is the network seam (Android `HttpURLConnection` actual, fake in tests).
- Onboarding Pack step + Settings → Packs screen wired; `LocaleProvider` feeds the locale-suggested pack. Side-load via Android SAF (`OpenDocument`).
- Cross-compat test reads the shipped `public/us.pack` + `catalog.json` and pins the KMP codec's SHA-256 against the manifest.

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`
- `docs/issue-06-pack-compiler-catalog-hosting.md`
