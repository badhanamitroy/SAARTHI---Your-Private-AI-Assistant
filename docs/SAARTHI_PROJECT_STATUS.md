# SAARTHI — Project Status & Capability Verification Matrix

**Document Version:** 3.1.0  
**Current Date:** October 26, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  
**Current Milestone:** Stage 3B Complete ("System Controls, Timer & Help Integration") → Stage 3C Ready  
**Target Hardware:** Low-to-mid-range Android Smartphones (Android 8.0+ / API 26+)

---

## 1. High-Level Executive Summary

SAARTHI is a privacy-first, lightweight, on-device AI voice and text assistant for Android smartphones. The application is built using native Kotlin, Jetpack Compose (Material 3), Android Speech APIs, and a deterministic offline intent execution engine.

Following the implementation of Stage 3B, SAARTHI provides safe system settings shortcuts (Wi-Fi, Bluetooth, Mobile Data, Sound, Brightness), an in-app countdown timer with Compose UI and local notifications, and a centralized, trilingual Help Registry. All automated test suites (131/131 tests) pass with 100% success rate, and release builds compile cleanly to **1,628,313 bytes**, preserving over 3.37 MB of headroom beneath the project's strict 5.0 MB maximum APK budget.

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
| **Command Engine (English Pattern Matching)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | English regex/token matching for app launch, time, date, math, battery, flashlight, settings, timer, help. Unit tests pass (50/50). |
| **Command Engine (Bangla Script: বাংলা)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Pattern matches "ক্যালকুলেটর খোলো", "সময় কত", "ব্যাটারি কত", "ফ্ল্যাশলাইট জ্বালাও", "টাইমার দাও", "সেটিংস খোলো", Bengali digits `০-৯`. |
| **Command Engine (Banglish: Latin Script)** | **AUTOMATED-TESTED** | [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Matches "calculator kholo", "shomoy koto", "battery koto", "torch on koro", "2 minute timer dao", "wifi settings kholo". |
| **Safe Math Evaluation (Arithmetic Expressions)** | **AUTOMATED-TESTED** | [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt) | Recursive descent parser for `+`, `-`, `*`, `/` with precedence. |
| **Wake-Word State Machine & Coordination** | **AUTOMATED-TESTED** | [WakeWordCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/WakeWordCoordinator.kt), [LocalWakeWordDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/LocalWakeWordDetector.kt) | Manages post-wake listening window, confirmation state, and TTS ducking. Tests pass (20/20). |
| **Acoustic Wake-Word KWS (Raw Audio Model)** | **NOT VERIFIED** (Stub/Interface Only) | [AcousticWakeWordEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/AcousticWakeWordEngine.kt) | No on-device neural network (TFLite/Porcupine) is bundled. Wake detection currently operates on text transcripts from `SpeechRecognizer`. |
| **Background Assistant Service** | **BUILD-VERIFIED** | [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) | Runs foreground service with notification. Handles all Stage 1, 2, 3A, and 3B intents. |
| **Device Hardware: Battery Status** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | Instant, zero-permission `Intent.ACTION_BATTERY_CHANGED` read. Tested across all charging states and bounded percentages. Physical device verification pending. |
| **Device Hardware: Flashlight Control** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | Zero-permission `CameraManager.setTorchMode` with `FEATURE_CAMERA_FLASH` verification, lens selection, and error contracts. Physical device verification pending. |
| **System Settings Panels (Wi-Fi, BT, Data, Sound, Brightness)** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [SystemSettingsController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt) | Official Android System Panels (`Settings.Panel.*` on API 29+) and standard Settings intents. Direct toggle requests explain platform security policies. Tests pass (13/13). |
| **Countdown Timer Engine & UI** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [TimerController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt), [TimerCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt) | Countdown timer engine with pause, resume, cancel, status, duplicate debouncing, and local notifications. Tests pass (19/19). |
| **Centralized Help Registry & Discovery** | **AUTOMATED-TESTED**<br>**BUILD-VERIFIED** | [HelpRegistry.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt), [HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt) | 10 capability topics with English, Bangla, and Banglish examples. Expandable accordion in Tools and command parser integration. |

---

## 3. Verification & Testing Metrics

### Automated Unit Test Summary
- **Total Test Classes:** 9
- **Total Unit Tests Executed:** 131
- **Test Pass Rate:** **100% (131 Passed, 0 Failed, 0 Skipped, 0 Errors)**
- **Test Execution Time:** 2.396s
- **Detailed Suite Breakdown:**
  1. `com.saarthi.assistant.core.CommandEngineTest`: **50 passed**
  2. `com.saarthi.assistant.core.TimerControllerTest`: **19 passed**
  3. `com.saarthi.assistant.core.DeviceHardwareControllerTest`: **14 passed**
  4. `com.saarthi.assistant.core.SystemSettingsControllerTest`: **13 passed**
  5. `com.saarthi.assistant.wakeword.NaturalVoiceSessionTest`: **11 passed**
  6. `com.saarthi.assistant.wakeword.WakeWordCoordinatorTest`: **9 passed**
  7. `com.saarthi.assistant.core.ExecutionContractTest`: **7 passed**
  8. `com.saarthi.assistant.voice.SpeechPronunciationFormatterTest`: **7 passed**
  9. `com.saarthi.assistant.ExampleUnitTest`: **1 passed**

### Build Artifact Verification
- **Release Build Task:** `.\gradlew.bat assembleRelease`
- **Build Status:** **SUCCESSFUL**
- **APK Output Location:** `android/app/build/outputs/apk/release/app-release.apk`
- **Measured APK File Size:** **1,628,313 bytes (~1.55 MB)**
- **Size Baseline (Stage 3A):** 1,584,569 bytes
- **Net Delta:** **+43,744 bytes (+2.76%)**
- **Size Budget:** Maximum 5,000,000 bytes (5.0 MB)
- **Remaining Headroom:** **3,371,687 bytes (67.43% unused headroom)**
- **SHA-256 Checksum:** `FC9288B21F023D85CCBEA8086E45AEAC8847AEFA17D439A679FC0E3491180C5B`

---

## 4. Android Configuration & Permission Profile

| Property | Value | Rationale |
|---|---|---|
| **minSdk** | 26 (Android 8.0 Oreo) | Ensures support for over 95% of active global Android hardware. |
| **targetSdk** | 36 (Android 16) | Adheres to modern Android behavior and security contracts. |
| **compileSdk** | 36 | Access to latest platform APIs and compilation optimizations. |
| **Permissions Declared** | 5 permissions total: | Strictly minimal. Zero high-risk/invasive permissions. (0 added in Stage 3B). |
| - `RECORD_AUDIO` | Runtime | Required for microphone input in speech recognition engine. |
| - `ACCESS_NETWORK_STATE` | Normal (Install-time) | Required for honest connectivity status checks (`ConnectivityObserver`). |
| - `FOREGROUND_SERVICE` | Normal (Install-time) | Required for background voice assistant foreground service. |
| - `FOREGROUND_SERVICE_MICROPHONE` | Normal (API 34+) | Required by Android 14+ when foreground service accesses microphone. |
| - `POST_NOTIFICATIONS` | Runtime (API 33+) | Required by Android 13+ to display persistent foreground service notification & timer alerts. |

---

## 5. Current Architectural Risks & Mitigation Status

1. **Physical Device Startup Stability:**
   - *Status:* **RESOLVED & VERIFIED**. Fixed missing `ACCESS_NETWORK_STATE` permission and wrapped initial composition observers in safe exception guards.
2. **Offline Speech Recognition Expectation:**
   - *Status:* **DOCUMENTED**. `SpeechRecognizer` delegates to OS services; offline transcription requires user to have on-device language packs installed. The app honestly reports `ERROR_NETWORK` if packs are missing.
3. **Continuous Background Microphone Listening:**
   - *Status:* **MANAGED**. Kept disabled by default to prevent battery drain on budget devices; toggled explicitly via Settings screen.
4. **Settings Modification Restrictions:**
   - *Status:* **RESOLVED IN STAGE 3B**. Adopted official Android System Panels (`Settings.Panel.*` on API 29+) and standard Settings intents. Ambiguous direct toggle requests clarify platform security constraints and offer safe panel opening.
5. **Background Timer Throttling on Aggressive OEM ROMs:**
   - *Status:* **DOCUMENTED**. Android background process limits and OEM task killers (Xiaomi/Samsung) may pause coroutine timers if swiped from recents; foreground service and in-app countdown provide reliable operation.

---

## 6. Next Planned Milestone

**Phase 3 — Stage 3C: Calendar / Alarms & Contextual Integrations**
- Integrate Clock / Alarms intents via standard Android provider contracts.
- Calendar event creation and date navigation shortcuts.
- User feedback loop on physical hardware testing of Stage 3A & Stage 3B features.
