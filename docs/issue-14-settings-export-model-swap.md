---
title: "Settings + export + model swap"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Full Settings tab. Theme picker (light / dark / system) applies live across the app. Units picker (metric / imperial) converts display across logging, diary, dashboard, and weight tracking. Mnemonic re-view (entry point wired in #4) shown here as well. Model swap: switch between Gemma 4 E2B / E4B / OS model — downloads new model, verifies checksum, swaps active model in `agent_model_state`, deletes old model file to reclaim space. Agent enable/disable toggle: when disabled, hides bottom text field, agent insight tile, onboarding Agent step (treats as skipped on next launch). Encrypted backup file export: full DB dump to user-chosen location (Files app / share sheet). Hard-delete-all action: multi-confirm flow (type "DELETE" to confirm), wipes all tables, checkpoints WAL, returns app to first-launch onboarding state.

## Acceptance criteria

- [ ] Theme picker applies live to every screen, persists across launches
- [ ] Units picker converts display across logging, diary, dashboard tiles, weight tracking
- [ ] Mnemonic re-view requires biometric / device auth, shows 12 words (no copy)
- [ ] Model swap: downloads new model, SHA-256 verifies, swaps in `agent_model_state`, deletes old file
- [ ] Agent disable hides bottom text field on data tabs, hides agent insight tile, marks Agent step as skipped
- [ ] Agent re-enable restores all UI without requiring onboarding rerun
- [ ] Backup export writes encrypted file to user-chosen location, file is restorable on a fresh install (manual test)
- [ ] Hard-delete-all: requires typing "DELETE" to confirm, wipes every table, WAL checkpoints, app returns to first-launch onboarding
- [ ] `commonTest`: unit conversion correctness across cup/slice/piece/oz/lb/kg/cm/m
- [ ] `commonTest`: hard-delete wipes all rows in all tables (including `agent_*`, `log_entries`, `weight_entries`, `products`, `targets`, `profile`, `settings`)

## Blocked by

- `docs/issue-10-assistant-foundation-nl-text-logging.md`
