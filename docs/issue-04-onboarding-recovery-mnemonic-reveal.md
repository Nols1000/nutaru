---
title: "Onboarding: Recovery mnemonic reveal"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-26
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Recovery mnemonic reveal step in onboarding, plus settings re-view. Mnemonic (silently generated in #1) is displayed prominently on a dedicated reveal screen. Copy-to-clipboard is disabled to force the user to write the words down manually. Confirm-saved gate: app prompts the user to tap one specific word (randomly chosen) from their mnemonic to proceed — proves they have actually seen and saved it. Settings entry to re-view the mnemonic, gated behind biometric or device auth. If user kills the app before completing the reveal step, relaunch forces the reveal screen before reaching Home. Once acknowledged, relaunch goes straight to Home.

## Acceptance criteria

- [x] Reveal screen shows all 12 words in a stable order, copy-to-clipboard disabled
- [x] Confirm-saved gate prompts one randomly-chosen word, validates correct entry, blocks progression on wrong entry
- [x] Settings has "View recovery mnemonic" entry, requires biometric or device auth (Face ID/Touch ID/BiometricPrompt)
- [x] App killed before reveal complete → relaunch routes back to reveal step, not Home
- [x] Once reveal acknowledged → relaunch goes straight to Home
- [x] Acknowledgment flag persisted in `settings` table
- [x] `commonTest`: relaunch state machine transitions correctly per acknowledgment flag

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`

## Implementation notes

### Storage (`sharedLogic`)

- New `settings` singleton table (`sharedLogic/.../sqldelight/.../settings.sq`) with `mnemonic_acknowledged INTEGER NOT NULL DEFAULT 0`; created via migration `4.sqm` (v4 → v5). SQLDelight database version bumped to 5.
- `NutaruRepository.isMnemonicAcknowledged()` / `acknowledgeMnemonic()` read/persist the flag.
- `commonTest`: `MnemonicAckPersistenceTest` (3 tests) — flag defaults false on a fresh DB, survives close/reopen (relaunch), and `acknowledgeMnemonic()` is idempotent.

### Relaunch state machine (`sharedUI`)

- `RelaunchRoute.kt` exposes a pure `relaunchRoute(hasProfile, mnemonicAcknowledged): RelaunchRoute` function (no SQL, no platform) returning `ONBOARDING | REVEAL | HOME`:
  - no profile → `ONBOARDING` (full flow)
  - profile saved, mnem not ack → `REVEAL` (app was killed before completing the reveal step)
  - profile saved, mnem acked → `HOME`
- `commonTest`: `RelaunchRouteTest` (4 tests) pins every transition including the defensive "ack without profile → onboarding" case.

### Reveal screen + confirm gate (`sharedUI`)

- `RecoveryReveal.kt`:
  - `RecoveryRevealScreen` renders the 12 words in stable index order inside a `Card`. No `SelectionContainer` (and no copy affordance) — copy-to-clipboard is disabled by design (criterion 1).
  - Confirm-saved gate: a `WordIndexPicker` (default `DefaultWordIndexPicker` draws uniformly from 1..12) selects the challenge index once per composition and holds it across retries. The user types the word at that 1-based position; `confirmSavedMatch(challengeIndex, mnemonic, entry)` is a pure function that accepts the exact (case-insensitive, whitespace-trimmed) word and rejects everything else. Wrong entry sets an error and blocks progression; the user can retry (criterion 2).
  - On acknowledge, the caller persists the flag via `repository.acknowledgeMnemonic()` then invokes `onComplete`.
- `commonTest`: `ConfirmSavedMatchTest` (7 tests) — exact/case-insensitive/whitespace accept; wrong word, empty entry, out-of-range index, partial word reject.
- `OnboardingFlow.kt`: new `RECOVERY` step after `AGENT`; `OnboardingFlow` now takes `mnemonic: List<String>` and an optional `wordIndexPicker` (defaulted). Flow: WELCOME → PROFILE → PLAN → PACK (skip) → AGENT (skip) → RECOVERY (persist ack) → Home.

### App routing + Settings re-view (`sharedUI`)

- `App.kt` reads `relaunchRoute(repository.hasProfile(), repository.isMnemonicAcknowledged())` off-thread on launch and routes accordingly. The `REVEAL` route renders `RecoveryRevealScreen` directly (forced reveal on relaunch — criterion 4); `HOME` goes straight to Home (criterion 5).
- `BiometricGate.kt` interface (`authenticate(onSuccess, onFailure)`) + `AlwaysPassBiometricGate` test/preview fake. Keeps `sharedUI:commonTest` free of platform SDK calls.
- Minimal `SettingsScreen` (full Settings tab lands in issue-14): a "View recovery mnemonic" `OutlinedButton` runs `BiometricGate.authenticate`; on success the 12 words are shown in a read-only card (copy disabled, same as onboarding). On failure/cancel the words stay hidden and an error is shown (criterion 3).

### Android wiring (`androidApp`)

- `MainActivity` extends `FragmentActivity` (required by `BiometricPrompt`), passes the loaded/generated `mnemonic` and an `AndroidBiometricGate` into `App`.
- `AndroidBiometricGate` uses AndroidX `BiometricPrompt` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` authenticators (falls back to PIN/pattern/password when no biometric hardware is enrolled) and fails closed (no reveal) when no auth is available.
- `androidx.biometric:biometric:1.1.0` added to `gradle/libs.versions.toml` + `androidApp/build.gradle.kts`.

### Verification

- `./gradlew :sharedLogic:testAndroidHostTest :sharedUI:testAndroidHostTest` — all green.
  - `MnemonicAckPersistenceTest`: 3/3
  - `RelaunchRouteTest`: 4/4
  - `ConfirmSavedMatchTest`: 7/7
- `./gradlew :androidApp:assembleDebug` — green (biometric wiring + `FragmentActivity` change compiles).
- iOS/JS test targets compile but are not executed in this environment (no Xcode/JS runtime); the tested logic lives in `commonMain` so it applies to all platforms. iOS biometric wiring (LocalAuthentication) is tracked by issue-18.
