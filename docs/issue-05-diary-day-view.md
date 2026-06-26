---
title: "Diary day view"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-26
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Diary tab fully functional. Top app bar shows current date; tapping opens a date picker (calendar or wheel) for direct navigation. Horizontal swipe gesture navigates to previous/next day with smooth animation. Entries for the selected day are grouped under meal-type headers (breakfast/lunch/dinner/snack) with per-meal macro subtotals. Each entry row shows food name, quantity with unit, and computed macros. A day-total card shows kcal + P/C/F against target with a progress indicator. Long-press or swipe on an entry reveals edit and delete actions.

## Acceptance criteria

- [x] Top app bar shows current date; tap opens date picker for arbitrary jump
- [x] Swipe left/right navigates days with smooth animation, no debounce issues
- [x] Entries grouped under meal-type headers in canonical order
- [x] Per-meal subtotals shown under each header
- [x] Day-total card shows kcal + P/C/F vs target, progress indicator reflects remaining
- [x] Long-press or swipe on entry reveals edit/delete actions
- [x] Edit entry routes to edit form pre-filled with current values
- [x] Delete entry removes from list (hard delete, with confirm dialog)
- [x] `commonTest`: day grouping returns correct rows per meal per day
- [x] `commonTest`: per-meal and day-total rollup math correct

## Blocked by

- `docs/issue-02-tracer-bullet-log-food-dashboard.md`
