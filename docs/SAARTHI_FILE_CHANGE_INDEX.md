# SAARTHI — Repository File Taxonomy & Change Index

**Document Version:** 3.2.0  
**Current Date:** October 26, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  

This index provides a complete, authoritative mapping of all active source files, resources, configuration files, and documentation in the SAARTHI codebase.

---

## 1. Application Layer & UI (`com.saarthi.assistant`)

| File Path | Module / Layer | Primary Responsibility | Verification Status |
|---|---|---|---|
| [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) | App Entrypoint | Activity lifecycle, permission requests (`RECORD_AUDIO`), service binding, navigation state, timer snapshot observer, direct settings launcher callbacks, `Lifecycle.Event.ON_RESUME` hardware state refresh, and unified `processCommand(text)` router. | **DEVICE-VERIFIED** |
| [ui/screens/HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt) | Presentation | Primary dashboard with pulsing voice orb, active Timer card banner, battery & flashlight quick status tiles, quick app action cards, and natural voice interaction card. | **BUILD-VERIFIED** |
| [ui/screens/ConversationScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ConversationScreen.kt) | Presentation | Message history stream displaying user commands and assistant responses with timestamps, copy actions, and TTS replay. | **BUILD-VERIFIED** |
| [ui/screens/ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt) | Presentation | Dedicated utilities screen with active Timer card, System Settings shortcuts card, Help & Commands accordion, hardware controls card (explicit Flashlight ON/OFF, Battery status), on-device math solver, and app launcher grid. | **BUILD-VERIFIED** |
| [ui/screens/SettingsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/SettingsScreen.kt) | Presentation | Assistant preferences (background listening toggle, wake sensitivity, speech rate/pitch sliders, and privacy assurances). | **BUILD-VERIFIED** |
| [ui/components/DeviceHardwareCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/DeviceHardwareCard.kt) | Presentation | Accessible battery and flashlight status indicators (`DeviceHardwareSummaryRow` for Home, `HardwareControlsCard` for Tools with explicit ON/OFF chips). | **BUILD-VERIFIED** |
| [ui/components/SystemSettingsCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SystemSettingsCard.kt) | Presentation | Grid of safe one-tap system settings shortcuts (Wi-Fi, Bluetooth, Mobile Data, Sound, Brightness). | **BUILD-VERIFIED** |
| [ui/components/TimerCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt) | Presentation | Reusable countdown timer card with circular progress, MM:SS display, pause/resume/cancel controls, and 1m/3m/5m duration presets. | **BUILD-VERIFIED** |
| [ui/components/HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt) | Presentation | Expandable Help accordion displaying all 10 capability categories with clickable prompt chips in English, Bangla, and Banglish. | **BUILD-VERIFIED** |
| [ui/components/TextCommandInput.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TextCommandInput.kt) | Presentation | Accessible text input field with send icon, IME search action, character counter, and keyboard dismissal. | **BUILD-VERIFIED** |
| [ui/components/VoicePulseVisualizer.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/VoicePulseVisualizer.kt) | Presentation | Dynamic multi-ring pulsing animation reacting to listening, processing, and speaking voice states. | **BUILD-VERIFIED** |
| [ui/theme/Color.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/Color.kt) | Design System | High-contrast dark theme color tokens (deep slate background, dark amber accent, container surfaces). | **BUILD-VERIFIED** |
| [ui/theme/Type.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/Type.kt) | Design System | Material 3 typography definitions utilizing clean system fonts for readability on low-DPI displays. | **BUILD-VERIFIED** |
| [ui/theme/Theme.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/theme/Theme.kt) | Design System | Compose theme wrapper applying custom color scheme and safe window insets controller guards. | **DEVICE-VERIFIED** |
| [ui/components/SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt) | Design System | Custom vector icons: Mic, Send, Calculator, Settings, Camera, Clock, Calendar, Check, Copy, History, Battery, BatteryCharging, FlashlightOn, FlashlightOff, Wifi, Bluetooth, MobileData, Brightness, Timer, Help, Play, Pause. | **BUILD-VERIFIED** |

---

## 2. Core Execution Engine (`com.saarthi.assistant.core`)

| File Path | Module / Layer | Primary Responsibility | Verification Status |
|---|---|---|---|
| [SystemSettingsController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt) | Core / Settings | Safe launcher for Wi-Fi, Bluetooth, Mobile Data, Sound, and Brightness using Android panels (`Settings.Panel.*`) on API 29+ and standard settings intents. Decoupled with `SystemSettingsLauncher` interface. | **AUTOMATED-TESTED** (13/13 unit tests) |
| [TimerController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt) | Core / Timer | Local countdown timer engine on `Dispatchers.Default` with state machine, debounced duplicate protection, local notifications, and injectable `TimeProvider`. | **AUTOMATED-TESTED** (19/19 unit tests) |
| [HelpRegistry.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt) | Core / Help | Single source of truth for all 10 assistant capabilities with authentic English, Bangla, and Banglish examples and topic-level matcher. | **AUTOMATED-TESTED** |
| [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | Core / Hardware | Offline battery status provider (`ACTION_BATTERY_CHANGED`) and safe `CameraManager` flashlight controller with real-time `TorchCallback` tracking and dependency injection hooks. | **AUTOMATED-TESTED** (14/14 unit tests) |
| [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Core / NLP | Regex and token-based intent parser supporting English, Bangla (বাংলা), and Banglish for settings shortcuts, timer, battery, flashlight, app launch, math, time, date, and help. Supports Bengali numerals `০-৯`. | **AUTOMATED-TESTED** (50/50 unit tests) |
| [AppLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/AppLauncher.kt) | Core / System | Multi-tier app launcher resolving OEM package tables (Samsung, Xiaomi, Oppo, etc.), category selectors, and system intents. | **AUTOMATED-TESTED** |
| [CalculatorLauncher.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CalculatorLauncher.kt) | Core / System | Specialized calculator launcher with explicit `ExecutionContract` returns and voice math fallback advice. | **AUTOMATED-TESTED** (7/7 unit tests) |
| [ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt) | Core / Contract | Sealed class hierarchy (`ExecutionContract.Succeeded`, `ExecutionContract.Failed`) enforcing structured results and error reporting (including `HARDWARE_UNAVAILABLE`, `HARDWARE_BUSY`, `LAUNCH_FAILED`). | **AUTOMATED-TESTED** |
| [SafeMathParser.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SafeMathParser.kt) | Core / Math | Recursive descent math expression evaluator with operator precedence, division-by-zero protection, and localized output formatting. | **AUTOMATED-TESTED** |
| [ConnectivityObserver.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ConnectivityObserver.kt) | Core / Network | Honest on-device network observer tracking online/offline transitions with multi-tier `SecurityException` protection. | **DEVICE-VERIFIED** |
| [HistoryManager.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HistoryManager.kt) | Core / Data | SharedPreferences-backed FIFO message history manager storing up to 50 conversations locally. | **BUILD-VERIFIED** |

---

## 3. Voice & Speech Subsystems (`com.saarthi.assistant.voice`)

| File Path | Module / Layer | Primary Responsibility | Verification Status |
|---|---|---|---|
| [AndroidSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt) | Voice / ASR | Android platform `SpeechRecognizer` wrapper passing `EXTRA_PREFER_OFFLINE = true` with speech state callbacks. | **BUILD-VERIFIED** |
| [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt) | Voice / TTS | Android platform `TextToSpeech` engine with audio ducking and completion listeners. Hardened against uninitialized speech calls. | **AUTOMATED-TESTED** |
| [NaturalVoiceSession.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/NaturalVoiceSession.kt) | Voice / Flow | Bidirectional conversational loop manager coordinating listen → recognize → parse → speak cycles. | **AUTOMATED-TESTED** (11/11 unit tests) |
| [SpeechPronunciationFormatter.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/SpeechPronunciationFormatter.kt) | Voice / Text | Text normalization utility ensuring natural TTS pronunciation of abbreviations, times, and math symbols. | **AUTOMATED-TESTED** (7/7 unit tests) |

---

## 4. Wake-Word & Background Subsystems (`com.saarthi.assistant.wakeword`, `.service`)

| File Path | Module / Layer | Primary Responsibility | Verification Status |
|---|---|---|---|
| [WakeWordCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/WakeWordCoordinator.kt) | Wake Word / State | Deterministic state machine (IDLE, WAKE_DETECTED, LISTENING_FOR_COMMAND, AWAITING_CONFIRMATION). | **AUTOMATED-TESTED** (9/9 unit tests) |
| [LocalWakeWordDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/LocalWakeWordDetector.kt) | Wake Word / Match | Textual regex phrase detector identifying "hey saarthi" / "ok saarthi" and extracting remainder commands. | **AUTOMATED-TESTED** |
| [AcousticWakeWordEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/wakeword/AcousticWakeWordEngine.kt) | Wake Word / KWS | Clean architectural interface defining raw PCM 16kHz audio stream inference for future acoustic models. | **NOT VERIFIED** (Interface only) |
| [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) | Service / OS | Android foreground service with persistent notification, background command execution (battery, flashlight, settings, timer), and microphone type declaration. | **BUILD-VERIFIED** |

---

## 5. Build, Manifest, & Android Configuration

| File Path | Module / Layer | Primary Responsibility | Verification Status |
|---|---|---|---|
| [android/app/build.gradle.kts](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/build.gradle.kts) | Build Config | Min SDK 26, Target/Compile SDK 36, R8 code/resource shrinking, Compose compiler config, zero external AI dependencies. | **BUILD-VERIFIED** |
| [android/app/src/main/AndroidManifest.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/AndroidManifest.xml) | Manifest | Declares permissions (`RECORD_AUDIO`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`) and `<queries>` tags. Zero invasive permissions. | **DEVICE-VERIFIED** |
| [android/app/src/main/res/values/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values/strings.xml) | Resources | English localized UI, speech strings, timer controls, settings shortcuts, and help titles. | **BUILD-VERIFIED** |
| [android/app/src/main/res/values-bn/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values-bn/strings.xml) | Resources | Bangla (বাংলা) localized UI, speech strings, timer controls, settings shortcuts, and help titles. | **BUILD-VERIFIED** |

---

## 6. Automated Test Suites (`android/app/src/test/java/com/saarthi/assistant`)

| File Path | Test Target | Tests Passed | Verification Status |
|---|---|---|---|
| [CommandEngineTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt) | `CommandEngine.kt`, Settings shortcuts, Timers, Bengali digits, Battery, Flashlight, Calculator | 50 / 50 | **AUTOMATED-TESTED** |
| [TimerControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/TimerControllerTest.kt) | `TimerController.kt` countdown, states, debouncing, bounds | 19 / 19 | **AUTOMATED-TESTED** |
| [DeviceHardwareControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/DeviceHardwareControllerTest.kt) | `DeviceHardwareController.kt` battery calculations, states, camera fallback | 14 / 14 | **AUTOMATED-TESTED** |
| [SystemSettingsControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/SystemSettingsControllerTest.kt) | `SystemSettingsController.kt` launchers, execution contracts, intent mappings | 13 / 13 | **AUTOMATED-TESTED** |
| [NaturalVoiceSessionTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/voice/NaturalVoiceSessionTest.kt) | `NaturalVoiceSession.kt` | 11 / 11 | **AUTOMATED-TESTED** |
| [WakeWordCoordinatorTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/wakeword/WakeWordCoordinatorTest.kt) | `WakeWordCoordinator.kt` | 9 / 9 | **AUTOMATED-TESTED** |
| [ExecutionContractTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/ExecutionContractTest.kt) | `ExecutionContract.kt`, `CalculatorLauncher.kt` | 7 / 7 | **AUTOMATED-TESTED** |
| [SpeechPronunciationFormatterTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/voice/SpeechPronunciationFormatterTest.kt) | `SpeechPronunciationFormatter.kt` | 7 / 7 | **AUTOMATED-TESTED** |
| [ExampleUnitTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/ExampleUnitTest.kt) | JVM sanity test | 1 / 1 | **AUTOMATED-TESTED** |
| **Total Automated Tests** | **All 9 Test Suites** | **131 / 131 (100% Passed)** | **AUTOMATED-TESTED** |

---

## 7. Project Documentation (`docs/`)

| File Path | Description |
|---|---|
| [docs/SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md) | Stage 3B implementation report detailing root cause analysis for Help integration, system settings shortcuts, timer engine, test results, release APK size, and physical testing checklist. |
| [docs/SAARTHI_STAGE3A_IMPLEMENTATION_REPORT.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_STAGE3A_IMPLEMENTATION_REPORT.md) | Stage 3A implementation report for offline battery status and flashlight hardware control, test outcomes, release build verification, and manual phone testing guide. |
| [docs/SAARTHI_PHASE3_READINESS_AUDIT.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_PHASE3_READINESS_AUDIT.md) | Authoritative source forensic audit, candidate feature evaluation, and staged Phase 3 plan. |
| [docs/SAARTHI_PROJECT_STATUS.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_PROJECT_STATUS.md) | Central verification matrix, test metrics, APK size budget, and security posture. |
| [docs/SAARTHI_PROJECT_CHANGELOG.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_PROJECT_CHANGELOG.md) | Chronological development and release history across all phases, stages, and hotfixes. |
| [docs/SAARTHI_FILE_CHANGE_INDEX.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_FILE_CHANGE_INDEX.md) | Repository file taxonomy and module index (this document). |
| [docs/SAARTHI_STARTUP_CRASH_INVESTIGATION.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_STARTUP_CRASH_INVESTIGATION.md) | Root cause analysis and resolution of P0 physical launch crash (`SecurityException`). |
| [docs/SAARTHI_PHASE2_ARCHITECTURE_AUDIT.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/SAARTHI_PHASE2_ARCHITECTURE_AUDIT.md) | Phase 2 architectural audit evaluating background service, wake word, and intent routing. |
| [docs/PROJECT_TECHNICAL_DOCUMENTATION.md](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/docs/PROJECT_TECHNICAL_DOCUMENTATION.md) | Master technical architectural documentation. |

