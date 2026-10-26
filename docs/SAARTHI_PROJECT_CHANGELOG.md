# SAARTHI — Project Chronological Changelog

All notable changes and architectural milestones for **SAARTHI AI — Your Private AI Assistant** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to semantic development staging.

## [Stage 3A — Device Hardware Essentials] — 2026-10-26

### Added
- **Feature A: Battery Status & Charging Monitoring**:
  - Implemented `DeviceHardwareController.kt` with `AndroidBatteryInfoProvider` reading `Intent.ACTION_BATTERY_CHANGED` broadcast and API 21+ property fallback with **0 permissions**.
  - Structured `BatteryStatusResult` reporting bounded percentage (0..100) and charging states (`CHARGING`, `DISCHARGING`, `FULL`, `NOT_CHARGING`, `UNKNOWN`) and plugged sources (`AC`, `USB`, `WIRELESS`).
  - Added full trilingual command routing in `CommandEngine.kt` for English, Bangla (বাংলা), and Banglish.
- **Feature B: Flashlight Control (Torch)**:
  - Implemented `AndroidFlashlightController` using Android `CameraManager.setTorchMode` with `FEATURE_CAMERA_FLASH` hardware verification and lens detection.
  - Zero runtime camera permissions required (API 26+ compliant; does not open Camera app).
  - Accurate state tracking using `CameraManager.TorchCallback`.
  - Defensive error contracts handling `CameraAccessException` reasons (`HARDWARE_BUSY`, `SECURITY_RESTRICTION`) and platform exceptions without broad `catch (Throwable)`.
  - Enforced clarification on ambiguous single-word inputs ("flashlight", "torch", "টর্চ", "ফ্ল্যাশলাইট").
- **UI Integration**:
  - Created `DeviceHardwareCard.kt` providing `DeviceHardwareSummaryRow` on `HomeScreen` and `HardwareControlsCard` on `ToolsScreen` with explicit ON and OFF button chips and live percentage/charging indicators.
  - Connected `Lifecycle.Event.ON_RESUME` in `MainActivity.kt` ensuring state refreshes automatically upon returning to the foreground.
  - Added custom vector icons: `Battery`, `BatteryCharging`, `FlashlightOn`, `FlashlightOff` in `SaarthiIcons.kt`.
- **Automated Testing Suite**:
  - Created `DeviceHardwareControllerTest.kt` with 14 unit tests covering percentage computation, clamped bounds, charging-state mappings, hardware-absent failure contracts, and camera-busy handling using dependency-injected fake controllers.
  - Added 6 new test methods to `CommandEngineTest.kt` validating 60+ English, Bangla, and Banglish battery and flashlight command variants.

### Fixed
- **Bengali Dari (`।`) Normalization**:
  - Enhanced `CommandEngine.kt`'s `trimEnd` to strip the Bengali full stop / dari (`।` / U+0964) alongside ASCII punctuation, ensuring queries like `"ফ্ল্যাশলাইট জ্বালাও।"` route correctly.

### Verified
- Automated test suite: **86/86 unit tests passed** across all 7 test classes (0 failures, 0 skipped, 5s execution).
- Release APK build: `assembleRelease` succeeded (1m 34s); generated APK size **1,584,569 bytes (~1.51 MB)** against 5.0 MB limit (3.41 MB headroom).
- Desktop staged APK: updated `C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk`.

---

## [Phase 3 Audit] — 2026-10-26

### Added
- **Phase 3 Readiness Audit & Implementation Plan (`docs/SAARTHI_PHASE3_READINESS_AUDIT.md`)**:
  - Comprehensive source code forensic audit answering 7 foundational questions on speech modality, offline capability, language coverage, wake-word reality, app launches, permissions, and battery risks.
  - Rigorous evaluation of Phase 3 candidate features (Battery, Flashlight, Settings panels, Math expansion, Local Notes, Reminders) detailing Android restrictions, offline feasibility, permissions, and priorities.
  - Acceptance criteria and automated test strategies for Phase 3 features.
  - APK size analysis confirming 1.56 MB release build against 5.0 MB hard constraint limit (3.44 MB headroom).
  - Staged roadmap prioritizing Stage 3A: Device Hardware Essentials (Battery & Flashlight).
- **Central Project Documentation**:
  - `docs/SAARTHI_PROJECT_STATUS.md`: Full verification matrix covering all modules with explicit `IMPLEMENTED`, `AUTOMATED-TESTED`, `BUILD-VERIFIED`, `DEVICE-VERIFIED`, and `NOT VERIFIED` statuses.
  - `docs/SAARTHI_PROJECT_CHANGELOG.md`: Chronological milestone ledger.
  - `docs/SAARTHI_FILE_CHANGE_INDEX.md`: Repository file taxonomy and module index.

### Verified
- Automated test suite: 66/66 unit tests passed (0 failures, 0 skipped, 16s cold / 1s cached).
- Release APK compilation: `assembleRelease` succeeded in 59s; release APK size 1,561,849 bytes (~1.49 MB).
- Release APK copied to developer Desktop: `C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk`.

---

## [P0 Startup Crash Hotfix] — 2026-10-26

### Fixed
- **P0 Crash on Launch on Physical Android Devices**:
  - Root Cause: Missing `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` in `AndroidManifest.xml` caused `ConnectivityManager.getActiveNetwork()` to throw unhandled `SecurityException` during initial composition in `rememberNetworkStatus()`.
  - Resolution in [AndroidManifest.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/AndroidManifest.xml): Declared `ACCESS_NETWORK_STATE`.
  - Resolution in [ConnectivityObserver.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ConnectivityObserver.kt): Wrapped network checks and callback registrations in multi-layer `try-catch` blocks, safely defaulting to `NetworkStatus.OFFLINE` if permissions or system services are unavailable.
- **Window Inset Manipulation Crash in Jetpack Compose**:
  - Root Cause: `WindowCompat.getInsetsController(window, view)` inside `SideEffect` in [Theme.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/Theme.kt) attempted to manipulate system bars before decor view was attached.
  - Resolution: Wrapped status/navigation bar controller calls in defensive `try-catch` guards.
- **TTS & History Manager Defensive Hardening**:
  - Added safety guards in [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt) and [HistoryManager.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HistoryManager.kt) to guarantee zero uncaught exceptions can escape to kill the application process during startup.
- **Syntax and Type Fixes in Compose UI**:
  - Fixed SVG path syntax error in [SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/SaarthiIcons.kt) (`lineToRelative(-9f, -9f)`).
  - Resolved `userFeedback` property resolution in [ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt).
  - Corrected `ExecutionContract` pattern matching in [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt).

### Documented
- `docs/SAARTHI_STARTUP_CRASH_INVESTIGATION.md`: Complete root cause analysis, static execution path trace, fix verification, and physical launch checklist.

---

## [UI/UX Redesign — Phase 2] — 2026-10-25

### Added
- **Modern Jetpack Compose Material 3 Dark UI**:
  - Premium deep slate and dark amber aesthetic system designed specifically for OLED/AMOLED battery conservation.
  - Custom vector iconography system ([SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/SaarthiIcons.kt)) eliminating reliance on heavy external icon libraries.
  - Four dedicated primary screens:
    - **HomeScreen**: Hero voice orb, quick action cards, natural voice session card, real-time status chips.
    - **ConversationScreen**: Message bubble transcript with copy action, audio replay, and timestamp indicators.
    - **ToolsScreen**: On-device math solver, system app launch matrix, offline connectivity visualizer.
    - **SettingsScreen**: Background voice listening toggle, wake-word sensitivity, voice pitch/speed sliders, privacy indicators.
- **Manual Text Command Input**:
  - Implemented [TextCommandInput.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TextCommandInput.kt) with send button, IME action listener, and character counter.
  - Unified routing into `processCommand(text)`.

---

## [Phase 2 — Natural Voice & Background Coordination] — 2026-10-24

### Added
- **Natural Voice Interaction & Mutual Exclusion**:
  - [NaturalVoiceSession.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/NaturalVoiceSession.kt): Coordinated bidirectional conversation loops.
  - [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt): Microphone muting during speech synthesis to prevent audio feedback loops.
  - [SpeechPronunciationFormatter.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/SpeechPronunciationFormatter.kt): Text normalization for clear phonetics.
- **Wake-Word State Machine & Coordination**:
  - [WakeWordCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/WakeWordCoordinator.kt): Multi-state coordinator (IDLE, WAKE_DETECTED, LISTENING_FOR_COMMAND, AWAITING_CONFIRMATION).
  - [LocalWakeWordDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/LocalWakeWordDetector.kt): Textual phrase extractor for "hey saarthi" / "ok saarthi".
  - [AcousticWakeWordEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/AcousticWakeWordEngine.kt): Architectural interface for future raw PCM keyword spotter.
- **Background Assistant Service**:
  - [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt): Foreground service with persistent notification and microphone type declaration.
- **Automated Testing Suite**:
  - Added unit test suites for `WakeWordCoordinatorTest`, `NaturalVoiceSessionTest`, `SpeechPronunciationFormatterTest`, and `ExecutionContractTest`.

---

## [Phase 1 — Core Offline Foundation] — 2026-10-23

### Added
- **Core Intent Routing Engine**:
  - [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt): Regex/token-based offline intent parser supporting English, Bangla, and Banglish.
  - [ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt): Sealed class hierarchy enforcing explicit success/failure handling.
- **App Launchers**:
  - [AppLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/AppLauncher.kt): Vendor package tables (Samsung, Xiaomi, Oppo, etc.) and category fallback for Camera, Clock, Calendar, Settings.
  - [CalculatorLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CalculatorLauncher.kt): Multi-tier calculator resolver.
- **Safe Math Evaluation**:
  - [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt): Pure algorithmic recursive descent arithmetic parser.
- **Speech Recognition & TTS**:
  - [AndroidSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt): Platform `SpeechRecognizer` integration with `EXTRA_PREFER_OFFLINE` hint.
