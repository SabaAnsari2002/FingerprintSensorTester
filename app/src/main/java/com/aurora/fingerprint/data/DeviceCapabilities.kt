package com.aurora.fingerprint.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager

/** Reported by Android, not a low-level physical sensor diagnostic. */
data class CapabilityReport(
    val fingerprintFeature: Boolean?,
    val weakBiometrics: Int,
    val strongBiometrics: Int
)

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER.orEmpty().ifBlank { "Unknown" },
    val model: String = Build.MODEL.orEmpty().ifBlank { "Unknown" },
    val androidVersion: String = Build.VERSION.RELEASE.orEmpty().ifBlank { "Unknown" },
    val sdk: Int = Build.VERSION.SDK_INT
)

interface CapabilitySource {
    fun inspect(): CapabilityReport
}

class AndroidCapabilitySource(private val context: Context) : CapabilitySource {
    override fun inspect(): CapabilityReport {
        val feature = try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        } catch (_: RuntimeException) {
            null // Even feature detection can fail on an unusual OEM implementation.
        }
        fun check(authenticators: Int): Int = try {
            BiometricManager.from(context).canAuthenticate(authenticators)
        } catch (_: RuntimeException) {
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN
        }
        return CapabilityReport(
            fingerprintFeature = feature,
            weakBiometrics = check(BiometricManager.Authenticators.BIOMETRIC_WEAK),
            strongBiometrics = check(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        )
    }
}

enum class Gate {
    READY, NO_FINGERPRINT_FEATURE, FEATURE_UNKNOWN, NO_ENROLLMENT,
    NO_BIOMETRIC_HARDWARE, HARDWARE_UNAVAILABLE, SECURITY_UPDATE,
    UNSUPPORTED, UNKNOWN
}

/** Fingerprint FEATURE and generic biometric eligibility are independent measurements. */
object CapabilityEvaluator {
    fun evaluate(report: CapabilityReport): Gate {
        if (report.fingerprintFeature == false) return Gate.NO_FINGERPRINT_FEATURE
        if (report.fingerprintFeature == null) return Gate.FEATURE_UNKNOWN
        return when (report.weakBiometrics) {
            BiometricManager.BIOMETRIC_SUCCESS -> Gate.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Gate.NO_ENROLLMENT
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Gate.NO_BIOMETRIC_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Gate.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Gate.SECURITY_UPDATE
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Gate.UNSUPPORTED
            else -> Gate.UNKNOWN
        }
    }

    fun statusText(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_SUCCESS -> "Available"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No compatible biometrics enrolled"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No compatible biometric hardware"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Temporarily unavailable"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Authenticator combination unsupported"
        else -> "Unknown / restricted (code $code)"
    }
}
