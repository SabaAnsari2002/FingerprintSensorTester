# Required Manual QA — Real Android Devices

The app doesn't fake fingerprints or mark manual scenarios as already tested.
Use at least one API 23–27 handset (if available) and a modern phone with both fingerprint
and face recognition, plus rotation, background/foreground, and system lockout tests.

| Scenario | Steps | Expected behavior |
|---|---|---|
| Enrolled fingerprint sensor | Launch on supported phone with fingerprint enrolled | Fingerprint feature reported; real system prompt opens once after splash. |
| No fingerprints / no compatible biometrics enrolled | Remove enrolled compatible biometrics then launch | "No Fingerprint Enrolled"; Open Settings and retry; no Home. |
| No fingerprint feature reported | Install on phone without `FEATURE_FINGERPRINT` | "No Fingerprint Sensor Detected"; Check Again; never show verified Home. |
| Correct fingerprint | Touch enrolled finger (where Android chooses fingerprint) | Real system success callback; success checkmark; automatically navigate to Home. Result says **biometric passed; fingerprint modality unconfirmed**. |
| Incorrect fingerprint | Try an unregistered finger once | System retries; "Fingerprint Not Recognized"; remain on test; not Home. |
| User cancels | Tap Cancel/system back on real prompt | "Authentication Cancelled"; visible Try Again; no auto retry loop. |
| Lockout | Repeatedly fail until Android locks biometric | "Authentication Temporarily Locked"; unlock phone normally before retry; do not bypass lockout. |
| Temporarily unavailable | Trigger OS/device hardware unavailability when reproducible | "Biometric Sensor Unavailable"; retry, no Home. |
| Security update / policy restricted | Test with enterprise or affected system if available | Security update or generic restricted/unknown warning; no false success. |
| System timeout | Leave prompt without interacting until timeout on a phone that times out | Timeout warning; retry available; no Home. |
| App closed / reopened | Authenticate, force-stop from Settings, relaunch | New splash and new prompt; previous history may remain but success-only Home is gated. |
| App backgrounded / reopened | Authenticate, switch to another app, return | Authorization invalidated; new authentication required; no old unlocked Home. |
| Rotate during active prompt | Trigger prompt, rotate device repeatedly | AndroidX retains/reconnects prompt; no duplicate dialog; eventual callback handled once. |
| Both face and fingerprint | Enroll both and launch | Either permitted Android modality may be chosen; **never label fingerprint-only passed**. |
| Face/iris used to authenticate | Complete using another enrolled modality where Android offers it | Green biometric success, **fingerprint-specific result inconclusive**. |
| Fingerprint-only controlled setup | Disable face/iris in Android Settings, enroll fingerprint, retest | If success, generic biometric passed; tester has external context but app still does not claim programmatic modality verification. |
| Retest successful result | On Home, press Test Again | Home authorization cleared; capability rechecked; new real prompt; success only from new callback. |
| Enrollment from Settings | Start unregistered, open Settings, enroll supported biometrics, return | Capability re-evaluated; prompt starts only if eligible. |
| No Settings activity | Test on unusual OEM without enrollment intent resolution | Falls back to Android Security settings or shows noncrashing Toast. |
| Switch light/dark / small screen | Switch appearance; rotate; enable large text / TalkBack | Legible interface, scrolling, accessible labels and non-color-only status text. |
| Clear history | Authenticate then open Details and clear history | Only local history is removed; current session state not changed. |
| Internet disabled | Disable connectivity, start and complete test | All flows function without a network connection. |

## Automation / unit tests

Run `./gradlew :app:testDebugUnitTest`. The included JUnit tests cover capability
policy outcomes: fingerprint hardware absent/unknown, missing enrollment, temporary
unavailability, hardware incompatibility, unknown and security-update codes, weak-vs-strong independence, and rejection of device-credential
callback results. These cannot replace physical authentication tests.

## Important acceptance checks

1. No normal path can call `navigate("home")` without `state.authorized` after a genuine success callback.
2. Prompt request tokens are consumed before calling `authenticate`; callbacks from invalid
   phases are ignored; no Compose recomposition invokes an authentication request.
3. Device-credential fallback is **not** configured; a returned credential result is rejected.
4. `BIOMETRIC_WEAK` availability is **not** evidence that a fingerprint is enrolled when
   face/iris is also enrolled; and a success does not identify the modality.
5. A fingerprint feature flag is a hardware *feature report* rather than an electrical test.
