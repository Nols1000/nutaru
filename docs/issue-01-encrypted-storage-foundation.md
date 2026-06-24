---
title: "Encrypted storage foundation"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

App launches cold and silently establishes an encrypted database as the foundation for every subsequent feature. On first launch: generate a 12-word recovery mnemonic (BIP-39 wordlist, sufficient entropy), derive the SQLCipher key via Argon2id, open the encrypted SQLDelight database. On subsequent launches: derive same key from same mnemonic, reopen existing DB. SQLDelight schema migrations framework wired end-to-end with one trivial migration as proof. Profile table created (empty). End-to-end behavior: tap app icon → app reaches a state where it can write a smoke row to an encrypted DB and read it back after relaunch.

## Acceptance criteria

- [ ] First launch: mnemonic generated, Argon2id key derived (target ~250ms on reference hardware), encrypted DB created on disk
- [ ] Subsequent launches: same mnemonic → same key → existing DB opens without re-onboarding
- [ ] DB file at rest is encrypted (verified via filesystem inspection — no plaintext strings recoverable)
- [ ] SQLDelight schema migrations framework wired; one trivial migration (e.g., add a column) tested round-trip
- [ ] `profile` table created (empty), singleton pattern enforced at access layer
- [ ] `commonTest` round-trip: write smoke row, close DB, reopen with same key, read smoke row back

## Blocked by

None — can start immediately.
