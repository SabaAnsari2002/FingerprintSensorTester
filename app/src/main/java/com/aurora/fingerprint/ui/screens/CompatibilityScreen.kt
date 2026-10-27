package com.aurora.fingerprint.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurora.fingerprint.data.CapabilityEvaluator
import com.aurora.fingerprint.vm.FingerprintUiState

@Composable
fun CompatibilityScreen(state: FingerprintUiState, onBack: () -> Unit,
                        onRefresh: () -> Unit, onSettings: () -> Unit, onTheme: () -> Unit) {
    AppScaffold {
        Header("COMPATIBILITY REPORT", state.lightMode, onTheme)
        Spacer(Modifier.height(32.dp))
        CenteredIntro("Device Compatibility", "What your device's Android framework actually reports")
        Spacer(Modifier.height(23.dp))
        InfoCard("Hardware features") {
            DetailRow("PackageManager.FEATURE_FINGERPRINT", state.fingerprintReported)
            Text("The feature flag is an Android/OEM report, not a physical health test or a count of fingerprint sensors.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(13.dp))
        InfoCard("AndroidX BiometricManager") {
            DetailRow("BIOMETRIC_WEAK (biometrics only)", state.report?.let {
                CapabilityEvaluator.statusText(it.weakBiometrics)
            } ?: "Unavailable")
            DetailRow("BIOMETRIC_STRONG (diagnostic only)", state.report?.let {
                CapabilityEvaluator.statusText(it.strongBiometrics)
            } ?: "Unavailable")
            DetailRow("Requested prompt type", "BIOMETRIC_WEAK — no device credential fallback")
        }
        Spacer(Modifier.height(13.dp))
        InfoCard("What this does not reveal") {
            Text("AndroidX BiometricPrompt does not reliably report whether fingerprint, face, or iris was used. The app cannot establish fingerprint enrollment independently when another biometric method is enrolled, discover sensor position, or diagnose defective internal components.",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(18.dp))
        MainButton("Recheck Compatibility", onRefresh)
        Spacer(Modifier.height(9.dp))
        SecondaryButton("Open Biometric Settings", onSettings)
        Spacer(Modifier.height(9.dp))
        SecondaryButton("Back", onBack)
        PrivacyFooter()
    }
}
