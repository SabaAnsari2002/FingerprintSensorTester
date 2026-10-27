package com.aurora.fingerprint.data

import androidx.biometric.BiometricPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricResultPolicyTest {
    @Test fun genuineBiometricCallbackCanPass() {
        assertTrue(BiometricResultPolicy.accepts(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC))
    }

    @Test fun unknownResultOnOlderOsMayPassBiometricOnlyPrompt() {
        assertTrue(BiometricResultPolicy.accepts(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN))
    }

    @Test fun deviceCredentialsNeverPass() {
        assertFalse(BiometricResultPolicy.accepts(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL))
        assertFalse(BiometricResultPolicy.accepts(999))
    }
}
