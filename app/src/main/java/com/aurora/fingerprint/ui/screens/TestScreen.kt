package com.aurora.fingerprint.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurora.fingerprint.data.Gate
import com.aurora.fingerprint.vm.FingerprintUiState
import com.aurora.fingerprint.vm.Notice
import com.aurora.fingerprint.vm.TestPhase

private data class Message(val title: String, val body: String, val settingsUseful: Boolean = false)

private fun messageFor(state: FingerprintUiState): Message {
    return when (state.notice) {
        Notice.CANCELLED -> Message("Authentication Cancelled", "The system prompt was dismissed. You can try again whenever you're ready.")
        Notice.LOCKED -> Message("Authentication Temporarily Locked", "Unlock your device using its normal security method, then try again.")
        Notice.UNAVAILABLE -> Message("Biometric Sensor Unavailable", "The biometric system is currently unavailable. Please try again later.")
        Notice.TIMEOUT -> Message("Authentication Timed Out", "The system stopped waiting for authentication. Tap Try Again to restart.")
        Notice.NO_ENROLLMENT -> Message("No Fingerprint Enrolled", "Please register a fingerprint in your phone's security settings.", true)
        Notice.NO_HARDWARE -> Message("Biometric Hardware Unavailable", "Android reports no hardware compatible with the requested biometric authenticator.")
        Notice.SECURITY_UPDATE -> Message("Security Update Required", "Android reports a biometric security restriction. Install pending device updates and retry.", true)
        Notice.FAILED -> Message("Fingerprint Not Recognized", "The biometric system could not process this attempt. Please try again.")
        Notice.RESTRICTED -> Message("Authentication Not Available", state.noticeDetail.ifBlank { "Android could not start or complete biometric authentication." })
        null -> when (state.gate) {
            Gate.NO_FINGERPRINT_FEATURE -> Message("No Fingerprint Sensor Detected", "This device does not report built-in fingerprint hardware.")
            Gate.FEATURE_UNKNOWN -> Message("Fingerprint Status Unknown", "Android could not determine whether fingerprint hardware is reported.")
            Gate.NO_ENROLLMENT -> Message("No Fingerprint Enrolled", "Please register a fingerprint in your phone's security settings.", true)
            Gate.NO_BIOMETRIC_HARDWARE -> Message("Biometric Hardware Not Available", "Android reports no compatible biometric hardware, even though a fingerprint feature is reported.")
            Gate.HARDWARE_UNAVAILABLE -> Message("Biometric Sensor Unavailable", "The biometric system is currently unavailable. Please try again later.")
            Gate.SECURITY_UPDATE -> Message("Security Update Required", "Android requires a security update before biometric authentication can be used.", true)
            Gate.UNSUPPORTED -> Message("Biometric Authentication Unsupported", "This Android implementation cannot use the requested biometric-only authenticator configuration.")
            Gate.UNKNOWN -> Message("Biometric Availability Unknown", "Android could not establish biometric readiness. Review device security policies and try again.")
            Gate.READY -> Message("Ready to Authenticate", "Use the system prompt to verify biometric authentication.")
        }
    }
}

@Composable
fun TestScreen(state: FingerprintUiState, onRetry: () -> Unit, onSettings: () -> Unit,
               onCompatibility: () -> Unit, onTheme: () -> Unit) {
    val loading = state.phase == TestPhase.SPLASH || state.phase == TestPhase.CHECKING
    val prompting = state.phase == TestPhase.PROMPTING
    val succeeded = state.phase == TestPhase.SUCCESS || state.phase == TestPhase.AUTHENTICATED
    val message = messageFor(state)
    val successColor = MaterialTheme.colorScheme.secondary
    AppScaffold {
        Header("SENSOR DIAGNOSTICS", state.lightMode, onTheme)
        Spacer(Modifier.height(19.dp))
        Tag(if (succeeded) "AUTHENTICATED" else if (prompting) "SYSTEM PROMPT ACTIVE" else "PRIVATE DEVICE CHECK",
            if (succeeded) successColor else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(11.dp))
        GlowScanner(active = loading || prompting, success = succeeded)
        Spacer(Modifier.height(6.dp))
        when {
            succeeded -> {
                CenteredIntro("Authentication Successful!", "Your device successfully completed biometric authentication.")
                Spacer(Modifier.height(22.dp))
                Tag("BIOMETRIC SUCCESS", successColor)
            }
            loading -> {
                CenteredIntro(if (state.phase == TestPhase.SPLASH) "Fingerprint Sensor Tester" else "Checking device support",
                    "Checking the capabilities Android reports for this device.")
                Spacer(Modifier.height(25.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            prompting -> {
                CenteredIntro("Fingerprint Sensor Test", "Place your finger on the fingerprint sensor")
                Spacer(Modifier.height(16.dp))
                InfoCard("Live test status") {
                    Tag("Biometric authentication available", successColor)
                    Text(state.scanFeedback ?: "The Android system prompt is managing authentication. Follow its instructions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.scanFeedback == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                CenteredIntro(message.title, message.body)
                Spacer(Modifier.height(23.dp))
                InfoCard("Reported status") {
                    DetailRow("Fingerprint hardware feature", state.fingerprintReported)
                    DetailRow("Biometric authentication", state.report?.let {
                        com.aurora.fingerprint.data.CapabilityEvaluator.statusText(it.weakBiometrics)
                    } ?: "Not checked")
                }
                Spacer(Modifier.height(15.dp))
                MainButton(if (state.phase == TestPhase.NOTICE) "Try Again / Check Again" else "Start Test", onRetry)
                if (message.settingsUseful) {
                    Spacer(Modifier.height(9.dp))
                    SecondaryButton("Open Settings", onSettings)
                }
            }
        }
        if (!loading) {
            Spacer(Modifier.height(17.dp))
            Text("A successful result confirms biometric authentication, not necessarily fingerprint-only use. Android may choose face or iris on some phones.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(15.dp))
            SecondaryButton("Check Device Compatibility", onCompatibility)
        }
        PrivacyFooter()
    }
}
