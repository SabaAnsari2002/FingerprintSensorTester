package com.aurora.fingerprint.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurora.fingerprint.vm.FingerprintUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(state: FingerprintUiState, onRetry: () -> Unit,
               onCompatibility: () -> Unit, onSettings: () -> Unit,
               onDetails: () -> Unit, onTheme: () -> Unit) {
    val successColor = MaterialTheme.colorScheme.secondary
    AppScaffold {
        Header("DEVICE VERIFICATION", state.lightMode, onTheme)
        Spacer(Modifier.height(21.dp))
        Tag("SYSTEM AUTHENTICATION PASSED", successColor)
        Spacer(Modifier.height(2.dp))
        GlowScanner(active = false, success = true)
        CenteredIntro("Authentication Successful!", "Your device successfully completed biometric authentication.")
        Spacer(Modifier.height(23.dp))
        InfoCard("Verified result") {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "Verification checkmark",
                tint = successColor, modifier = Modifier.height(30.dp))
            DetailRow("Biometric authentication", "Passed", successColor)
            Text("Android's genuine authentication callback succeeded. This does not prove which biometric modality was used or that the hardware is defect-free.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Tag("FINGERPRINT-ONLY RESULT: INCONCLUSIVE", MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("Fingerprint sensor information") {
            DetailRow("Fingerprint hardware detected", state.fingerprintReported)
            DetailRow("Biometric authentication available", state.biometricAvailable)
            DetailRow("Authentication result", "Passed (biometric)", successColor)
            DetailRow("Authentication method", com.aurora.fingerprint.data.BiometricResultPolicy.VERIFIED_MODALITY)
            DetailRow("Fingerprint test result", "Inconclusive (modality not disclosed)")
            DetailRow("Last test", state.lastSuccessTime?.let {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
            } ?: "This session")
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("Device information") {
            DetailRow("Manufacturer", state.device.manufacturer)
            DetailRow("Model", state.device.model)
            DetailRow("Android version", state.device.androidVersion)
            DetailRow("SDK version", state.device.sdk.toString())
        }
        Spacer(Modifier.height(18.dp))
        MainButton("Test Again", onRetry)
        Spacer(Modifier.height(9.dp))
        SecondaryButton("Check Device Compatibility", onCompatibility)
        Spacer(Modifier.height(9.dp))
        SecondaryButton("Open Biometric Settings", onSettings)
        Spacer(Modifier.height(9.dp))
        SecondaryButton("View Test Details & History", onDetails)
        Spacer(Modifier.height(15.dp))
        Text("To test fingerprint specifically, temporarily disable other biometric methods in Android Settings, then retest. The app cannot read or change those settings for you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        PrivacyFooter()
    }
}
