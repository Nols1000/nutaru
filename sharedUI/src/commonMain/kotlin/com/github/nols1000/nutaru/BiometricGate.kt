package com.github.nols1000.nutaru

/**
 * Platform-supplied biometric / device-auth gate (Face ID / Touch ID /
 * BiometricPrompt). Implemented in `androidApp` via AndroidX Biometric; iOS
 * wiring lands with issue-18.
 *
 * `authenticate` runs the platform prompt and invokes [onSuccess] on
 * confirmation, [onFailure] on cancel or error. The common UI calls this
 * before revealing the recovery mnemonic from Settings (issue-04, criterion 3)
 * and before any other sensitive re-view.
 *
 * Kept as an interface (not an expect/actual) so `sharedUI:commonTest` can
 * supply a no-op fake without a platform source set.
 */
interface BiometricGate {
    fun authenticate(onSuccess: () -> Unit, onFailure: () -> Unit)
}

/**
 * Test/preview fake that always succeeds synchronously. Used by tooling
 * previews and any commonTest that needs a [BiometricGate].
 */
object AlwaysPassBiometricGate : BiometricGate {
    override fun authenticate(onSuccess: () -> Unit, onFailure: () -> Unit) = onSuccess()
}
