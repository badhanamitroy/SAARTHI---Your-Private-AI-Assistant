# SAARTHI — Stage 3B: System Controls, Timer & Help Integration Implementation Report

**Document Version:** 1.0.0  
**Date:** October 26, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  
**Milestone:** Stage 3B ("System Controls, Timer & Help Integration")  
**Execution Status:** FULLY IMPLEMENTED, AUTOMATED-TESTED, BUILD-VERIFIED  
**Device Verification Status:** NOT VERIFIED (No physical device attached or tested during this development cycle)

---

## 1. Executive Summary & Objective

Stage 3B extends SAARTHI's local, privacy-first assistant capabilities with safe system shortcuts, an in-app timer, unified help discovery, and verified routing for Stage 3A hardware features (Battery & Flashlight).

### Core Objectives Delivered:
1. **Root-Cause Resolution for Battery & Flashlight in Help:** Diagnosed and corrected why Battery and Flashlight were previously absent from the user-facing Help flow.
2. **Feature A — Centralized Help Registry:** Integrated an expandable, discoverable Help directory in the UI and command engine covering all 10 local capabilities with realistic English, Bangla, and Banglish examples.
3. **Feature B — Safe System Settings Shortcuts:** Built safe, honest system shortcuts for Wi-Fi, Bluetooth, Mobile Network, Sound/Volume, and Brightness using Android panels (`Settings.Panel.*`) on API 29+ and standard settings intents. Zero invasive permissions requested.
4. **Feature C — Practical Local Timer:** Built a high-precision countdown timer engine with start, pause, resume, cancel, status query, and debounced commands, coupled with Compose UI representation and local notifications.
5. **Feature D — Trilingual Command Routing & Contracts:** Added robust command parsing and execution contracts across voice and manual text input with ambiguity detection for direct toggle requests and duration-free timer requests.
6. **Feature E — UI Integration:** Seamlessly incorporated the active Timer card, System Settings card, and expandable Help accordion into `HomeScreen` and `ToolsScreen`.
7. **Quality & Performance:** 131 automated unit tests passing (100%), both Debug and Release APKs cleanly assembled, and Release APK size is **1,628,313 bytes** (well below the 5,000,000-byte budget).

---

## 2. Mandatory Pre-Implementation Audit & Root-Cause Analysis

### 2.1 Audit of Inspected Codebase
The following files were inspected prior to changes:
- `docs/SAARTHI_PROJECT_STATUS.md`
- `docs/SAARTHI_PROJECT_CHANGELOG.md`
- `docs/SAARTHI_FILE_CHANGE_INDEX.md`
- `docs/SAARTHI_STAGE3A_IMPLEMENTATION_REPORT.md`
- Core source files: [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt), [IntentType.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt), [CommandResult.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt), [ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt), [DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt), [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt), [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt), [HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt), [ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt).

### 2.2 Root-Cause of Missing Battery & Flashlight in Help
During Stage 3A, the `HelpRegistry.kt` file had been partially authored with entries for `BATTERY` and `FLASHLIGHT`. However, they were completely absent from user interactions for two distinct root causes:
1. **Disconnected Command Routing in `CommandEngine.kt`:**
   In `CommandEngine.detectIntent()`, whenever a user spoke or typed `"help"`, `"ki korte paro"`, or `"সাহায্য"`, the engine routed to `IntentType.HELP` with a hardcoded static string that only listed calculator, date/time, app launching, identity, and conversation controls. It never queried `HelpRegistry.getAllHelpTopics()` or `HelpRegistry.getTopicHelp()`.
2. **Missing UI Exposure in Screens:**
   Neither `HomeScreen.kt` nor `ToolsScreen.kt` included any UI component rendering `HelpRegistry`. Users had no visual catalog to browse what commands SAARTHI could perform.

### 2.3 The Fix Applied
- Refactored `CommandEngine.detectIntent()` so that general help commands return `CommandResult.ShowHelp(HelpRegistry.formatGeneralHelp())` and topic-specific queries (e.g., `"help battery"`, `"help flashlight"`, `"help timer"`, `"help settings"`) query `HelpRegistry.getTopicHelp(topic)`.
- Replaced hardcoded help text with centralized, single-source-of-truth entries in `HelpRegistry.kt`.
- Created [HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt), an expandable accordion UI embedded directly in `ToolsScreen`, providing instant access to all capabilities with clickable prompt chips.

---

## 3. Technical Implementation Details

### 3.1 Feature A: Centralized Help Registry & Discovery
- **Single Source of Truth:** [HelpRegistry.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt) contains 10 structured capability entries:
  1. `BATTERY`: Battery status & charging
  2. `FLASHLIGHT`: Flashlight ON/OFF
  3. `SETTINGS_WIFI`: Wi-Fi settings panel
  4. `SETTINGS_BLUETOOTH`: Bluetooth settings panel
  5. `SETTINGS_DATA`: Mobile data settings
  6. `SETTINGS_SOUND`: Sound & volume panel
  7. `SETTINGS_BRIGHTNESS`: Display brightness settings
  8. `TIMER`: Countdown timer (set, pause, resume, cancel, status)
  9. `CALCULATOR`: Offline math solver
  10. `APP_LAUNCHER`: Local system app launching
- **Trilingual Examples:** Every topic includes authentic English, Bangla script, and Banglish (phonetic Roman script) commands.
- **Dynamic Help Formatter:** Supports topic-level lookups (`"help torch"`, `"help wifi"`, `"help timer"`, etc.) with informative fallbacks.

### 3.2 Feature B: Safe System Settings Shortcuts
- **Architecture & Decoupling:** Implemented [SystemSettingsController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt) backed by an injectable interface `SystemSettingsLauncher` for testability without Android runtime dependencies.
- **Zero Invasive Permissions:** Does NOT use `WRITE_SETTINGS`, `BLUETOOTH_ADMIN`, or Accessibility services.
- **Panel Support (API 29+):**
  - **Wi-Fi:** `Settings.Panel.ACTION_WIFI` (with fallback to `Settings.ACTION_WIFI_SETTINGS`).
  - **Bluetooth:** `Settings.ACTION_BLUETOOTH_SETTINGS`.
  - **Mobile Data:** `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` (with fallback to `Settings.ACTION_DATA_ROAMING_SETTINGS` / `Settings.ACTION_NETWORK_OPERATOR_SETTINGS`).
  - **Volume & Sound:** `Settings.Panel.ACTION_VOLUME` (with fallback to `Settings.ACTION_SOUND_SETTINGS`).
  - **Brightness:** `Settings.ACTION_DISPLAY_SETTINGS`.
- **Honest Feedback on Direct Toggle Attempts:** When users request direct toggles (e.g., `"turn on wifi"`, `"bluetooth bondho koro"`), SAARTHI clarifies platform restrictions:
  `"Android security prevents apps from toggling Wi-Fi directly. I can open Wi-Fi settings for you."`
- **Execution Contract Verification:** Emits `ExecutionContract.Succeeded` only after the Android activity launcher successfully accepts the intent, and `ExecutionContract.Failed` if an `ActivityNotFoundException` or `SecurityException` occurs.

### 3.3 Feature C: Practical Local Timer Engine
- **Engine Design:** Implemented [TimerController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt) with an injectable `TimeProvider` and `CoroutineScope` (`Dispatchers.Default`).
- **Timer States:** `Idle`, `Running`, `Paused`, `Finished(Ringing)`.
- **Debouncing:** Built-in duplicate command prevention rejects identical timer commands issued within a 2.5-second window.
- **Duration Parser:** Supports digits and words in English, Bangla script (e.g., `২ মিনিট`, `৩০ সেকেন্ড`), and Banglish (e.g., `2 minute er timer dao`). Rejects durations $\le 0$ or $> 24$ hours with helpful feedback.
- **Notifications:** Posts a system notification (`CHANNEL_TIMER`) when the timer completes if `POST_NOTIFICATIONS` is granted.
- **Background Execution Limitation:** Coroutine runs on `Dispatchers.Default`. The limitation is documented honestly: if the system terminates the process when the user swipes the app away, the timer will pause unless the foreground service is active.

### 3.4 Feature D: Command Routing Pipeline
- **Shared Voice & Text Path:** Both voice recognition and manual text input feed into `CommandEngine.detectIntent()`.
- **Precedence Rule:** Help queries are checked before hardware/timer action parsers so commands like `"help timer"` return guidance rather than failing on missing durations.
- **Intent Extensions:**
  - `IntentType.OPEN_SYSTEM_SETTINGS(type: SystemSettingType)`
  - `IntentType.SET_TIMER(durationSeconds: Long)`
  - `IntentType.PAUSE_TIMER`
  - `IntentType.RESUME_TIMER`
  - `IntentType.CANCEL_TIMER`
  - `IntentType.TIMER_STATUS`
- **Foreground Service Alignment:** [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) implements full execution branches for all Stage 3B intents.

### 3.5 Feature E: UI Integration
- **[TimerCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt):** Circular progress indicator, formatted MM:SS countdown, Start/Pause/Resume/Cancel/Dismiss buttons, and 1m/3m/5m preset chips. Appears on `HomeScreen` whenever a timer is active, and persists on `ToolsScreen`.
- **[SystemSettingsCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SystemSettingsCard.kt):** Grid of one-tap shortcuts for Wi-Fi, Bluetooth, Mobile Data, Sound, and Brightness.
- **[HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt):** Expandable accordion listing all 10 capability categories with clickable prompt chips that automatically execute or populate the query.
- **[SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt):** Added custom vector icons for `Wifi`, `Bluetooth`, `MobileData`, `Brightness`, `Timer`, `Help`, `Play`, and `Pause`.

---

## 4. Supported Command Examples

| Category | English | Bangla | Banglish |
|---|---|---|---|
| **Battery** | "What is my battery percentage?" | "ব্যাটারি কত শতাংশ আছে?" | "Battery koto percent ache?" |
| **Flashlight** | "Turn on flashlight" / "Torch off" | "ফ্ল্যাশলাইট জ্বালাও" / "টর্চ বন্ধ করো" | "Torch on koro" / "Flashlight off" |
| **Wi-Fi Settings** | "Open Wi-Fi settings" / "Wi-Fi" | "ওয়াইফাই সেটিংস খোলো" | "Wifi settings kholo" |
| **Bluetooth Settings** | "Open Bluetooth settings" | "ব্লুটুথ সেটিংস খোলো" | "Bluetooth settings open koro" |
| **Mobile Data Settings** | "Open mobile network settings" | "মোবাইল নেটওয়ার্ক সেটিংস খোলো" | "Mobile data settings kholo" |
| **Sound / Volume** | "Open volume settings" | "ভলিউম সেটিংস খোলো" | "Sound settings open koro" |
| **Brightness** | "Open brightness settings" | "ব্রাইটনেস সেটিংস খোলো" | "Brightness settings kholo" |
| **Timer Set** | "Set a timer for 2 minutes" | "২ মিনিটের টাইমার দাও" | "2 minute er timer dao" |
| **Timer Pause** | "Pause timer" | "টাইমার থামাও" | "Timer pause koro" |
| **Timer Resume** | "Resume timer" | "টাইমার চালু করো" | "Timer resume koro" |
| **Timer Cancel** | "Cancel timer" | "টাইমার বাতিল করো" | "Timer cancel koro" |
| **Timer Status** | "How much time is left on the timer?" | "টাইমারে কত সময় বাকি আছে?" | "Timer e koto time baki?" |
| **Help (General)** | "Help" / "What can you do?" | "সাহায্য" / "তুমি কি কি করতে পারো?" | "Help koro" / "Ki korte paro?" |
| **Help (Topic)** | "Help timer" / "Help flashlight" | "ব্যাটারি সাহায্য" | "Help wifi" |

---

## 5. Permissions, Privacy & Security Audit

- **Permissions in `AndroidManifest.xml`:**
  - `RECORD_AUDIO`: Voice command recognition (offline).
  - `ACCESS_NETWORK_STATE`: Honest offline/online detection.
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Background assistant operation.
  - `POST_NOTIFICATIONS`: Android 13+ timer completion notifications.
- **New Permissions Added in Stage 3B:** **NONE (0 new permissions)**.
- **Invasive Permissions Rejected:**
  - `WRITE_SETTINGS`: NOT added. Android prevents silent changes without special system write permission. SAARTHI directs users safely to system settings instead.
  - `CHANGE_WIFI_STATE`: Deprecated on modern Android for toggling; opening settings panels is the supported, non-invasive standard.
  - `BLUETOOTH_ADMIN` / `BLUETOOTH_CONNECT`: Direct toggle is restricted; opening Bluetooth settings requires no dangerous permissions.
  - `SCHEDULE_EXACT_ALARM`: Avoided to prevent battery optimization warnings; the timer uses lightweight coroutine ticks with local notifications.
- **Cloud/Internet Access:** Zero cloud APIs, telemetry, or remote dependencies introduced. 100% on-device local execution.

---

## 6. Files Created and Modified

| File Path | Action | Description |
|---|---|---|
| [android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt) | **Created** | Controller and launcher for Wi-Fi, Bluetooth, Data, Volume, and Brightness settings with SDK-aware panels. |
| [android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt) | **Created** | Countdown timer engine with state machine, debouncer, notification hooks, and pure JVM test provider. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/SystemSettingsCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SystemSettingsCard.kt) | **Created** | Reusable Compose card offering direct system settings launcher tiles. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt) | **Created** | Reusable Compose card displaying countdown progress, controls, and duration presets. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt) | **Created** | Reusable Compose accordion presenting categorized help topics and trilingual prompt chips. |
| [android/app/src/test/java/com/saarthi/assistant/core/SystemSettingsControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/SystemSettingsControllerTest.kt) | **Created** | 13 automated unit tests for settings launcher contracts and intent action mappings across SDK versions. |
| [android/app/src/test/java/com/saarthi/assistant/core/TimerControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/TimerControllerTest.kt) | **Created** | 19 automated unit tests for timer states, debouncing, tick countdown, pause, resume, cancel, and bounds validation. |
| [android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt) | Modified | Added complete entries for all 10 capabilities; added `getTopicHelp` fuzzy matcher for torch, battery, wifi, settings, etc. |
| [android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt) | Modified | Added `OPEN_SYSTEM_SETTINGS`, `SET_TIMER`, `PAUSE_TIMER`, `RESUME_TIMER`, `CANCEL_TIMER`, `TIMER_STATUS`. |
| [android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt) | Modified | Added result models: `OpenSystemSettings`, `SetTimer`, `PauseTimer`, `ResumeTimer`, `CancelTimer`, `GetTimerStatus`. |
| [android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Modified | Integrated settings & timer regexes, Bengali numeral parsing (`০-৯`), duration extraction, and toggle ambiguity interceptors. |
| [android/app/src/main/java/com/saarthi/assistant/MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) | Modified | Wired `TimerController` snapshot, settings launchers, direct UI action callbacks, and notifications channel setup. |
| [android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) | Modified | Added command execution branches for `OpenSystemSettings`, `SetTimer`, `PauseTimer`, `ResumeTimer`, `CancelTimer`, and `GetTimerStatus`. |
| [android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt) | Modified | Conditionally renders `TimerCard` banner when a timer is running/paused/ringing. |
| [android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt) | Modified | Integrated `TimerCard`, `SystemSettingsCard`, and `HelpSection`. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt) | Modified | Added vector icons: `Wifi`, `Bluetooth`, `MobileData`, `Brightness`, `Timer`, `Help`, `Play`, `Pause`. |
| [android/app/src/main/res/values/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values/strings.xml) | Modified | Added English strings for settings shortcuts, timer controls, and help headers. |
| [android/app/src/main/res/values-bn/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values-bn/strings.xml) | Modified | Added Bengali strings for settings shortcuts, timer controls, and help headers. |
| [android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt) | Modified | Expanded to 50 comprehensive tests covering settings, timers, Bengali numerals, and help topics. |

---

## 7. Automated Test Results & Verification

### 7.1 Test Suite Summary
- **Execution Command:** `.\gradlew.bat test`
- **Build Status:** **BUILD SUCCESSFUL**
- **Execution Duration:** 2.396s
- **Total Test Classes:** 9
- **Total Tests Executed:** **131**
- **Passed:** **131** | **Failed:** **0** | **Skipped:** **0** (100% Pass Rate)

### 7.2 Breakdown by Test Class
1. **[CommandEngineTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt):** 50 tests (100% pass)
   - Trilingual settings shortcuts (Wi-Fi, Bluetooth, Mobile Data, Sound, Brightness).
   - Direct toggle request interceptors & ambiguity explanations.
   - Bengali numeral & duration parsers for timers.
   - Timer commands (set, pause, resume, cancel, status).
   - Topic-specific help requests (`"help timer"`, `"help flashlight"`, `"help wifi"`).
   - Stage 1, Stage 2, and Stage 3A regressions (Calculator, Date/Time, App Launcher, Battery, Flashlight).
2. **[TimerControllerTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/TimerControllerTest.kt):** 19 tests (100% pass)
   - Initial idle state, start timer, duration boundaries ($>0$ and $\le 24$h).
   - Precise countdown ticks, zero-time completion transition to `Finished`.
   - Pause and resume logic, pause state invariance during ticks.
   - Cancel and reset to `Idle`.
   - Duplicate command debouncing within 2.5-second threshold.
   - Status query string formatting across states.
3. **[SystemSettingsControllerTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/SystemSettingsControllerTest.kt):** 13 tests (100% pass)
   - Launcher execution contracts (`ExecutionContract.Succeeded` on valid launch).
   - Activity not found / Security exception handling (`ExecutionContract.Failed`).
   - SDK-specific intent action mappings (Panels on API 29+ vs fallbacks on older SDKs).
4. **[DeviceHardwareControllerTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/DeviceHardwareControllerTest.kt):** 14 tests (100% pass)
   - Battery level clamping ($0-100\%$), charging states, and source mapping.
   - Flashlight contracts, hardware unavailable, and hardware busy states.
5. **[ExecutionContractTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/ExecutionContractTest.kt):** 7 tests (100% pass)
6. **[SpeechPronunciationFormatterTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/voice/SpeechPronunciationFormatterTest.kt):** 7 tests (100% pass)
7. **[NaturalVoiceSessionTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/wakeword/NaturalVoiceSessionTest.kt):** 11 tests (100% pass)
8. **[WakeWordCoordinatorTest](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/wakeword/WakeWordCoordinatorTest.kt):** 9 tests (100% pass)
9. **`ExampleUnitTest`:** 1 test (100% pass)

---

## 8. Build Verification & APK Footprint Analysis

Both Debug and Release APKs were compiled and verified:

### 8.1 Build Execution Commands & Status
- **Debug Build:** `.\gradlew.bat assembleDebug` $\rightarrow$ **BUILD SUCCESSFUL in 29s**
- **Release Build:** `.\gradlew.bat clean assembleRelease` $\rightarrow$ **BUILD SUCCESSFUL in 3m 6s**

### 8.2 APK Artifacts & Metrics
| Build Type | File Path | File Size (Bytes) | SHA-256 Checksum |
|---|---|---|---|
| **Debug APK** | `android\app\build\outputs\apk\debug\app-debug.apk` | 13,097,742 bytes | — |
| **Release APK** | `android\app\build\outputs\apk\release\app-release.apk` | **1,628,313 bytes** | `FC9288B21F023D85CCBEA8086E45AEAC8847AEFA17D439A679FC0E3491180C5B` |

### 8.3 Comparison with Previous Baseline
- **Stage 3A Reported Release APK:** 1,584,569 bytes
- **Stage 3B Release APK:** 1,628,313 bytes
- **Net Difference:** **+43,744 bytes (+2.76%)**
- **Target Budget Threshold:** 5,000,000 bytes
- **Budget Consumption:** **32.56% of ceiling** (over 3.37 MB remaining buffer)
- **Package Identity:** `com.saarthi.assistant` (matches existing installation package).

---

## 9. Feature Verification Status Table

| Feature / Capability | Implementation Status | Verification Label | Notes |
|---|---|---|---|
| Battery Percentage & Charging Query | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Unit tests verify broadcast parsing; awaits physical phone test |
| Flashlight ON/OFF | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | CameraManager mock tested; awaits physical phone test |
| Wi-Fi Settings Shortcut | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Panel / Settings intent verified by unit test |
| Bluetooth Settings Shortcut | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Settings intent verified by unit test |
| Mobile Data Settings Shortcut | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Panel / Settings intent verified by unit test |
| Sound / Volume Settings Shortcut | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Panel / Settings intent verified by unit test |
| Brightness Settings Shortcut | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Display settings intent verified by unit test |
| Timer Countdown & Controls | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Coroutine engine & debouncing verified by unit test |
| Help Registry (All 10 Topics) | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | Topic lookups verified by unit test |
| Trilingual Command Routing | Implemented | `AUTOMATED-TESTED`, `BUILD-VERIFIED` | 50 CommandEngine unit tests passing |
| UI Integration (Home & Tools) | Implemented | `BUILD-VERIFIED` | Compose layouts compiled cleanly into APK |
| Physical Hardware Verification | Not Done | `NOT VERIFIED` | **Physical phone verification has NOT been performed** |

---

## 10. Known Android & OEM Limitations

1. **Direct Toggles Restricted on Modern Android:**
   Android 10+ (API 29+) explicitly prohibits third-party applications from programmatically enabling or disabling Wi-Fi, Bluetooth, or Mobile Data without system-level signatures. SAARTHI respects this constraint and opens the official system settings panels instead of employing unstable private API hacks.
2. **Background Coroutine Throttling on Aggressive OEM ROMs:**
   Certain OEM skins (e.g., Xiaomi MIUI/HyperOS, Samsung OneUI, Vivo FuntouchOS) aggressively kill background processes when an app is removed from the recent apps list. While `TimerController` continues counting while the app is paused or if the foreground service is active, swiping away the app without battery optimization exclusion may pause the coroutine ticker.
3. **Notification Permission on Android 13+ (API 33+):**
   If the user denies the runtime `POST_NOTIFICATIONS` permission, the in-app timer will complete and display the finished state on screen, but the system ring notification will not appear in the status bar.

---

## 11. Practical Phone-Testing Checklist for the User

Once the user transfers and installs `app-release.apk` (`1,628,313 bytes`), perform the following verification steps:

- [ ] **1. Help Discovery Check:**
  - Open SAARTHI and navigate to the **Tools** tab.
  - Expand the **Help & Commands** section.
  - Verify that all 10 capability categories are present, including **Battery Status**, **Flashlight Control**, **System Controls**, and **Timer**.
  - Tap one of the example prompt chips (e.g., `Torch on koro`) and verify that it populates and executes.
- [ ] **2. Help Voice / Text Query Check:**
  - In the input bar, type `help battery` $\rightarrow$ Confirm it returns battery command examples.
  - Type `help timer` $\rightarrow$ Confirm it returns timer command examples.
- [ ] **3. Battery & Flashlight Check:**
  - Type or speak: `What is my battery percentage?` $\rightarrow$ Confirm real device percentage is returned.
  - Type or speak: `Torch on koro` $\rightarrow$ Confirm rear camera LED illuminates.
  - Type or speak: `Flashlight off` $\rightarrow$ Confirm rear camera LED turns off.
- [ ] **4. System Settings Shortcuts Check:**
  - In **Tools** $\rightarrow$ **System Controls**, tap **Wi-Fi** $\rightarrow$ Verify the Wi-Fi panel or settings screen opens.
  - Tap **Bluetooth** $\rightarrow$ Verify Bluetooth settings screen opens.
  - Tap **Mobile Data** $\rightarrow$ Verify Cellular/Internet settings screen opens.
  - Tap **Sound** $\rightarrow$ Verify Volume panel or Sound settings screen opens.
  - Tap **Brightness** $\rightarrow$ Verify Display settings screen opens.
  - Type `turn on wifi` $\rightarrow$ Confirm SAARTHI explains that Android restricts direct toggling and offers to open Wi-Fi settings.
- [ ] **5. Timer Flow Check:**
  - In **Tools** $\rightarrow$ **Timer**, tap the **+1m** preset chip.
  - Confirm the countdown begins and the circular progress decreases.
  - Tap **Pause** $\rightarrow$ Confirm countdown stops.
  - Tap **Resume** $\rightarrow$ Confirm countdown continues.
  - Switch to the **Home** tab $\rightarrow$ Confirm the active timer banner appears at the top.
  - Speak: `30 second er timer dao` $\rightarrow$ Confirm a 30-second timer starts.
  - Allow the timer to reach 00:00 $\rightarrow$ Confirm the "Timer Completed" status triggers.
