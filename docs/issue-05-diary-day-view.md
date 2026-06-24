---
title: "Diary day view"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Diary tab fully functional. Top app bar shows current date; tapping opens a date picker (calendar or wheel) for direct navigation. Horizontal swipe gesture navigates to previous/next day with smooth animation. Entries for the selected day are grouped under meal-type headers (breakfast/lunch/dinner/snack) with per-meal macro subtotals. Each entry row shows food name, quantity with unit, and computed macros. A day-total card shows kcal + P/C/F against target with a progress indicator. Long-press or swipe on an entry reveals edit and delete actions.

## Acceptance criteria

- [ ] Top app bar shows current date; tap opens date picker for arbitrary jump
- [ ] Swipe left/right navigates days with smooth animation, no debounce issues
- [ ] Entries grouped under meal-type headers in canonical order
- [ ] Per-meal subtotals shown under each header
- [ ] Day-total card shows kcal + P/C/F vs target, progress indicator reflects remaining
- [ ] Long-press or swipe on entry reveals edit/delete actions
- [ ] Edit entry routes to edit form pre-filled with current values
- [ ] Delete entry removes from list (hard delete, with confirm dialog)
- [ ] `commonTest`: day grouping returns correct rows per meal per day
- [ ] `commonTest`: per-meal and day-total rollup math correct

## Blocked by

- `docs/issue-02-tracer-bullet-log-food-dashboard.md`
