---
title: "iOS encrypted storage wiring"
type: issue
status: ready-for-agent
created: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Bring iOS to parity with Android for the encrypted storage foundation. Issue-01 shipped the commonMain `EncryptedDatabase.open(path, mnemonic)` seam plus a working Android actual (SQLCipher via `SupportOpenHelperFactory` + Argon2id via BouncyCastle). The iOS actuals are `TODO` stubs that throw today:

- `sharedLogic/src/iosMain/kotlin/com/github/nols1000/nutaru/crypto/Argon2id.kt` — needs a native Argon2id implementation. CommonCrypto / CryptoKit do not expose Argon2; ship the reference C implementation via a CocoaPod or bundle a Swift port (e.g. `swift-crypto` does not help, but libraries like `argon2-swift` or a hand-vendored wrapper around the upstream `argon2` C reference are viable).
- `sharedLogic/src/iosMain/kotlin/com/github/nols1000/nutaru/db/EncryptedDatabase.kt` — currently opens an unencrypted `NativeSqliteDriver`. Swap to a SQLCipher-linked SQLite (`net.zetetic:sqlcipher-ios` pod or equivalent) and pass the derived key.

End-to-end behavior on iOS: same mnemonic → same key → existing encrypted DB opens, smoke row round-trips across relaunch.

## Acceptance criteria

- [ ] iOS `argon2idHash` actual produces a 32-byte key from `(password, salt, iterations, memoryKib, parallelism)` matching the JVM output for the same inputs (cross-platform test vector)
- [ ] iOS `createSqlCipherDriver` opens a SQLCipher-encrypted DB at the supplied path with the supplied key
- [ ] Wrong key fails to open an existing iOS DB (SQLCipher rejects)
- [ ] `iosTest` round-trip: write smoke row, close, reopen with same key, read back — passes on the iOS simulator
- [ ] No regressions to `:sharedLogic:testAndroidHostTest`

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`

## Notes

- The PRD targets iOS 17+; SQLCipher 4.x supports this.
- The Argon2id parameters chosen in issue-01 (3 iterations, 64 MiB, parallelism 1) are a placeholder until `issue-20-argon2id-parameter-tuning.md` lands. Don't tune here — just wire the algorithm.
- JS is intentionally out of scope (PRD: webApp not shipped for V1); leave the JS stubs throwing.
