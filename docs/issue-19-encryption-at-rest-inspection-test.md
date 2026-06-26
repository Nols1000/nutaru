---
title: "Encryption-at-rest inspection test"
type: issue
status: ready-for-agent
created: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Automated proof that the on-disk database file is actually encrypted — closes the gap left in issue-01 acceptance criterion 3. Issue-01's production code path uses SQLCipher AES-256 via `SupportOpenHelperFactory`, but the `commonTest` round-trip runs through `app.cash.sqldelight:sqlite-driver` (JDBC, no SQLCipher) because the JVM host can't load the SQLCipher native libraries. The result: encryption is wired but not asserted by an automated check.

Two complementary tests wanted:

1. **Robolectric host test** in `:sharedLogic:androidHostTest` — try the real `EncryptedDatabase.open(path, mnemonic)` path under Robolectric. If Robolectric loads the SQLCipher `.so` from `net.zetetic:sqlcipher-android`, write a profile row with a recognizable plaintext string, close the DB, read the on-disk file bytes, and assert the string is not recoverable. If Robolectric can't load SQLCipher natively, `@Ignore` the test with a `TODO` and fall back to option 2.
2. **Instrumentation test** in `:androidApp:androidTest` (or `:sharedLogic` instrumented source set) that runs the same proof on a real device or emulator — this is the authoritative check.

End-to-end behavior: a green CI step proves no plaintext food log / profile data is recoverable from the DB file.

## Acceptance criteria

- [ ] Robolectric test exists and either (a) proves no plaintext on disk via `EncryptedDatabase.open`, or (b) is `@Ignore`'d with a documented reason if SQLCipher can't load under Robolectric
- [ ] Instrumented test on Android emulator writes a profile row with a known plaintext marker, closes the DB, reads the raw DB file bytes, and asserts the marker is absent
- [ ] Wrong-key test: reopening the encrypted DB with a different mnemonic fails
- [ ] CI gate runs whichever of the above is automatable in the project's GitHub Actions setup

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`

## Notes

- The marker string should be something that would definitely appear in plaintext if the DB were unencrypted — e.g. a unique `display_name` value written via `profileQueries.upsertProfile`.
- This issue is Android-only; iOS gets its inspection test when `issue-18-ios-encrypted-storage-wiring.md` lands.
