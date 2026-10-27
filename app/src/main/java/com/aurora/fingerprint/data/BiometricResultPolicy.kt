package com.aurora.fingerprint.data

import androidx.biometric.BiometricPrompt

/** Only for prompts configured exclusively with BIOMETRIC_WEAK; no credential fallback. */
object BiometricResultPolicy {
    fun accepts(authenticationType: Int): Boolean = when (authenticationType) {
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> true
        // Some older versions cannot report the exact authenticator type. Because this
        // app configured biometric-only, the real success callback is still meaningful.
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN -> true
        else -> false // Includes device credential and unexpected OEM codes.
    }

    /** AndroidX never supplies a trustworthy fingerprint/face/iris identifier. */
    const val VERIFIED_MODALITY = "Biometric / modality unconfirmed"
}
