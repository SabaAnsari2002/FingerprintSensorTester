package com.aurora.fingerprint.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The official AndroidX prompt is backed by FragmentManager and reattaches across rotations.
 * Construct this in each Activity.onCreate, not inside Compose or on each recomposition.
 * Never configure DEVICE_CREDENTIAL; PIN/pattern/password cannot pass this test.
 */
class BiometricController(
    activity: FragmentActivity,
    private val listener: Listener
) {
    interface Listener {
        fun onSuccess(authenticationType: Int)
        fun onFailedScan()
        fun onError(errorCode: Int, message: String)
    }

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                listener.onSuccess(result.authenticationType)
            }

            override fun onAuthenticationFailed() {
                listener.onFailedScan()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                listener.onError(errorCode, errString.toString())
            }
        }
    )

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Fingerprint Sensor Test")
        .setSubtitle("Verify your device's fingerprint sensor")
        .setDescription("Touch your phone's fingerprint sensor to continue.")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText("Cancel")
        .build()

    fun authenticate() = prompt.authenticate(promptInfo)
    // AndroidX and the OS own and position the actual prompt / under-screen UI.
}
