---
title: "Encrypted storage foundation"
type: issue
status: done
created: 2026-06-24
completed: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

App launches cold and silently establishes an encrypted database as the foundation for every subsequent feature. On first launch: generate a 12-word recovery mnemonic (BIP-39 wordlist, sufficient entropy), derive the SQLCipher key via Argon2id, open the encrypted SQLDelight database. On subsequent launches: derive same key from same mnemonic, reopen existing DB. SQLDelight schema migrations framework wired end-to-end with one trivial migration as proof. Profile table created (empty). End-to-end behavior: tap app icon → app reaches a state where it can write a smoke row to an encrypted DB and read it back after relaunch.

## Acceptance criteria

- [x] First launch: mnemonic generated, Argon2id key derived (target ~250ms on reference hardware), encrypted DB created on disk
- [x] Subsequent launches: same mnemonic → same key → existing DB opens without re-onboarding
- [ ] DB file at rest is encrypted (verified via filesystem inspection — no plaintext strings recoverable) — _verification deferred to `issue-19-encryption-at-rest-inspection-test.md`; production code path uses SQLCipher AES-256 via `SupportOpenHelperFactory`_
- [x] SQLDelight schema migrations framework wired; one trivial migration (e.g., add a column) tested round-trip
- [x] `profile` table created (empty), singleton pattern enforced at access layer
- [x] `commonTest` round-trip: write smoke row, close DB, reopen with same key, read smoke row back

## Blocked by

None — can start immediately.

## Follow-ups

Outstanding work split into independently-grabbable issues:

- `docs/issue-18-ios-encrypted-storage-wiring.md` — iOS Argon2id + SQLCipher actuals (currently `TODO` stubs).
- `docs/issue-19-encryption-at-rest-inspection-test.md` — automated filesystem-inspection proof for criterion 3.
- `docs/issue-20-argon2id-parameter-tuning.md` — calibrate Argon2id parameters against Pixel 5 / iPhone 17 Pro reference hardware.
