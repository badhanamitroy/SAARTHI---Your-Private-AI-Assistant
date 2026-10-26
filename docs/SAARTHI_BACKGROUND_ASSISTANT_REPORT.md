# SAARTHI — Background Assistant & Native App Launching Implementation Report

**Document Version:** 1.0.0  
**Phase:** Background-Ready Assistant & Native App Launching  
**Date:** October 25, 2026  
**Package:** `com.saarthi.assistant`  
**Target SDK:** 36 | **Compile SDK:** 36 | **Min SDK:** 26  

---

## 1. Executive Summary & Verification Matrix

SAARTHI has been evolved from a foreground voice-command prototype into a background-ready Android assistant capable of:
1. Recognizing wake phrases locally (e.g. `"Hey SAARTHI, open calculator"`, `"SAARTHI, calculator kholo"`) and stripping wake prefixes cleanly.
2. Launching existing installed calculator apps via Android package visibility and Intent APIs without hardcoding a single vendor package.
3. Maintaining microphone readiness via an Android Foreground Service (`AssistantForegroundService`) with persistent notification controls (`Pause`, `Resume`, `Stop`).
4. Operating 100% locally with zero internet permissions, zero cloud fallbacks, and zero API keys.

```
+-----------------------------------------------------------------------------------------+
| VERIFICATION GATE                                              | RESULT                |
+-----------------------------------------------------------------------------------------+
| Automated Unit Tests (26/26 passed, 0 failures, 0 errors)       | PASSED                |
| Debug APK Build (`assembleDebug`)                               | PASSED                |
| Release APK Build (`assembleRelease`) with R8 Optimization      | PASSED                |
| Release APK Measured Size                                       | 1,348,593 bytes       |
| Hard Constraint Limit (< 5,000,000 bytes)                       | PASSED (3.65 MB free) |
| Zero Cloud / Remote AI Dependencies                             | VERIFIED (Pure Local) |
| Physical-Device Verification                                    | BLOCKED (No hardware) |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Feature A — Launching Existing Phone Calculator

### 2.1 Intent Routing & Classification
Utterances asking to open the calculator are classified as `OPEN_CALCULATOR` (yielding `CommandResult.OpenCalculator`) instead of triggering arithmetic evaluation:
- **English:** `"Hey SAARTHI, open calculator."`, `"Open my calculator"`, `"launch calculator"`, `"start the calculator"`, `"show calculator"`.
- **Hinglish / Hindi:** `"SAARTHI, calculator kholo."`, `"calculator kholna"`, `"calculator open karo"`, `"calculator chalu karo"`, `"calculator chalao"`.
- **Ambiguity Guarding:** When the user simply says `"calculator"` alone without action verbs or numbers, SAARTHI asks for clarification: `"Did you want to open the calculator app, or calculate an arithmetic expression?"`
- **Arithmetic Preservation:** Arithmetic queries (`"calculate 25 plus 17"`, `"what is 2 + 2"`) continue to route to `SafeMathParser`.

### 2.2 Privacy-Preserving App Detection (`CalculatorLauncher.kt`)
Rather than hardcoding a single package name, `CalculatorLauncher` searches installed apps using a 3-tier discovery strategy:
1. **Category Query:** Searches for activities registered with `Intent.CATEGORY_APP_CALCULATOR` and `Intent.ACTION_MAIN`.
2. **Known Manufacturer Packages:** Probes for standard OEM packages:
   - Samsung One UI (Galaxy A03, etc.): `com.sec.android.app.popupcalculator`
   - Google Pixel / Stock: `com.google.android.calculator`
   - AOSP Generic: `com.android.calculator2`
   - Xiaomi / MIUI: `com.miui.calculator`
   - Oppo / Realme / ColorOS: `com.coloros.calculator`
   - OnePlus / OxygenOS: `com.oneplus.calculator`
   - Tecno / Infinix / Itel: `com.transsion.calculator`
   - Asus ZenUI: `com.asus.calculator`
   - Motorola: `com.motorola.calculator`
   - Huawei / Honor: `com.huawei.calculator`
3. **Dynamic Launcher Filter:** Scans launchable apps matching `calculator` or `calc` in package identity.

### 2.3 Honest Execution & Fallback
- `context.startActivity(launchIntent)` is called with `FLAG_ACTIVITY_NEW_TASK`.
- The assistant speaks `"Opening calculator."` only when launch initiation succeeds.
- If no compatible calculator is installed, SAARTHI reports: `"No compatible calculator app was found on your phone. You can say 'calculate' followed by your numbers to use the built-in calculator."`

---

## 3. Feature B & C — Background-Ready Assistant & Android Lifecycle

### 3.1 Foreground Service Architecture (`AssistantForegroundService.kt`)
To comply with Android 14+ (API 34+) and Android 15/16 background microphone restrictions:
- The service declares `android:foregroundServiceType="microphone"`.
- Uses `startForeground()` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE`.
- Shows a persistent notification (`saarthi_assistant_channel`) displaying live assistant status.
- Notification Action Buttons:
  - **"⏸ Pause" / "▶ Resume":** Toggles voice recognition loop without stopping the service.
  - **"⏹ Stop":** Stops listening, dismisses the foreground notification, and calls `stopSelf()`.

### 3.2 Continuous Listening State Machine
Instead of treating a single short speech session as continuous listening, the service orchestrates an event-driven loop:
1. **`IDLE` / `READY`:** Recognizer ready.
2. **`LISTENING`:** `AndroidSpeechEngine.startListening()` active with `EXTRA_PREFER_OFFLINE = true`.
3. **`PROCESSING`:** On speech match, normalizes text, strips wake word (`"Hey SAARTHI"`, `"SAARTHI"`), processes command via `CommandEngine`, and speaks response via `TextToSpeechEngine`.
4. **Resumption:** When TTS completes (`onSpeechDone`), the service schedules the next recognition session after a 400ms cooldown to avoid audio feedback loops.
5. **Timeout Handling:** If no speech is detected (`ERROR_SPEECH_TIMEOUT` / `ERROR_NO_MATCH`), the next recognition session is scheduled after a 600ms backoff, avoiding busy CPU loops.

### 3.3 Permissions Added to `AndroidManifest.xml`
```xml
<!-- Existing offline audio recording -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Foreground Service for background assistant operation -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

<!-- Notification permission for Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 3.4 Package Visibility Declared (`<queries>`)
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.APP_CALCULATOR" />
    </intent>
    <package android:name="com.sec.android.app.popupcalculator" />
    <package android:name="com.google.android.calculator" />
    <package android:name="com.android.calculator2" />
    <package android:name="com.miui.calculator" />
    <package android:name="com.coloros.calculator" />
    <package android:name="com.oneplus.calculator" />
    <package android:name="com.transsion.calculator" />
</queries>
```

### 3.5 Real-World Android Restrictions & Lifecycle Realities
1. **Force Stop:** If the user opens App Info and taps "Force Stop", Android terminates all processes and cancels all pending alarms/services. No app can auto-restart after a Force Stop until manually launched by the user.
2. **Battery Optimization (OEM Doze / Aggressive Task Killers):** On devices like Samsung Galaxy A03, vendor background management may pause background network or long-running audio. Users wishing to run continuous background listening should set Battery to "Unrestricted" in App Info.
3. **Microphone Indicator:** Android 12+ displays a persistent green privacy dot in the status bar whenever the microphone is accessed by any foreground service.
4. **Screen-Off Mic Muting:** On certain budget Android devices, the hardware audio HAL enters deep sleep when the screen turns off. The foreground notification provides the user complete visibility and control.

---

## 4. Feature D — Privacy & Offline Operation

- **Zero Cloud AI / Network Access:** No Internet permission is requested. No voice data or transcripts ever leave the phone.
- **Zero Heavy Binary Models:** No 40–150 MB embedded models were bundled, keeping the release APK at **1.35 MB** (well below the 5.0 MB hard constraint).

---

## 5. Physical Device Verification Protocol

Querying `adb devices -l` showed no physical hardware attached during this step. Physical-device testing is reported as **BLOCKED / PENDING HARDWARE**.

Upon connecting the Samsung Galaxy A03:
1. `adb install app-release.apk`
2. Launch SAARTHI and tap "🎙 Enable Permissions". Grant Audio and Notification permissions.
3. Test English voice command: `"Hey SAARTHI, open calculator"`. Verify Samsung Calculator opens.
4. Test Hinglish voice command: `"SAARTHI, calculator kholo"`. Verify calculator opens.
5. Tap `"▶ Start Background Assistant"`. Verify notification appears.
6. Press Home button. From home screen, say `"Hey SAARTHI, what time is it?"`. Verify spoken time.
7. From notification shade, test `"⏸ Pause"` and `"⏹ Stop"` controls.
