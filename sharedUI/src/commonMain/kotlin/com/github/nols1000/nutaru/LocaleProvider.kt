package com.github.nols1000.nutaru

/**
 * Platform-supplied device locale, as an ISO country / region tag the catalog
 * packs carry (e.g. "US", "JP", "GB"). Used by the onboarding Pack step to
 * suggest the locale-matching starter pack (issue-07 criterion 5).
 *
 * Mirrors [DayBoundsProvider]: common UI stays free of `java.util.Locale` /
 * platform APIs, the Android entry point wires the concrete implementation.
 * Returns "" when no region can be determined (the onboarding step then shows
 * the catalog without a suggestion, which is still a valid path).
 */
interface LocaleProvider {
    fun regionTag(): String
}
