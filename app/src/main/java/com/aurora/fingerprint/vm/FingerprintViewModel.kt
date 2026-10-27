package com.aurora.fingerprint.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.biometric.BiometricPrompt
import com.aurora.fingerprint.data.BiometricResultPolicy
import com.aurora.fingerprint.data.AndroidCapabilitySource
import com.aurora.fingerprint.data.CapabilityEvaluator
import com.aurora.fingerprint.data.CapabilityReport
import com.aurora.fingerprint.data.CapabilitySource
import com.aurora.fingerprint.data.DeviceInfo
import com.aurora.fingerprint.data.Gate
import com.aurora.fingerprint.data.TestHistory
import com.aurora.fingerprint.data.TestRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestPhase { SPLASH, CHECKING, READY, PROMPTING, SUCCESS, AUTHENTICATED, NOTICE }
enum class Notice { CANCELLED, LOCKED, UNAVAILABLE, TIMEOUT, NO_ENROLLMENT, NO_HARDWARE,
    SECURITY_UPDATE, FAILED, RESTRICTED }

data class FingerprintUiState(
    val phase: TestPhase = TestPhase.SPLASH,
    val report: CapabilityReport? = null,
    val gate: Gate = Gate.UNKNOWN,
    val notice: Notice? = null,
    val noticeDetail: String = "",
    val scanFeedback: String? = null,
    val launchToken: Long? = null,
    val authorized: Boolean = false,
    val lightMode: Boolean = false,
    val lastSuccessTime: Long? = null,
    val history: List<TestRecord> = emptyList(),
    val device: DeviceInfo = DeviceInfo(),
) {
    val fingerprintReported: String get() = when (report?.fingerprintFeature) {
        true -> "Yes (reported by Android)"
        false -> "No (not reported)"
        null -> "Unknown"
    }
    val biometricAvailable: String get() = when (report?.let { CapabilityEvaluator.evaluate(it) }) {
        Gate.READY -> "Yes"
        else -> "No / undetermined"
    }
}

class FingerprintViewModel(app: Application) : AndroidViewModel(app) {
    private val source: CapabilitySource = AndroidCapabilitySource(app)
    private val historyRepo = TestHistory(app)
    private val mutable = MutableStateFlow(FingerprintUiState(history = historyRepo.read()))
    val state: StateFlow<FingerprintUiState> = mutable.asStateFlow()

    private var hostResumed = false
    private var shouldAutoLaunch = false
    private var tokenCounter = 0L

    init {
        viewModelScope.launch {
            delay(850) // A short branded splash, never a simulated sensor measurement.
            refresh(autoLaunch = true)
        }
    }

    fun toggleTheme() = mutable.update { it.copy(lightMode = !it.lightMode) }

    fun refresh(autoLaunch: Boolean) {
        if (mutable.value.phase == TestPhase.PROMPTING) return
        shouldAutoLaunch = false
        mutable.update { it.copy(phase = TestPhase.CHECKING, launchToken = null,
            scanFeedback = null, notice = null, noticeDetail = "", authorized = false) }
        val report = source.inspect()
        val gate = CapabilityEvaluator.evaluate(report)
        shouldAutoLaunch = autoLaunch && gate == Gate.READY
        mutable.update { it.copy(report = report, gate = gate,
            phase = if (gate == Gate.READY) TestPhase.READY else TestPhase.NOTICE) }
        launchWhenEligible()
    }

    fun onHostResumed() {
        hostResumed = true
        launchWhenEligible()
    }

    fun onHostStopped(isChangingConfigurations: Boolean) {
        hostResumed = false
        if (isChangingConfigurations) return
        // The success-only dashboard never stays unlocked across background sessions.
        if (mutable.value.authorized) {
            refresh(autoLaunch = true)
        }
        // Don't disturb an OS-owned prompt. AndroidX handles external cancellation.
    }

    private fun launchWhenEligible() {
        if (hostResumed && shouldAutoLaunch && mutable.value.phase == TestPhase.READY) {
            shouldAutoLaunch = false
            tokenCounter++
            mutable.update { it.copy(phase = TestPhase.PROMPTING,
                launchToken = tokenCounter, scanFeedback = null) }
        }
    }

    /** Consumed BEFORE authenticate() to avoid duplicates on rotation / Flow collection. */
    fun consumeLaunchToken(token: Long): Boolean {
        if (mutable.value.phase != TestPhase.PROMPTING || mutable.value.launchToken != token) return false
        mutable.update { it.copy(launchToken = null) }
        return true
    }

    fun recheckCompatibility() {
        if (mutable.value.phase == TestPhase.PROMPTING) return
        val report = source.inspect()
        mutable.update { it.copy(report = report, gate = CapabilityEvaluator.evaluate(report)) }
    }

    fun testAgain() {
        if (mutable.value.phase == TestPhase.PROMPTING || mutable.value.phase == TestPhase.SUCCESS) return
        refresh(autoLaunch = true)
    }

    fun onPromptFailedScan() {
        if (mutable.value.phase == TestPhase.PROMPTING) {
            mutable.update { it.copy(scanFeedback = "Fingerprint Not Recognized — Please try again.") }
        }
        // This is a nonterminal callback; Android manages subsequent retries.
    }

    fun onPromptSuccess(authenticationType: Int) {
        if (mutable.value.phase != TestPhase.PROMPTING) return
        // Credential fallback was never enabled. Explicitly reject it if an OEM violates the contract.
        if (!BiometricResultPolicy.accepts(authenticationType)) {
            terminalNotice(Notice.RESTRICTED, "Unexpected authenticator result; biometric test not passed.")
            return
        }
        val timestamp = System.currentTimeMillis()
        saveRecord("Passed (biometric)", "Android confirmed biometric authentication; modality not disclosed")
        mutable.update { it.copy(phase = TestPhase.SUCCESS, lastSuccessTime = timestamp,
            scanFeedback = null, launchToken = null, notice = null) }
        viewModelScope.launch {
            delay(900)
            if (mutable.value.phase == TestPhase.SUCCESS) {
                mutable.update { it.copy(phase = TestPhase.AUTHENTICATED, authorized = true) }
            }
        }
    }

    fun onPromptError(code: Int, systemMessage: String) {
        if (mutable.value.phase != TestPhase.PROMPTING) return
        val mapped = when (code) {
            BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED -> Notice.CANCELLED
            BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> Notice.LOCKED
            BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_CANCELED -> Notice.UNAVAILABLE
            BiometricPrompt.ERROR_TIMEOUT -> Notice.TIMEOUT
            BiometricPrompt.ERROR_NO_BIOMETRICS -> Notice.NO_ENROLLMENT
            BiometricPrompt.ERROR_HW_NOT_PRESENT -> Notice.NO_HARDWARE
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> Notice.SECURITY_UPDATE
            BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> Notice.FAILED
            else -> Notice.RESTRICTED
        }
        // Never infer the scanned modality from the OEM's error message.
        terminalNotice(mapped, if (mapped == Notice.RESTRICTED) systemMessage.take(160) else "")
    }

    fun onPromptLaunchException(message: String) {
        if (mutable.value.phase == TestPhase.PROMPTING) {
            terminalNotice(Notice.RESTRICTED, message.take(160))
        }
    }

    private fun terminalNotice(notice: Notice, details: String) {
        saveRecord("Not passed", notice.name.replace('_', ' ').lowercase())
        mutable.update { it.copy(phase = TestPhase.NOTICE, notice = notice, noticeDetail = details,
            scanFeedback = null, launchToken = null, authorized = false) }
    }

    private fun saveRecord(outcome: String, description: String) {
        val list = historyRepo.append(TestRecord(System.currentTimeMillis(), outcome, description))
        mutable.update { it.copy(history = list) }
    }

    fun clearHistory() {
        historyRepo.clear()
        mutable.update { it.copy(history = emptyList()) }
    }
}
