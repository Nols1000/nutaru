---
title: "Food search + barcode logging"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Full set of manual logging input modes via Quick Add bottom sheet. **Text/Manual mode**: typeahead search of `products` via FTS5, results ranked by relevance, tap result to open quantity entry, enter quantity in original unit (cup/slice/g/etc), grams computed from product's serving definition, macros preview live, confirm commits. **Barcode mode**: camera scanner (ML Kit on Android, AVFoundation on iOS) detects barcode, lookup in `products` table, if found routes to same confirm flow, if not found offers custom-food creation (full custom-food flow lands in #13, here just route to a placeholder). Logging supports manual date selection (defaults to today) for backfill. Edit and delete of existing entries via Diary long-press (wired into #5).

## Acceptance criteria

- [ ] Quick Add bottom sheet offers Text / Photo (placeholder for #11) / Barcode / Manual modes
- [ ] Text search: typeahead returns FTS5 matches in < 100ms on a 50k-product pack
- [ ] Quantity entry: original unit stored as entered, grams computed from serving definition, macro preview updates live
- [ ] Barcode scan: camera opens, detects barcode in real-time, looks up, routes to confirm
- [ ] Barcode not found: clear UX ("not in installed packs"), offers custom-food creation entry point
- [ ] Date picker defaults to today, allows past and future dates
- [ ] Confirm commits `log_entries` row, Diary + dashboard update
- [ ] `commonTest`: FTS5 query construction correctness across multi-word, partial, and ranked matches
- [ ] `commonTest`: unit conversion (cup/slice/piece/etc → grams) for sample serving definitions

## Blocked by

- `docs/issue-07-pack-import-runtime.md`
