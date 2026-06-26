package com.github.nols1000.nutaru

/** Where the app lands on (re)launch, derived from persisted state. */
enum class RelaunchRoute { ONBOARDING, REVEAL, HOME }

/**
 * Pure relaunch routing decision — no SQL, no platform. Pinned in
 * `commonTest` (issue-04, criterion 7) so the acknowledgment-flag transition
 * contract can't drift.
 *
 *  - no profile yet              → full onboarding (Welcome…Reveal)
 *  - profile saved, mnem not ack → forced reveal (app was killed before
 *                                  completing the reveal step)
 *  - profile saved, mnem acked   → Home
 *
 * `hasProfile` is the primary gate: an acknowledged flag without a profile
 * (impossible in practice — ack only happens inside onboarding) still routes
 * to onboarding rather than skipping profile setup.
 */
fun relaunchRoute(hasProfile: Boolean, mnemonicAcknowledged: Boolean): RelaunchRoute = when {
    !hasProfile -> RelaunchRoute.ONBOARDING
    !mnemonicAcknowledged -> RelaunchRoute.REVEAL
    else -> RelaunchRoute.HOME
}
