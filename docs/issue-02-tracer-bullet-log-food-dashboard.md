---
title: "Tracer-bullet: log food → dashboard"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

The smallest possible end-to-end vertical slice through the architecture. App opens directly to a minimal Home screen showing one dashboard tile: today's macro totals (initially zero). A curated hardcoded list of ~5 food items is seeded into the DB (no pack system yet). User taps a "log food" affordance → simple form picks an item from the hardcoded list, enters a quantity, commits → `log_entries` row written → macros tile updates in the same session and after relaunch. No real onboarding, no profile, no targets, no search — just prove that schema, encryption, KMP layers, and UI integrate cleanly. This slice forces every architectural seam to be exercised once.

## Acceptance criteria

- [ ] Home screen renders one tile showing today's kcal + P/C/F (initially zero)
- [ ] Hardcoded list of ~5 food items seeded into DB on first launch (idempotent on relaunch)
- [ ] "Log food" affordance opens form: pick item, enter quantity, commit
- [ ] Commit writes `log_entries` row; macros tile updates same-session
- [ ] Relaunch: tile shows previously logged totals
- [ ] Logging for a different date (manual date picker) writes to the correct day, not today
- [ ] `commonTest`: log_entry insertion + day-total rollup math correct (sums the right rows per day)

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`
