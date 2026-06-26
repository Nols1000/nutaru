---
title: "Argon2id parameter tuning"
type: issue
status: ready-for-agent
created: 2026-06-25
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Tune the Argon2id parameters to hit the PRD's ~250 ms derivation target on reference hardware. Issue-01 shipped conservative placeholders in `sharedLogic/src/commonMain/kotlin/com/github/nols1000/nutaru/crypto/Argon2id.kt`:

```kotlin
const val DEFAULT_ITERATIONS = 3
const val DEFAULT_MEMORY_KIB = 65_536 // 64 MiB
const val DEFAULT_PARALLELISM = 1
const val DEFAULT_OUTPUT_LENGTH = 32   // 256-bit SQLCipher key
```

These are untested guesses. The PRD sets the performance budget at ~250 ms on:
- **iPhone 17 Pro** (high-end ceiling)
- **Pixel 5** (mid-tier floor, Snapdragon 765G, 8 GB RAM)

Measure actual derivation time on both, then iterate the `iterations` / `memoryKib` / `parallelism` triple until each device lands in the ~200–300 ms window. Memory ceiling on Pixel 5 class matters — too high a `memoryKib` will get the process killed under memory pressure.

End-to-end behavior: cold-launch key derivation lands in-budget on both reference devices, parameters updated in code, benchmarks captured for future regression checks.

## Acceptance criteria

- [ ] Benchmark harness in repo (one-shot script or `./gradlew` task) that times `Argon2id.deriveKey` over a range of parameter triples and prints a table
- [ ] Measured derivation time on iPhone 17 Pro lands in 200–300 ms with the chosen parameters
- [ ] Measured derivation time on Pixel 5 lands in 200–300 ms with the chosen parameters (a different parameter set per platform is acceptable if needed — split into per-platform defaults)
- [ ] `Argon2id.DEFAULT_*` constants updated from the placeholder values to the measured ones, with the benchmark numbers cited in a comment
- [ ] If per-platform tuning diverges meaningfully, the `Argon2id.deriveKey` seam accepts platform-aware defaults (expect/actual or a `Platform` probe), documented inline
- [ ] Smoke benchmark added to `commonTest` (or `androidHostTest`) that asserts derivation time stays under a generous ceiling (e.g. < 2 s) so parameter regressions surface in CI

## Blocked by

- `docs/issue-01-encrypted-storage-foundation.md`

## Notes

- Don't go above the bottom of the PRD's budget to "be safe" — slower derivation directly worsens cold-start, which is also a PRD NFR (< 2 s cold start).
- The benchmark is informational; do not check in a parameter set you haven't actually measured on the reference hardware. If the reference hardware isn't available, leave the placeholders and call that out in the issue status.
- This issue is purely about parameter tuning; algorithm/correctness is owned by issue-01 (Android) and `issue-18-ios-encrypted-storage-wiring.md` (iOS).
