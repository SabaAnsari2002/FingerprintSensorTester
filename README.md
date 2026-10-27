# Fingerprint Sensor Tester

A native, offline-first Kotlin Android application using Jetpack Compose (Material 3), MVVM,
AndroidX BiometricPrompt, BiometricManager and Navigation Compose.

**Android 6.0+ (API 23), compile/target Android 17 (API 37).**

> **Diagnostic limitation:** Android's generic `BiometricPrompt` can permit fingerprint,
> face or iris on compatible phones. AndroidX `AuthenticationResult.authenticationType`
> identifies **biometric vs device credential**, not which biometric modality.
> This app never claims "Fingerprint Test Passed" from an ambiguous success.
> Its green result confirms **real biometric authentication**; its fingerprint-only
> verdict is **inconclusive** unless the user has established fingerprint-only
> enrollment outside the app. The app can't verify that configuration.

## Getting started

1. Install a recent stable **Android Studio** with support for AGP 9.4, **JDK 17+**, and
   **Android SDK Platform 37** via SDK Manager.
2. Extract the project ZIP, then select **Open** in Android Studio and choose the extracted
   `FingerprintSensorTester/` directory (not the `app/` directory).
3. Accept Gradle sync. The project uses **Android Gradle Plugin 9.4.0**, **Gradle 9.6.1**,
   Kotlin Compose compiler **2.4.10** and Compose BOM **2026.09.00**.
4. Connect a **real Android phone** with USB debugging enabled; select it and click **Run**.
   Android emulators may simulate biometric callbacks and **cannot establish physical sensor health**.
5. Alternatively, from the project root run:

   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:testDebugUnitTest
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   On Windows use `gradlew.bat`.

### Gradle bootstrap information

The bundled small `gradle-wrapper.jar` is a **minimal self-contained bootstrap**, not the
original Gradle-distributed wrapper binary. The authoring environment cannot fetch the
official wrapper JAR. It downloads **only Gradle 9.6.1 via HTTPS**, verifies the official
published SHA-256 checksum before extracting, and invokes Gradle. Its Java source is in
`docs/GradleWrapperMain.java`, along with the official checksum and explanation. After the first
successful sync, you can replace the bootstrap with an **official Gradle wrapper** using
`gradle wrapper --gradle-version 9.6.1` if preferred.

The first Gradle/AndroidX dependency download needs an internet connection on the
**development machine**; **the installed app does not**.

## User flow

Launch → short branded splash → `PackageManager.FEATURE_FINGERPRINT` feature report →
`BiometricManager.canAuthenticate(BIOMETRIC_WEAK)` → real AndroidX biometric dialog
(automatically, once ready) → system success callback → green verification transition → Home.

- The **Home** and **Test Details** routes are guarded by the in-memory success state.
- A new process / new Activity session requires another test. Returning to an already
  verified app after backgrounding also resets its in-memory authorization (except rotations).
- On API 23–27, AndroidX uses its compatibility fingerprint dialog backed by real
  system biometric hardware APIs (the framework `BiometricPrompt` UI starts at API 28).
- A prompt already running during rotation is **not** launched again; AndroidX restores its
  retained fragment and the recreated Activity installs a fresh callback.
- Cancel, lockout, nonmatching finger, timeout, or error **cannot** unlock Home.
- "Test Again" explicitly drops authorization and reruns capability checks.
- Enrollment button opens Android biometric/security settings (with fallback).
- Compatibility view reports both weak and strong generic biometric readiness.
- Details displays the last 20 **local status summaries**; Clear History erases them.
- The theme button switches dark (default) / light appearance for the current session.

## Why BIOMETRIC_WEAK?

This is a hardware compatibility tester, not a payment/authenticator vault. Many genuine
fingerprint sensors do not qualify as Class 3/STRONG on all OS builds. `BIOMETRIC_WEAK`
includes Class 2 and Class 3 biometric authenticators while excluding device credentials.
A separate `BIOMETRIC_STRONG` check is shown for diagnostic information, but not required
for the test. `setNegativeButtonText("Cancel")` is used, with **no `DEVICE_CREDENTIAL` flag**.

Because Android may also permit face or iris, users wanting stronger fingerprint-specific
evidence should temporarily disable **other enrolled biometric methods in Android Settings**,
then retest. This remains a **user-controlled procedure, not programmatic proof**.

## Architecture / project map

```
app/src/main/
  AndroidManifest.xml                 USE_BIOMETRIC & USE_FINGERPRINT, fingerprint optional
  java/com/aurora/fingerprint/
    MainActivity.kt                   Prompt host; lifecycle and Navigation Compose
    biometric/BiometricController.kt  System BiometricPrompt (never simulated)
    data/DeviceCapabilities.kt        PackageManager + BiometricManager + policy evaluation
    data/BiometricResultPolicy.kt    Accepts only biometric/legacy-unknown success callbacks
    data/TestHistory.kt               Local, bounded and clearable status history
    vm/FingerprintViewModel.kt        Single source of truth; attempt token; gated success
    ui/theme/Theme.kt                 Material 3 color schemes and typography
    ui/screens/Components.kt          Reusable cards, actions, decorative scanner
    ui/screens/TestScreen.kt          Splash, ready, prompted, success, unsupported/error
    ui/screens/HomeScreen.kt          Guarded results dashboard
    ui/screens/CompatibilityScreen.kt Hardware/biometric system capabilities
    ui/screens/DetailsScreen.kt       Guarded session details and history
  res/mipmap-*                        App launcher assets
app/src/test/
    .../CapabilityEvaluatorTest.kt    Hardware/authentication-gate unit tests
    .../BiometricResultPolicyTest.kt  Rejects credential and invalid success types
TESTING.md                            Full real-device validation checklist
```

## Security and privacy

- Uses only Android official/framework and AndroidX authentication APIs.
- Does not access fingerprint templates, frames, sensor geometry, or secrets.
- Does not use `FingerprintManager`, device credential fallback, internet, analytics,
  OEM-specific hidden APIs, accessibility, or privileged permissions.
- Stores **only** local timestamps / coarse non-sensitive test outcome strings in
  `SharedPreferences`. No raw secrets or identifying account data are stored.
- Application backup is disabled; history is not sent to a server.
- `FEATURE_FINGERPRINT` reports the feature declared by Android/OEM; it is **not** proof
  that a sensor exists physically or works properly.
- System errors (including unsupported code values) are safely handled. Some OEMs
  may expose undocumented policy restrictions only through generic/unknown results.

## Production verification status

Sources and configuration are provided as a complete Android Studio project, with unit tests
and a manual QA checklist. **An APK was not built and no physical-device authentication was
executed in the authoring environment**, because it does not have Android SDK/Gradle dependency
artifacts or a connected Android handset. Run `:app:assembleDebug`, the unit tests, and the
real-device cases in `TESTING.md` before shipping or claiming device certification.
