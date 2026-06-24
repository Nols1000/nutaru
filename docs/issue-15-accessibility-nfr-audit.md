---
title: "Accessibility + NFR audit"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Pre-beta quality gate. Run Accessibility Inspector (iOS) and Accessibility Scanner (Android) on every screen built in #1–#14. Fix all P0 and P1 findings, log P2 findings as V1.1 backlog. Verify WCAG 2.1 AA across all dimensions: every interactive element labeled for VoiceOver/TalkBack, Dynamic Type / Font Scale to system maximum doesn't clip text or break layouts, contrast ratios ≥ 4.5:1 verified in light, dark, and system themes, status indicators (over/under target) use icon + text + color (never color alone), touch targets ≥ 44pt iOS / ≥48dp Android, Reduce Motion respected across all animations, Voice Control fully operable. Verify all performance budgets from the PRD on iPhone 17 Pro (ceiling) and Pixel 5 (floor) across cold start, food search, agent first-token warm/cold, streaming rate, plate detect, and tap-to-commit.

## Acceptance criteria

- [ ] Every screen passes Accessibility Inspector / Scanner with no P0 or P1 findings
- [ ] VoiceOver (iOS) navigates every flow end-to-end: onboarding, logging, diary, dashboard, assistant chat, settings
- [ ] TalkBack (Android) navigates the same flows end-to-end
- [ ] Dynamic Type / Font Scale at system maximum: no clipped text, no broken layouts on any screen
- [ ] Contrast verified ≥ 4.5:1 in light, dark, and system themes across all surfaces
- [ ] Status indicators (over/under target, completed/pending) use icon + text + color
- [ ] Touch targets meet platform minimums (44pt iOS / 48dp Android) on every interactive element
- [ ] Reduce Motion / Remove Animations respected across all screen transitions and micro-animations
- [ ] Voice Control fully operable on both platforms
- [ ] Cold start < 2s verified on both reference devices via instrumented profiling
- [ ] Food search latency < 100ms verified on a 50k-product pack
- [ ] Agent first-token latency meets budgets: iPhone 17 Pro < 1s warm / < 3s cold; Pixel 5 < 2s warm / < 5s cold
- [ ] Agent streaming ≥ 10 tok/s on iPhone 17 Pro, ≥ 5 tok/s on Pixel 5
- [ ] Plate detect < 5s on iPhone 17 Pro, < 8s on Pixel 5
- [ ] Log entry tap-to-commit < 500ms on both devices

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md` through `docs/issue-14-settings-export-model-swap.md` (all feature slices complete)
