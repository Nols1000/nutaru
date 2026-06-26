---
title: "Tracer-bullet: log food → dashboard"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

The smallest possible end-to-end vertical slice through the architecture. App opens directly to a minimal Home screen showing one dashboard tile: today's macro totals (initially zero). A curated hardcoded list of ~5 food items is seeded into the DB (no pack system yet). User taps a "log food" affordance → simple form picks an item from the hardcoded list, enters a quantity, commits → `log_entries` row written → macros tile updates in the same session and after relaunch. No real onboarding, no profile, no targets, no search — just prove that schema, encryption, KMP layers, and UI integrate cleanly. This slice forces every architectural seam to be exercised once.

## Acceptance criteria

- [x] Home screen renders one tile showing today's kcal + P/C/F (initially zero)
- [x] Hardcoded list of ~5 food items seeded into DB on first launch (idempotent on relaunch)
- [x] "Log food" affordance opens form: pick item, enter quantity, commit
- [x] Commit writes `log_entries` row; macros tile updates same-session
- [x] Relaunch: tile shows previously logged totals
- [x] Logging for a different date (manual date picker) writes to the correct day, not today
- [x] `commonTest`: log_entry insertion + day-total rollup math correct (sums the right rows per day)

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`

## Follow-ups

Out of scope for the tracer bullet, tracked for later:

- `docs/issue-21-mnemonic-store-hardening.md` — mnemonic currently persisted in plaintext Android `SharedPreferences` (sufficient for the tracer-bullet's relaunch criterion, not a V1 ship bar). Wrap with a hardware-backed Keystore key before beta; mirror to iOS Keychain alongside issue-18.
- `docs/issue-22-ios-tracer-bullet-wiring.md` — iOS `MnemonicStore` is a no-op stub (`load()` returns null every launch), so the iOS app cannot complete the relaunch criterion yet. Blocked on issue-18 iOS encrypted-storage actuals.
- Date input is a free-form `yyyy-MM-dd` text field, not a Material3 `DatePickerDialog`. Swap once the V1 plan/meal polish lands (`issue-*-diary-day-view`).
- `products` table ships only the columns the tracer bullet needs (id, name, per-100g macros). i18n name, `categories`, ingredients, `source_id` foreign keys, `products_fts` FTS5 index, and `packs` table land with the pack-import issue.
