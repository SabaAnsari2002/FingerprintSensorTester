package com.aurora.fingerprint

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.aurora.fingerprint.biometric.BiometricController
import com.aurora.fingerprint.ui.screens.CompatibilityScreen
import com.aurora.fingerprint.ui.screens.DetailsScreen
import com.aurora.fingerprint.ui.screens.HomeScreen
import com.aurora.fingerprint.ui.screens.TestScreen
import com.aurora.fingerprint.ui.theme.FingerprintTheme
import com.aurora.fingerprint.vm.FingerprintViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * FragmentActivity is mandatory for androidx.biometric:biometric 1.1.0.
 * Activity-scoped ViewModel survives rotation; AndroidX BiometricPrompt owns retained fragments.
 */
class MainActivity : FragmentActivity() {
    private val model: FingerprintViewModel by viewModels()
    private lateinit var biometric: BiometricController

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Enrollment may have changed while Settings was open. Re-evaluate with no assumptions.
        model.refresh(autoLaunch = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create on EVERY Activity creation so the retained BiometricFragment can receive a fresh callback.
        biometric = BiometricController(this, object : BiometricController.Listener {
            override fun onSuccess(authenticationType: Int) = model.onPromptSuccess(authenticationType)
            override fun onFailedScan() = model.onPromptFailedScan()
            override fun onError(errorCode: Int, message: String) = model.onPromptError(errorCode, message)
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.state.map { it.launchToken }.filterNotNull().collect { token ->
                    if (model.consumeLaunchToken(token)) {
                        try {
                            biometric.authenticate() // Official system-managed UI, never a simulation.
                        } catch (failure: RuntimeException) {
                            model.onPromptLaunchException(failure.localizedMessage ?: "Unable to launch the system prompt")
                        }
                    }
                }
            }
        }

        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            FingerprintTheme(lightMode = state.lightMode) {
                FingerprintNavigation(
                    state = state,
                    onRetest = model::testAgain,
                    onRecheck = model::recheckCompatibility,
                    onOpenSettings = ::openBiometricSettings,
                    onClearHistory = model::clearHistory,
                    onTheme = model::toggleTheme
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.onHostResumed()
    }

    override fun onStop() {
        model.onHostStopped(isChangingConfigurations)
        super.onStop()
    }

    private fun openBiometricSettings() {
        val preferred = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.BIOMETRIC_WEAK)
                }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            settingsLauncher.launch(preferred)
        } catch (_: ActivityNotFoundException) {
            openFallbackSettings()
        } catch (_: SecurityException) {
            openFallbackSettings()
        }
    }

    private fun openFallbackSettings() {
        try {
            settingsLauncher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open Android security settings on this device.", Toast.LENGTH_LONG).show()
        }
    }
}

private object Destinations {
    const val TEST = "test"
    const val HOME = "home"
    const val COMPATIBILITY = "compatibility"
    const val DETAILS = "details"
}

@Composable
private fun FingerprintNavigation(
    state: com.aurora.fingerprint.vm.FingerprintUiState,
    onRetest: () -> Unit,
    onRecheck: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit,
    onTheme: () -> Unit
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    LaunchedEffect(state.authorized, current) {
        if (state.authorized && current == Destinations.TEST) {
            nav.navigate(Destinations.HOME) {
                popUpTo(Destinations.TEST) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!state.authorized &&
            (current == Destinations.HOME || current == Destinations.DETAILS)) {
            nav.navigate(Destinations.TEST) {
                popUpTo(Destinations.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Destinations.TEST,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(Destinations.TEST) {
            TestScreen(state, onRetry = onRetest, onSettings = onOpenSettings,
                onCompatibility = { nav.navigate(Destinations.COMPATIBILITY) }, onTheme = onTheme)
        }
        composable(Destinations.HOME) {
            if (state.authorized) {
                HomeScreen(state,
                    onRetry = {
                        // Explicit retest clears authorization, then returns to the test screen.
                        onRetest()
                        nav.navigate(Destinations.TEST) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCompatibility = { nav.navigate(Destinations.COMPATIBILITY) },
                    onSettings = onOpenSettings,
                    onDetails = { nav.navigate(Destinations.DETAILS) }, onTheme = onTheme)
            }
        }
        composable(Destinations.COMPATIBILITY) {
            CompatibilityScreen(state, onBack = { nav.popBackStack() },
                onRefresh = onRecheck, onSettings = onOpenSettings, onTheme = onTheme)
        }
        composable(Destinations.DETAILS) {
            if (state.authorized) {
                DetailsScreen(state, onBack = { nav.popBackStack() },
                    onClearHistory = onClearHistory, onTheme = onTheme)
            }
        }
    }
}
