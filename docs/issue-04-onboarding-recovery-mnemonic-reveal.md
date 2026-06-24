---
title: "Onboarding: Recovery mnemonic reveal"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Recovery mnemonic reveal step in onboarding, plus settings re-view. Mnemonic (silently generated in #1) is displayed prominently on a dedicated reveal screen. Copy-to-clipboard is disabled to force the user to write the words down manually. Confirm-saved gate: app prompts the user to tap one specific word (randomly chosen) from their mnemonic to proceed — proves they have actually seen and saved it. Settings entry to re-view the mnemonic, gated behind biometric or device auth. If user kills the app before completing the reveal step, relaunch forces the reveal screen before reaching Home. Once acknowledged, relaunch goes straight to Home.

## Acceptance criteria

- [ ] Reveal screen shows all 12 words in a stable order, copy-to-clipboard disabled
- [ ] Confirm-saved gate prompts one randomly-chosen word, validates correct entry, blocks progression on wrong entry
- [ ] Settings has "View recovery mnemonic" entry, requires biometric or device auth (Face ID/Touch ID/BiometricPrompt)
- [ ] App killed before reveal complete → relaunch routes back to reveal step, not Home
- [ ] Once reveal acknowledged → relaunch goes straight to Home
- [ ] Acknowledgment flag persisted in `settings` table
- [ ] `commonTest`: relaunch state machine transitions correctly per acknowledgment flag

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`
