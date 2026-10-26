# SAARTHI — Phase 3 Readiness Audit & Implementation Planning

**Document Version:** 3.0.0  
**Date:** October 26, 2026  
**Auditor:** Antigravity Architecture & Mobile Systems Group  
**Project Root:** `D:\SAARTHI - Your Private AI Assistant`  
**Package:** `com.saarthi.assistant`  
**Current Milestone:** Phase 2 Complete → Phase 3 Planning ("Offline Essentials & Reliable Phone Actions")  

---

## 1. Executive Summary

Following the successful resolution of the P0 startup crash (caused by an undeclared `ACCESS_NETWORK_STATE` manifest permission and unhandled `SecurityException` during initial composition), **SAARTHI launches and renders stably on physical Android devices**. 

This audit was conducted to inspect the entire codebase, measure test verification status, analyze architectural constraints, and establish an evidence-based roadmap for **Phase 3: Offline Essentials & Reliable Phone Actions**.

```
+---------------------------------------------------------------------------------------------------+
| AUDIT METRIC                                  | VERIFIED VALUE                                   |
+---------------------------------------------------------------------------------------------------+
| Automated Unit Tests                          | 66 passed, 0 failures, 0 errors, 0 skipped        |
| Release Build Status (`assembleRelease`)      | BUILD SUCCESSFUL (59s)                            |
| Measured Release APK Size                     | 1,561,849 bytes (~1.49 MB)                        |
| Size Constraint Target (< 5,000,000 bytes)    | PASSED (3.44 MB headroom remaining)               |
| Min SDK / Target SDK / Compile SDK            | Min 26 (Android 8.0) | Target 36 | Compile 36     |
| UI Framework                                  | Native Kotlin & Jetpack Compose (Material 3)      |
| Physical Device Launch Status                 | DEVICE-VERIFIED (Launches without crashing)       |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Source Code Forensic Audit

Every architectural subsystem was inspected against actual Kotlin code rather than documentation claims. Below are the verified answers to the mandatory audit inquiries:

### 2.1 Input Modality: Speech vs. Manual Text
- **Source Files:** [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt), [HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt), [ConversationScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ConversationScreen.kt), [TextCommandInput.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TextCommandInput.kt).
- **Verified Reality:** **Both microphone speech and manual text input are fully implemented and functional.**
- **Routing:** Both `speechEngine.startListening { text -> processCommand(text) }` and `TextCommandInput` (`onSend = { text -> processCommand(text) }`) dispatch into the identical unified `processCommand(text)` router in `MainActivity.kt`.
- **Status:** **IMPLEMENTED**, **AUTOMATED-TESTED**, **BUILD-VERIFIED**.

### 2.2 Speech-Recognition API & Offline Availability
- **Source File:** [AndroidSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt).
- **API Used:** Android Platform `android.speech.SpeechRecognizer` with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- **Offline Request:** In `startListening()`, line 116 explicitly injects:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
  }
  ```
- **Technical Reality:** `EXTRA_PREFER_OFFLINE` is an Android framework **hint**, not an on-device neural model. The Android OS routes this request to the device's installed speech recognition provider (e.g. Google Speech Services, Samsung Voice). If the user has downloaded offline language speech packs in device settings, recognition functions offline. If offline packs are not present and no network exists, the OS returns `SpeechRecognizer.ERROR_NETWORK`.
- **Verification:** The engine catches `ERROR_NETWORK` and informs the user: *"Network error during speech recognition. (Offline voice may require downloaded language pack)."*
- **Status:** **IMPLEMENTED**, **BUILD-VERIFIED** (Requires downloaded OS speech packs for pure offline ASR).

### 2.3 Language & Command Pattern Coverage (English, Bangla, Banglish)
- **Source Files:** [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt), [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt), [strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values/strings.xml), [values-bn/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values-bn/strings.xml).
- **English:** Comprehensive support for app launches ("open calculator", "launch camera", "open clock"), time ("what time is it"), date ("what is today's date"), math ("calculate 45 plus 17", "12 * 8"), help, identity, stop.
- **Bangla (বাংলা Script):** Verified pattern matchers for:
  - Apps: "ক্যালকুলেটর খোলো", "ক্যামেরা খোলো", "ঘড়ি খোলো", "ক্যালেন্ডার খোলো", "সেটিংস খোলো".
  - Time & Date: "সময় কত", "কয়টা বাজে", "আজকের তারিখ কি", "আজ কত তারিখ".
  - Social & Identity: "নমস্কার", "সালাম", "কেমন আছো", "হ্যালো", "তুমি কে", "তোমার নাম".
  - Control: "সাহায্য", "সাহায্য করো", "থামো", "চুপ করো".
- **Banglish (Latin Script Phonetic):**
  - "calculator kholo", "camera kholo", "ghori kholo", "shomoy koto", "somoy koto", "koita baje", "ajker date", "tumi ke", "kemon acho", "thamo".
- **Identified Gap:** [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt) only recognizes ASCII digits (`0-9`) and English operators (`plus`, `minus`, `times`, `divided by`). It does **not** yet parse Bengali digits (`০-৯`) or Bengali operators (`যোগ`, `বিয়োগ`, `গুণ`, `ভাগ`).
- **Status:** **IMPLEMENTED**, **AUTOMATED-TESTED** (31/31 `CommandEngineTest` passed).

### 2.4 Wake-Word Detection Reality: Acoustic Neural Net vs. State Machine
- **Source Files:** [LocalWakeWordDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/LocalWakeWordDetector.kt), [WakeWordCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/WakeWordCoordinator.kt), [AcousticWakeWordEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/AcousticWakeWordEngine.kt).
- **Forensic Truth:** **There is currently NO embedded acoustic neural network running keyword spotting (KWS) on raw PCM audio.**
- **Actual Mechanism:**
  1. `AcousticWakeWordEngine.kt` is an interface specifying the future requirements for raw PCM 16kHz audio chunk processing.
  2. `LocalWakeWordDetector.kt` performs **textual string matching** on text output produced by `SpeechRecognizer`.
  3. `AssistantForegroundService.kt` runs a repeating `SpeechRecognizer` loop. When an utterance begins with "hey saarthi" or "ok saarthi", `LocalWakeWordDetector.extractWakeWordAndCommand()` strips the wake phrase and passes the remainder to `CommandEngine`.
  4. `WakeWordCoordinator.kt` is a deterministic **state machine** (IDLE → WAKE_DETECTED → LISTENING_FOR_COMMAND → AWAITING_CONFIRMATION) managing mutual exclusion with TTS playback.
- **Status:** **STATE-MACHINE IMPLEMENTED & TESTED** (20 unit tests passed), **ACOUSTIC KWS NOT IMPLEMENTED** (Deliberate design decision to preserve < 1.5 MB APK footprint without bundling a 3–5 MB TFLite binary).

### 2.5 App-Launch Intents & Honest Failure Handling
- **Source Files:** [AppLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/AppLauncher.kt), [CalculatorLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CalculatorLauncher.kt), [ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt).
- **Target Apps:** `CALCULATOR`, `CLOCK`, `CALENDAR`, `CAMERA`, `SETTINGS`.
- **Implementation:**
  - Multi-tier discovery: Canonical Android category selectors (`CATEGORY_APP_CALCULATOR`, `CATEGORY_APP_CALENDAR`), standard system intents (`AlarmClock.ACTION_SHOW_ALARMS`, `MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA`, `Settings.ACTION_SETTINGS`), vendor package tables (Samsung, Google, Xiaomi, Oppo, OnePlus, Transsion, Vivo, Motorola, Huawei, Honor, Nothing), and dynamic launcher queries (`CATEGORY_LAUNCHER`).
  - Intent verification: Calls `launchIntent.resolveActivity(packageManager)` before calling `startActivity()`.
  - Failure contracts: Returns formal `ExecutionContract.Failed(errorType, spokenFeedback, displayFeedback)` catching `ActivityNotFoundException`, `SecurityException`, and returning specific fallback advice (e.g. for missing calculator, recommends using voice math).
- **Status:** **IMPLEMENTED**, **AUTOMATED-TESTED** (7/7 `ExecutionContractTest` passed), **BUILD-VERIFIED**.

### 2.6 Required Android API Levels & Permissions
- **Source Files:** [app/build.gradle.kts](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/build.gradle.kts), [AndroidManifest.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/AndroidManifest.xml).
- `minSdk = 26` (Android 8.0 Oreo) — Ensures compatibility with 95%+ of active Android devices worldwide.
- `targetSdk = 36` | `compileSdk = 36` (Android 16).
- **Declared Manifest Permissions:**
  1. `RECORD_AUDIO`: Runtime permission for speech-to-text.
  2. `ACCESS_NETWORK_STATE`: Normal install-time permission for honest connectivity observation.
  3. `FOREGROUND_SERVICE`: Background assistant service execution.
  4. `FOREGROUND_SERVICE_MICROPHONE`: Specific foreground type permission required on Android 14+ (API 34+).
  5. `POST_NOTIFICATIONS`: Runtime permission on Android 13+ (API 33+) for persistent foreground notification.
  6. `<queries>`: Declarations for OEM calculators, clocks, calendars, cameras, and settings packages.
- **Status:** **COMPLIANT & MINIMAL**. Zero sensitive permissions like Contacts, SMS, Location, or Storage.

### 2.7 Architectural Risks & Vulnerabilities
1. **Continuous Speech Recognition Battery Drain:**
   - In [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt), looping `SpeechRecognizer.startListening()` with a 400ms restart interval causes heavy battery drain and CPU lock on entry-level devices if left running continuously.
   - *Mitigation Plan:* By default, background assistant should be off unless explicitly toggled by user, and should pause automatically when device audio hardware is occupied.
2. **Broad Exception Handling (`catch (_: Throwable)`) in Recent Hotfix:**
   - [ConnectivityObserver.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ConnectivityObserver.kt), [Theme.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/Theme.kt), and [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt) catch `Throwable` to guarantee zero startup crashes.
   - *Refinement:* Narrow down to `Exception` and specific error classes (`SecurityException`, `IllegalStateException`) with Android `Log.w()` logging.

---

## 3. Evaluation of Phase 3 Candidate Features

Each requested candidate feature was evaluated against Android OS restrictions, permissions, battery footprint, and offline feasibility:

| Feature | Technical Mechanism | Android Permissions | OS Restrictions / Caveats | Priority Recommendation |
|---|---|---|---|---|
| **1. Battery Status & Charging** | `context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` | **Zero permissions** | None. Sticky broadcast, instant synchronous read, 100% offline, zero battery cost. | **P0 (Immediate)** |
| **2. Flashlight Control** | `CameraManager.setTorchMode(id, enabled)` | **Zero permissions** (API 23+) | Check `hasSystemFeature(FEATURE_CAMERA_FLASH)`. Handled gracefully if camera is open elsewhere. | **P0 (Immediate)** |
| **3. Device Settings Shortcuts** | Official System Panels (`Settings.Panel.ACTION_WIFI`, `ACTION_VOLUME`, `ACTION_INTERNET_CONNECTIVITY`) & System Intents (`Settings.ACTION_BLUETOOTH_SETTINGS`) | **Zero permissions** | Direct programmatic toggle of Wi-Fi (`setWifiEnabled`) or Bluetooth (`BluetoothAdapter.enable`) is **strictly blocked** by Android on API 29+. System panels provide the approved native UX. | **P0 (Immediate)** |
| **4. Timer & Stopwatch** | Native Clock Intent (`AlarmClock.ACTION_SET_TIMER`) + in-app Tools tab stopwatch | `SET_ALARM` for Clock intent (optional); zero permissions for in-app stopwatch. | Handled via existing `AppLauncher` philosophy. | **P1 (High)** |
| **5. Unit & Percentage Math** | Algorithmic extension to `SafeMathParser` (metric/imperial conversion, currency units, `%` calculations, Bengali digit normalization) | **Zero permissions** | 100% offline pure Kotlin logic. Zero APK weight. | **P1 (High)** |
| **6. Local Notes & Search** | Lightweight SQLite / JSON file storage in app internal files directory (`context.filesDir`) | **Zero permissions** | Private by design. No cloud sync. | **P2 (Medium)** |
| **7. Tasks & Local Reminders** | In-app task list + AlarmManager / Calendar insert intent (`ACTION_INSERT`) | `POST_NOTIFICATIONS` (already declared) | Android 12+ restricts `SCHEDULE_EXACT_ALARM`. Prefer Calendar intent or standard alarms to avoid battery restrictions. | **P2 (Medium)** |
| **8. Bangla / Banglish Expansion** | Expansion of `CommandEngine` phrase matchers for battery, flashlight, settings, and units | **Zero permissions** | 100% offline rule matching. | **P0 (Immediate)** |

---

## 4. Testable Acceptance Criteria for Phase 3

### Feature 1: Battery Percentage & Charging Status
- **Commands:**
  - English: `"what is my battery level"`, `"battery percentage"`, `"battery status"`, `"how much battery do I have"`
  - Bangla: `"ব্যাটারি কত"`, `"ব্যাটারি পার্সেন্টেজ"`, `"চার্জ কত আছে"`
  - Banglish: `"battery koto"`, `"battery percentage koto"`, `"charge koto ache"`
- **Result:** Spoken & visual feedback: *"Your battery is at 84% and currently charging."* (Exact real device values).
- **Permissions:** None (`Intent.ACTION_BATTERY_CHANGED`).
- **Offline:** 100% on-device.
- **Tests:** Unit test with mock BatteryManager Intent; physical device verification.

### Feature 2: Flashlight Control (Torch)
- **Commands:**
  - English: `"turn on flashlight"`, `"flashlight on"`, `"turn off flashlight"`, `"torch on"`, `"torch off"`
  - Bangla: `"টর্চ জ্বালাও"`, `"টর্চ চালু করো"`, `"টর্চ বন্ধ করো"`, `"ফ্ল্যাশলাইট অন করো"`
  - Banglish: `"torch jalao"`, `"torch on karo"`, `"torch bondho koro"`, `"flashlight off karo"`
- **Result:** Toggles device camera flash LED. Returns spoken & visual feedback: *"Flashlight turned on."* / *"Flashlight turned off."*
- **Failure Behavior:** If device lacks flash hardware, reports: *"This device does not have a flashlight."*
- **Permissions:** None (API 23+ `CameraManager.setTorchMode`).
- **Offline:** 100% on-device.
- **Tests:** Unit tests with mock `CameraManager`; physical device verification.

### Feature 3: System Settings Shortcuts (Wi-Fi, Bluetooth, Volume, Display)
- **Commands:**
  - English: `"open wifi settings"`, `"open bluetooth settings"`, `"open volume settings"`, `"open display settings"`
  - Bangla: `"ওয়াইফাই সেটিংস খোলো"`, `"ব্লুটুথ সেটিংস খোলো"`, `"ভলিউম সেটিংস"`
  - Banglish: `"wifi settings kholo"`, `"bluetooth settings kholo"`, `"sound settings kholo"`
- **Result:** Dispatches approved Android System Panel or Settings Intent without requesting invasive permissions.
- **Permissions:** None.
- **Offline:** 100% on-device.
- **Tests:** Intent resolution unit tests in `AppLauncherTest`.

### Feature 4: Bengali Number & Operator Parsing in Math Solver
- **Inputs:** `"১২৫ + ৭৫"`, `"calculate ৫০ গুণ ৪"`, `"১০% of ৫০০"`, `"২৫ এর ১০ শতাংশ কত"`
- **Result:** Evaluates arithmetic accurately and formats answer into user's language.
- **Permissions:** None.
- **Offline:** 100% algorithmic.
- **Tests:** 15+ new unit tests in `SafeMathParserTest`.

---

## 5. APK Size & Performance Budget

```
+-----------------------------------------------------------------------------------------+
| Hard Size Constraint Limit: 5,000,000 bytes (5.0 MB)                                    |
| Current Measured Release APK: 1,561,849 bytes [#########...............] 31.2% Utilized |
| Available Budget Headroom:   3,438,151 bytes (68.8% Free)                               |
+-----------------------------------------------------------------------------------------+
```
- **Constraint Policy:** Phase 3 utilities (Battery, Torch, System Panels, Math Expansion) will use Android SDK platform APIs and pure Kotlin code with **0 external dependencies**.
- **Projected Net Code Growth:** Approximately +12 KB to +25 KB of compiled R8 bytecode, keeping the final APK strictly below **1.6 MB**.

---

## 6. Staged Implementation Plan for Phase 3

### Stage 3A: Device Hardware Essentials (Recommended Next Task)
1. Implement `DeviceHardwareController.kt` for battery query and flashlight toggle with hardware checks.
2. Integrate battery and flashlight intents into `CommandEngine.kt` (English, Bangla, Banglish).
3. Add Quick Action buttons for Battery and Flashlight on `HomeScreen` and `ToolsScreen`.
4. Automated unit tests for battery parser and flashlight failure contracts.

### Stage 3B: Settings Panels & System Shortcuts
1. Add supported Android System Panels (`Settings.Panel.ACTION_WIFI`, `ACTION_VOLUME`, `ACTION_INTERNET_CONNECTIVITY`) and Bluetooth Settings to `AppLauncher.kt`.
2. Map commands in `CommandEngine.kt`.

### Stage 3C: Math & Bengali Numerals Expansion
1. Implement Bengali numeral normalization (`০-৯` → `0-9`) in `SafeMathParser.kt`.
2. Add percentage calculations (`% of`) and standard unit conversions.

### Stage 3D: Local Notes & Utilities
1. Implement private, local-only JSON/SQLite note storage in app files directory.
2. In-app Timer / Stopwatch card on `ToolsScreen`.

---

## 7. Single Recommended Next Task

**Stage 3A: Implement Device Hardware Essentials (Battery Status & Flashlight Control)**  
- Adds high-value, everyday phone utility.
- Operates 100% offline with zero external dependencies and zero new permissions.
- Supported across both low-end and high-end Android phones.
- 100% testable with automated unit tests and immediate physical device verification.
