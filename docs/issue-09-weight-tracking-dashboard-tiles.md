---
title: "Weight tracking + reorderable dashboard tiles"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Weight tracking plus the full dashboard tile system. Quick Add offers "Log weight" option: numeric entry + date, writes `weight_entries`. Home dashboard extended with reorderable tile grid: today's macros (existing, polished), weight trend sparkline (rolling 30-day with 7-day moving average smoothing for noise reduction), recent logs (last 5 entries with tap-to-re-log shortcut), weekly goal progress (rolling 7-day adherence %, kcal + macros). Each tile has a drag handle for reordering; order persists in `settings`. Agent insight tile remains a placeholder, filled by #12.

## Acceptance criteria

- [ ] Quick Add offers "Log weight" with numeric entry and date picker
- [ ] Weight entry writes `weight_entries`, weight trend tile updates same-session
- [ ] Sparkline shows 30-day trend with 7-day moving average smoothing, handles gaps gracefully
- [ ] Recent logs tile shows last 5 entries, tap to re-log the same food with same quantity
- [ ] Weekly goal tile shows 7-day adherence % for kcal and each macro
- [ ] Drag handle on each tile reorders, persists across launches
- [ ] Tiles resize correctly across phone screen sizes (small + large)
- [ ] `commonTest`: sparkline smoothing math correct (7-day MA, edge cases for < 7 days)
- [ ] `commonTest`: weekly adherence calc correct (rolling 7-day, handles future-dated entries)
- [ ] `commonTest`: tile order persistence round-trip through `settings`

## Blocked by

- `docs/issue-05-diary-day-view.md`
