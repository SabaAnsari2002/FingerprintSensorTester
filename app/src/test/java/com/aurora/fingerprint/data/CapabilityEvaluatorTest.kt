package com.aurora.fingerprint.data

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityEvaluatorTest {
    private val success = BiometricManager.BIOMETRIC_SUCCESS

    @Test fun readyRequiresReportedFingerprintFeatureAndBiometricSuccess() {
        assertEquals(Gate.READY, CapabilityEvaluator.evaluate(CapabilityReport(true, success, success)))
        assertEquals(Gate.NO_FINGERPRINT_FEATURE, CapabilityEvaluator.evaluate(CapabilityReport(false, success, success)))
        assertEquals(Gate.FEATURE_UNKNOWN, CapabilityEvaluator.evaluate(CapabilityReport(null, success, success)))
    }

    @Test fun noEnrollmentIsNotClassifiedAsDefectiveHardware() {
        assertEquals(Gate.NO_ENROLLMENT, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED, success)))
    }

    @Test fun availabilityStatusesStayDistinct() {
        assertEquals(Gate.HARDWARE_UNAVAILABLE, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, success)))
        assertEquals(Gate.SECURITY_UPDATE, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED, success)))
        assertEquals(Gate.UNSUPPORTED, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED, success)))
        assertEquals(Gate.UNKNOWN, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN, success)))
        assertEquals(Gate.NO_BIOMETRIC_HARDWARE, CapabilityEvaluator.evaluate(CapabilityReport(true,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, success)))
    }

    @Test fun weakBiometricMayBeUsableWhenStrongIsNot() {
        assertEquals(Gate.READY, CapabilityEvaluator.evaluate(CapabilityReport(true, success,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)))
    }
}
