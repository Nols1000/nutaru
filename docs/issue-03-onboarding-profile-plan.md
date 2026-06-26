---
title: "Onboarding: Profile + Plan"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Real onboarding welcome flow with profile collection and target calculation. Welcome screen with privacy pitch + "Begin" CTA. Profile form collects age, sex, height, current weight, goal (lose/maintain/gain), activity level — all stored locally with explicit "stays on device" copy. Plan screen: shows calculated daily kcal via Mifflin-St Jeor + activity factor + goal delta, with P/C/F split and reasoning breakdown visible. User can override kcal and macros manually; override persists to `targets` table with `effective_from`. Pack and Agent onboarding steps render as visible "skip — configure later" placeholders (wired in their respective slices). On completion: app reaches Home with profile + targets set; macros tile now shows progress against target, not just raw totals.

## Acceptance criteria

- [x] Welcome screen with privacy pitch and "Begin" CTA
- [x] Profile form collects all six fields, validates input (ranges, required)
- [x] Plan screen shows kcal target with reasoning breakdown (BMR + activity + goal delta)
- [x] Manual override of kcal + P/C/F persists to `targets` table with `effective_from = today`
- [x] Skip Pack and Skip Agent paths complete onboarding with profile + targets set
- [x] Home macros tile shows progress against target (kcal remaining, macro compliance)
- [x] `commonTest`: Mifflin-St Jeor BMR calc correctness across sex/goal/activity combinations
- [x] `commonTest`: target persistence + retrieval by `effective_from`

## Blocked by

- `docs/issue-02-tracer-bullet-log-food-dashboard.md`

## Follow-ups

Out of scope for this slice, tracked for later:

- **Adaptive / algorithmic targets** (`docs/issue-*-adaptive-targets`) — V1 ships static user-set targets with manual override. The `targets.source = "algorithm"` row is written at onboarding but never recomputed; MacroFactor-style adaptive adjustment lands in V1.1 with a transparency UI. The 1200 kcal safety floor in `TargetCalc.calculate` is a hard constant that should scale with lean body mass once the adaptive algorithm exists (marked `TODO` in `TargetCalc.kt`).
- **Pack onboarding step** (`docs/issue-*-pack-import`) — the Pack screen is a visible "skip — configure later" placeholder. Locale-suggested pack + catalog browse + one-tap import replaces it when the pack-import slice lands.
- **Agent onboarding step** (`docs/issue-*-agent-onboarding`) — the Agent screen is a visible "skip — configure later" placeholder. Model choice (Gemma 4 E2B / E4B / OS model / skip) + download replaces it when the agent-onboarding slice lands.
- **Mnemonic reveal + confirm-saved gate** (`docs/issue-*-mnemonic-reveal`) — PRD onboarding step 5. Generated silently at first launch already (issue-01); the reveal UI + tap-a-specific-word confirmation gate ships with the recovery-hardening slice. Currently onboarding completes straight from "skip Agent" into Home.
- **Weight trend UI** (`docs/issue-*-weight-tracking`) — `weight_entries` table is in the schema as of this slice (onboarding writes the first row; `TargetCalc` reads the latest as the BMR weight input), but the weight sparkline tile and assistant-suggested logging cadence ship with the weight-tracking slice.
- **Profile/Plan UI polish** — Compose form uses basic `OutlinedTextField` + `RadioButton` groups and integer-rounded override fields; Material3 `DatePicker`-style selectors and fractional-gram input land with the V1 diary/plan polish slice. Validation ranges (age 13–100, height 100–250 cm, weight 30–300 kg) are reasonable defaults, not clinically reviewed.
