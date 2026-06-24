---
title: "Assistant foundation (NL text logging)"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Assistant infrastructure end-to-end through a single tool (`log_food` via natural-language parse). Onboarding Agent step (placeholder in #3) now functional: explains on-device AI, runs capability probe for OS model, presents model options (Gemma 4 E2B, Gemma 4 E4B if device RAM permits, OS model if probe passes, skip). Gemma 4 runtime integrated via KMP expect/actual — Google AI Edge on both platforms, Apple Foundation Models as OS-model path on iOS 26+. Model download with progress, SHA-256 checksum verified against Google's published hash, persisted in app storage. Function calling wired: `log_food` tool defined in `sharedLogic`, schema exposed to model, response parser maps tool-call to structured proposal. Bottom text field on data tabs (Home / Diary / Workout) routes input to the assistant; hidden on Settings. NL input ("2 eggs and toast") → assistant proposes structured entries → suggest-confirm bottom sheet → user confirms → `log_entries` written.

## Acceptance criteria

- [ ] Onboarding Agent step shows model options appropriate to device capability (RAM floor, OS version, hardware)
- [ ] Capability probe runs for OS model, option only appears if probe passes threshold
- [ ] Model download has progress UI, SHA-256 verifies against published hash, file persists
- [ ] Skip path completes onboarding, hides all assistant UI across the app
- [ ] Bottom text field visible on Home / Diary / Workout, hidden on Settings
- [ ] NL input "2 eggs and toast" → assistant proposes 2 entries with correct macros from product DB
- [ ] Suggest-confirm bottom sheet shows proposed entries, user can confirm or reject each
- [ ] Confirmed entries write to `log_entries`, dashboard and diary update same-session
- [ ] Cold-start first-token latency under perf budget on iPhone 17 Pro (< 3s) and Pixel 5 (< 5s)
- [ ] `commonTest`: tool dispatcher routes correct tool name, rejects undefined tool calls
- [ ] `commonTest`: tool arg schema validation passes valid args, rejects invalid (missing required, wrong type)
- [ ] `commonTest`: capability probe decision logic across device profiles

## Blocked by

- `docs/issue-02-tracer-bullet-log-food-dashboard.md`
