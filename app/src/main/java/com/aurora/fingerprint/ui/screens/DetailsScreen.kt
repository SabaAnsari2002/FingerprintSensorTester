package com.aurora.fingerprint.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurora.fingerprint.vm.FingerprintUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun DetailsScreen(state: FingerprintUiState, onBack: () -> Unit,
                  onClearHistory: () -> Unit, onTheme: () -> Unit) {
    AppScaffold {
        Header("SESSION DETAILS", state.lightMode, onTheme)
        Spacer(Modifier.height(32.dp))
        CenteredIntro("Test Details", "Local diagnostic results with no biometric secrets")
        Spacer(Modifier.height(22.dp))
        InfoCard("Current session") {
            DetailRow("System authentication", "Succeeded with biometric-only authenticators")
            DetailRow("Reported modality", "Unconfirmed — OS did not identify fingerprint vs. face/iris")
            DetailRow("Fingerprint-only verification", "Not established")
            DetailRow("Credential fallback", "Disabled")
            DetailRow("Last successful test", state.lastSuccessTime?.let {
                DateFormat.getDateTimeInstance().format(Date(it))
            } ?: "Not available")
        }
        Spacer(Modifier.height(13.dp))
        InfoCard("Local test history (last 20)") {
            if (state.history.isEmpty()) {
                Text("No completed attempts recorded.", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.history.forEach { record ->
                    DetailRow(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(record.timestamp)), "${record.outcome}: ${record.explanation}")
                }
            }
            Text("History stores only timestamps and non-sensitive status descriptions on this device. It is never uploaded.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Clear Test History", onClearHistory)
        Spacer(Modifier.height(10.dp))
        MainButton("Back to Dashboard", onBack)
        PrivacyFooter()
    }
}
