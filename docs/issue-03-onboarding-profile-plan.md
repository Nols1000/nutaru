---
title: "Onboarding: Profile + Plan"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Real onboarding welcome flow with profile collection and target calculation. Welcome screen with privacy pitch + "Begin" CTA. Profile form collects age, sex, height, current weight, goal (lose/maintain/gain), activity level — all stored locally with explicit "stays on device" copy. Plan screen: shows calculated daily kcal via Mifflin-St Jeor + activity factor + goal delta, with P/C/F split and reasoning breakdown visible. User can override kcal and macros manually; override persists to `targets` table with `effective_from`. Pack and Agent onboarding steps render as visible "skip — configure later" placeholders (wired in their respective slices). On completion: app reaches Home with profile + targets set; macros tile now shows progress against target, not just raw totals.

## Acceptance criteria

- [ ] Welcome screen with privacy pitch and "Begin" CTA
- [ ] Profile form collects all six fields, validates input (ranges, required)
- [ ] Plan screen shows kcal target with reasoning breakdown (BMR + activity + goal delta)
- [ ] Manual override of kcal + P/C/F persists to `targets` table with `effective_from = today`
- [ ] Skip Pack and Skip Agent paths complete onboarding with profile + targets set
- [ ] Home macros tile shows progress against target (kcal remaining, macro compliance)
- [ ] `commonTest`: Mifflin-St Jeor BMR calc correctness across sex/goal/activity combinations
- [ ] `commonTest`: target persistence + retrieval by `effective_from`

## Blocked by

- `docs/issue-02-tracer-bullet-log-food-dashboard.md`
