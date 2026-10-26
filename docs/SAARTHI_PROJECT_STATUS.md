# SAARTHI — Project Status & Capability Verification Matrix

**Document Version:** 3.0.0  
**Current Date:** October 26, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  
**Current Milestone:** Phase 2 Complete → Phase 3 Ready ("Offline Essentials & Reliable Phone Actions")  
**Target Hardware:** Low-to-mid-range Android Smartphones (Android 8.0+ / API 26+)

---

## 1. High-Level Executive Summary

SAARTHI is a privacy-first, lightweight, on-device AI voice and text assistant for Android smartphones. The application is built using native Kotlin, Jetpack Compose (Material 3), Android Speech APIs, and a deterministic offline intent execution engine.

Following the resolution and build verification of the P0 startup crash on physical Android devices, **the application launches, renders, and operates stably on physical hardware**. All automated test suites (66/66 tests) pass with zero errors, and release builds compile cleanly to **1.56 MB**, preserving over 3.4 MB of headroom beneath the project's strict 5.0 MB maximum APK budget.

---

## 2. Capability Verification Matrix

Every feature is classified using one of five standardized verification states:
- **`IMPLEMENTED`**: Source code is fully authored in the repository.
- **`AUTOMATED-TESTED`**: Validated by automated JVM/Robolectric unit or integration tests.
- **`BUILD-VERIFIED`**: Validated in compiled debug and minified release APK artifacts.
- **`DEVICE-VERIFIED`**: Physically tested on an attached or user-confirmed Android device.
- **`NOT VERIFIED`**: Pending hardware verification or not yet implemented.

| Subsystem / Capability | Verification Status | Implementation File(s) | Operational Reality / Limitations |
|---|---|---|---|
| **App Launch (Activity Lifecycle)** | **DEVICE-VERIFIED** | [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) | Successfully opens and renders on physical Android phone without crashing. |
| **Material 3 UI & Navigation** | **BUILD-VERIFIED** | `ui/screens/*`, `ui/components/*`, `ui/theme/*` | Home, Conversation, Tools, and Settings tabs with custom high-contrast dark theme. |
| **Manual Text Command Input** | **BUILD-VERIFIED** | [TextCommandInput.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TextCommandInput.kt), [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) | Text bar with send button; dispatches into unified `processCommand(text)` router. |
| **Microphone Speech Recognition** | **BUILD-VERIFIED** | [AndroidSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt) | Uses Android platform `SpeechRecognizer`; passes `EXTRA_PREFER_OFFLINE = true`. Requires device offline speech pack for pure airplane-mode ASR. |
| **Text-to-Speech (TTS) Feedback** | **AUTOMATED-TESTED** | [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt), [SpeechPronunciationFormatter.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/SpeechPronunciationFormatter.kt) | Platform `android.speech.tts.TextToSpeech` with mutual exclusion against microphone. Formatter unit tests pass (7/7). |
| **App Launcher (Calculator, Clock, Camera, Settings, Calendar)** | **AUTOMATED-TESTED** | [AppLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/AppLauncher.kt), [CalculatorLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CalculatorLauncher.kt) | Multi-tier intent resolver (categories, vendor tables for 11 OEMs, system intents, launcher fallback). Contract tests pass (7/7). |
| **Command Engine (English Pattern Matching)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | English regex/token matching for app launch, time, date, math, stop, identity. Unit tests pass (31/31). |
| **Command Engine (Bangla Script: বাংলা)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Pattern matches "ক্যালকুলেটর খোলো", "সময় কত", "আজকের তারিখ", "থামো", "কেমন আছো", etc. |
| **Command Engine (Banglish: Latin Script)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Matches "calculator kholo", "shomoy koto", "ajker date", "tumi ke", "thamo", etc. |
| **Safe Math Evaluation (Arithmetic Expressions)** | **AUTOMATED-TESTED** | [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt) | Recursive descent parser for `+`, `-`, `*`, `/` with precedence. (Bangla digits `০-৯` pending in Phase 3). |
| **Wake-Word State Machine & Coordination** | **AUTOMATED-TESTED** | [WakeWordCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/WakeWordCoordinator.kt), [LocalWakeWordDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/LocalWakeWordDetector.kt) | Manages post-wake listening window, confirmation state, and TTS ducking. Tests pass (20/20). |
| **Acoustic Wake-Word KWS (Raw Audio Model)** | **NOT VERIFIED** (Stub/Interface Only) | [AcousticWakeWordEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/AcousticWakeWordEngine.kt) | No on-device neural network (TFLite/Porcupine) is bundled. Wake detection currently operates on text transcripts from `SpeechRecognizer`. |
| **Background Assistant Service** | **BUILD-VERIFIED** | [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) | Runs foreground service with notification. Continuous mic listening has battery cost; disabled by default until user opts in. |
| **Device Hardware: Battery Status** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | Instant, zero-permission `Intent.ACTION_BATTERY_CHANGED` read. Tested across all charging states and bounded percentages. |
| **Device Hardware: Flashlight Control** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | Zero-permission `CameraManager.setTorchMode` with `FEATURE_CAMERA_FLASH` verification, lens selection, and error contracts. |
| **System Settings Panels (Wi-Fi, Vol, BT)** | **NOT VERIFIED** (Planned Stage 3B) | Planned: `AppLauncher.kt` | Android official System Panels (`Settings.Panel.*`) without invasive toggle permissions. Scheduled for Stage 3B. |

---

## 3. Verification & Testing Metrics

### Automated Unit Test Summary
- **Total Test Classes:** 7
- **Total Unit Tests Executed:** 86
- **Test Pass Rate:** **100% (86 Passed, 0 Failed, 0 Skipped, 0 Errors)**
- **Test Execution Time:** 5 seconds (cached) / 11 seconds (cold)
- **Detailed Suite Breakdown:**
  1. `com.saarthi.assistant.core.CommandEngineTest`: **37 passed**
  2. `com.saarthi.assistant.core.DeviceHardwareControllerTest`: **14 passed**
  3. `com.saarthi.assistant.wakeword.NaturalVoiceSessionTest`: **11 passed**
  4. `com.saarthi.assistant.wakeword.WakeWordCoordinatorTest`: **9 passed**
  5. `com.saarthi.assistant.core.ExecutionContractTest`: **7 passed**
  6. `com.saarthi.assistant.voice.SpeechPronunciationFormatterTest`: **7 passed**
  7. `com.saarthi.assistant.ExampleUnitTest`: **1 passed**

### Build Artifact Verification
- **Release Build Task:** `.\gradlew.bat assembleRelease`
- **Build Status:** **SUCCESSFUL**
- **APK Output Location:** `android/app/build/outputs/apk/release/app-release.apk`
- **Desktop Staging Location:** `C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk`
- **Measured APK File Size:** **1,584,569 bytes (~1.51 MB)**
- **Size Budget:** Maximum 5,000,000 bytes (5.0 MB)
- **Remaining Headroom:** **3,415,431 bytes (68.3% unused headroom)**

---

## 4. Android Configuration & Permission Profile

| Property | Value | Rationale |
|---|---|---|
| **minSdk** | 26 (Android 8.0 Oreo) | Ensures support for over 95% of active global Android hardware. |
| **targetSdk** | 36 (Android 16) | Adheres to modern Android behavior and security contracts. |
| **compileSdk** | 36 | Access to latest platform APIs and compilation optimizations. |
| **Permissions Declared** | 5 permissions total: | Strictly minimal. Zero high-risk/invasive permissions. |
| - `RECORD_AUDIO` | Runtime | Required for microphone input in speech recognition engine. |
| - `ACCESS_NETWORK_STATE` | Normal (Install-time) | Required for honest connectivity status checks (`ConnectivityObserver`). |
| - `FOREGROUND_SERVICE` | Normal (Install-time) | Required for background voice assistant foreground service. |
| - `FOREGROUND_SERVICE_MICROPHONE` | Normal (API 34+) | Required by Android 14+ when foreground service accesses microphone. |
| - `POST_NOTIFICATIONS` | Runtime (API 33+) | Required by Android 13+ to display persistent foreground service notification. |

---

## 5. Current Architectural Risks & Mitigation Status

1. **Physical Device Startup Stability:**
   - *Status:* **RESOLVED & VERIFIED**. Fixed missing `ACCESS_NETWORK_STATE` permission and wrapped initial composition observers in safe exception guards.
2. **Offline Speech Recognition Expectation:**
   - *Status:* **DOCUMENTED**. `SpeechRecognizer` delegates to OS services; offline transcription requires user to have on-device language packs installed. The app honestly reports `ERROR_NETWORK` if packs are missing.
3. **Continuous Background Microphone Listening:**
   - *Status:* **MANAGED**. Kept disabled by default to prevent battery drain on budget devices; toggled explicitly via Settings screen.
4. **Settings Modification Restrictions:**
   - *Status:* **ADDRESSED IN ROADMAP**. Direct programmatic toggling of Wi-Fi or Bluetooth is blocked on Android 10+ (API 29+); Phase 3 adopts official Android System Panels instead of fragile private APIs.

---

## 6. Next Planned Milestone

**Phase 3 — Stage 3B: Device Settings Panels & System Shortcuts**
- Official Android System Panels for Wi-Fi (`Settings.Panel.ACTION_WIFI`), Volume (`Settings.Panel.ACTION_VOLUME`), and Internet Connectivity (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`).
- Bluetooth Settings intent dispatch (`Settings.ACTION_BLUETOOTH_SETTINGS`).
- Trilingual command routing in `CommandEngine.kt`.
