package com.github.nols1000.nutaru

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the relaunch routing contract (issue-04, criterion 7). The App router
 * reads `hasProfile` and `isMnemonicAcknowledged` off-thread on launch and maps
 * them through [relaunchRoute]; these are the only legal transitions.
 */
class RelaunchRouteTest {

    @Test
    fun no_profile_routes_to_full_onboarding() {
        assertEquals(
            RelaunchRoute.ONBOARDING,
            relaunchRoute(hasProfile = false, mnemonicAcknowledged = false),
        )
    }

    @Test
    fun profile_without_acknowledgment_routes_to_reveal() {
        assertEquals(
            RelaunchRoute.REVEAL,
            relaunchRoute(hasProfile = true, mnemonicAcknowledged = false),
        )
    }

    @Test
    fun profile_with_acknowledgment_routes_to_home() {
        assertEquals(
            RelaunchRoute.HOME,
            relaunchRoute(hasProfile = true, mnemonicAcknowledged = true),
        )
    }

    @Test
    fun acknowledged_without_profile_still_onboarding() {
        // Defensive: ack only happens inside onboarding, but if the flag is
        // somehow set with no profile, route to onboarding rather than skipping
        // profile setup.
        assertEquals(
            RelaunchRoute.ONBOARDING,
            relaunchRoute(hasProfile = false, mnemonicAcknowledged = true),
        )
    }
}
