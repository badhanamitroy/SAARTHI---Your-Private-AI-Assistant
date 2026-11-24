# SAARTHI — Project Chronological Changelog

All notable changes and architectural milestones for **SAARTHI AI — Your Private AI Assistant** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to semantic development staging.

## [Stage 5B — Hybrid AI Architecture & Wake-Word Feasibility] — 2026-10-29

### Added & Enhanced
- **Architectural Request Classification**:
  - Formalized a 4-tier classification enum (`RequestClassification`) in `HybridRequestRouter`: `LOCAL_DEVICE_COMMAND`, `LOCAL_OFFLINE_QUERY`, `GENERAL_KNOWLEDGE_AI`, and `SENSITIVE_REJECTED`.
  - Added deterministic `classify(rawInput, conversationContext)` method ensuring known local commands (apps, hardware, settings, timer, calls) and local queries (time, date, math, greetings, help) execute 100% offline.
  - Added `suspend fun routeSuspend(...)` using `withContext(Dispatchers.IO)` to guarantee asynchronous, non-blocking execution off the Android UI thread.
- **Telephony & Contact Privacy Protection in `SensitiveDataFilter`**:
  - Added `SensitiveCategory.PHONE_OR_CONTACT` covering personal phone numbers, caller identity queries, contact records, and call log queries.
  - Created regex patterns detecting international E.164, local 11-digit mobile (BD), formatted US/standard numbers, and telephony queries in English, Bangla script, and Banglish.
  - Guarantees that private telephony data is intercepted and refused locally (`PrivacyRefusal`) with zero bytes ever transmitted to third-party or cloud AI services.
- **Multi-Provider Serverless Proxy Script (`worker.js`)**:
  - Upgraded Cloudflare Worker proxy script to dynamically support Groq Free Developer Tier (primary, sub-second latency), Google Gemini 2.5 Flash (high reasoning via OpenAI compatibility layer), and Cloudflare Workers AI (`@cf/meta/llama-3.2-3b-instruct`) with zero secrets bundled in the Android client APK.
- **AI Provider & Acoustic Wake-Word Feasibility Research**:
  - Documented comparative analysis of 5 free AI providers (Groq, Gemini, Cloudflare Workers AI, Hugging Face, OpenRouter) covering quotas, data retention policies, and voice latency.
  - Conducted detailed KWS feasibility study analyzing measured vs. estimated sizes for openWakeWord, Vosk, Porcupine, and custom TFLite Micro CNN under the 5 MB APK budget, public transit bus noise SNR degradation, and Android background microphone restrictions (`android:foregroundServiceType="microphone"`).
- **Automated Test Suite Expansion**:
  - Added 6 new unit tests in `HybridRequestRouterTest` (classification for all 4 architectural tiers, phone/contact privacy refusal, and coroutine-safe `routeSuspend`).
  - Added 2 new unit tests in `SensitiveDataFilterTest` (phone number detection and telephony/contact query detection).
  - Total test count expanded from **211 to 219 tests across 18 test suites** (100% pass rate, 0 failures, 0 errors, 0 skipped).

### Verified & Measured
- Automated Unit Tests: **219/219 passed** across 18 test suites (0 failures, 100% pass rate).
- Release APK Build: `packageRelease --rerun-tasks` succeeded in 2m 57s with final size **1,652,449 bytes (~1.58 MB)** (3,347,551 bytes / ~3.35 MB headroom below the 5.0 MB ceiling).
- SHA-256 Checksum: `5D46AEDD91C17FF03CBB81A65FCFFAACB5A075E2BAC9415E5D7B21563FA2C297`.
- Real-Device Verification: Truthfully retained as **`NOT VERIFIED (No physical device connected)`**.

## [Stage 5A — Hands-Free Voice Intelligence & Wake-Word Audit] — 2026-10-29

### Fixed & Hardened
- **Turn-Taking Continuous Voice Session Lifecycle**:
  - Fixed infinite listening restart defect in `VoiceSessionCoordinator`: when user issues `CommandResult.StopListening` (e.g. "Stop listening", "শোনা বন্ধ করো", "থামো"), `isSessionActive` is immediately marked `false`. Upon completing confirmation speech, the coordinator transitions to `ConversationState.IDLE, "Stopped"` instead of restarting recognition.
  - Added exception safety around `actionExecutor` in `VoiceSessionCoordinator`, ensuring `isDispatchingAction` is always cleared even if a controller or platform API throws an exception.
  - Hardened `pauseListening()` to explicitly silence active speech synthesis via `speechSynthesizer.stop()` when an incoming call arrives.
  - Hardened `resumeListening()` to reset consecutive silences and seamlessly restore active follow-up context.
  - Added unrecoverable service error gating in `handleRecognitionError`: stops session immediately when recognition service is not available or unsupported.
- **Speech Engine Concurrency Hardening**:
  - Added `@Volatile private var isStarting = false` guard in `AndroidSpeechEngine` to prevent race conditions and duplicate recognizer initializations during rapid asynchronous triggers.
  - Maintained listening status during end-of-speech window until final results or errors are received.
- **Acoustic Wake-Word Feasibility Disclosed**:
  - Evaluated 6 KWS engines (Picovoice Porcupine, openWakeWord, Vosk, CMU PocketSphinx, TFLite Micro, and Android VoiceInteractionService) against offline, free, and < 5 MB constraints.
  - Formally documented that genuine on-device acoustic models (ONNX/TFLite + weights) add 6–15 MB, exceeding the 5 MB ceiling. Retained deterministic `LocalWakeWordDetector` for transcript parsing in active sessions and outlined options for future stage decisions.
- **Automated Test Suite Expansion**:
  - Added 5 new unit tests in `VoiceSessionCoordinatorTest`: explicit stop listening exit, action execution exception recovery, unrecoverable error gating, call interruption shielding, and duplicate start session debouncing.
  - Added 2 new unit tests in `NaturalVoiceSessionTest`: transcript phrase extraction limits and `AcousticWakeWordEngine` 16 kHz audio frame contract.
  - Total test count increased from **204 to 211 tests across 18 test suites** (100% pass rate, 0 failures).

### Verified & Measured
- Automated Unit Tests: **211/211 passed** across 18 test suites (0 failures, 100% pass rate).
- Release APK Build: `assembleRelease` succeeded in 3m with final size **1,652,449 bytes (~1.58 MB)** (3,347,551 bytes / ~3.35 MB headroom below the 5.0 MB ceiling).
- SHA-256 Checksum: `32E268FCEB7B5DD8DC2A7002374486B29198587F85633F5414F00BF0EDB482C1`.
- Real-Device Verification: Recorded honestly as **`NOT VERIFIED`** due to headless test environment without an attached physical device.

## [Stage 4.1.1 — Test Evidence Audit & Real-Device Readiness] — 2026-10-29

### Audited & Verified
- **Resolved Test-Count Discrepancy**:
  - Forensically verified all 18 JUnit XML reports under `android/app/build/test-results/testDebugUnitTest/` and the HTML report index.
  - Resolved documentation arithmetic discrepancy: the true count is exactly **204 unit tests across 18 test suites** (100% pass rate, 0 failures, 0 errors, 0 skipped).
  - Executed fresh live test run with `.\gradlew.bat testDebugUnitTest --rerun` (Exit code: 0, 38s).
- **Verified Release Build Artifact**:
  - Fresh compilation via `.\gradlew.bat assembleRelease` succeeded with exit code 0.
  - APK file verified at `android\app\build\outputs\apk\release\app-release.apk`.
  - Byte size: **1,652,449 bytes (~1.58 MB)**, leaving 3,347,551 bytes (~3.35 MB headroom) below 5.0 MB target.
  - SHA-256 Hash: `59D277ADF4CE7DB2F0134163CA9A2000D48EE43B56AD8DE358A906271CD501BE`.
- **Physical Device Readiness & Environment Check**:
  - Invoked `adb.exe` at `C:\Users\Dell\AppData\Local\Android\Sdk\platform-tools\adb.exe` (`adb devices`).
  - Confirmed 0 connected physical devices or emulators.
  - Reaffirmed strict **`NOT VERIFIED (No physical device connected)`** status for hardware tests.
  - Prepared comprehensive 10-step Real-Device Testing Checklist covering permission grants, speaker/earbud routing, unknown callers, voice answer/reject, duplicate suppression, Call Waiting protection, and background resilience.

## [Stage 4.1 — Caller Announcement Preference Correction & Verification] — 2026-10-29

### Fixed & Corrected
- **Default Caller Announcement Audio Routing**:
  - Corrected announcement gating to play through the **phone speaker by default** when no earphones/earbuds are plugged in, matching explicit user requirements.
  - Decoupled `speakerAnnouncementEnabled` (default `true`) and `earbudsAnnouncementEnabled` (default `true`) into explicit, independent preferences in `CallPreferences.kt`.
  - Updated `SmartCallerAnnouncer.kt` to route audio according to active hardware status: evaluates `earbudsAnnouncementEnabled` when a headset is connected, and `speakerAnnouncementEnabled` when using the phone speaker.
  - Honored user opt-out: if the user explicitly turns off speaker announcements, calls remain silent through the speaker when earbuds are disconnected.
- **Active Call Protection (Call Waiting Safety)**:
  - Added guard in `CallCoordinator.kt` (`previousState == CallState.OFFHOOK` -> return) preventing incoming call waiting notifications from announcing over an ongoing phone conversation.
- **Duplicate Broadcast Suppression**:
  - Implemented deduplication in `CallCoordinator.kt` suppressing repeated `PHONE_STATE` broadcasts for the same incoming call number while already ringing.
- **Settings Screen & Localization**:
  - Updated `SettingsScreen.kt` with separate switches for "Phone Speaker Announcement" and "Earbuds / Headset Announcement".
  - Added complete Bengali translations in `values-bn/strings.xml` for all calling preference labels and descriptions.
  - Wired settings state changes from `MainActivity.kt` into `AndroidCallPreferencesManager`.
- **Deprecation Suppressions**:
  - Added explicit `@Suppress("DEPRECATION")` annotations on `TelephonyManager.EXTRA_INCOMING_NUMBER` and legacy reflection endpoints to keep build logs clean.

### Verified & Measured
- Automated Unit Tests: **204/204 passed** across 18 test suites (0 failures, 100% pass rate).
- Release APK Build: `assembleRelease` succeeded with final binary size **1,652,449 bytes (~1.58 MB)** (3,347,551 bytes / ~3.35 MB headroom below the 5.0 MB limit).
- SHA256 Checksum: `59D277ADF4CE7DB2F0134163CA9A2000D48EE43B56AD8DE358A906271CD501BE`.
- Real-Device Verification: Recorded honestly as **NOT VERIFIED** due to headless test environment without an attached physical device.

## [Stage 4 — Hands-Free Calling, Smart Caller Awareness & Privacy Controls] — 2026-10-28

### Added & Implemented
- **Incoming Call State Detection**:
  - Implemented `IncomingCallReceiver` listening to `android.intent.action.PHONE_STATE`.
  - Added [CallCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/telephony/CallCoordinator.kt) managing call transitions across `RINGING`, `OFFHOOK`, and `IDLE` states without polling or root hacks.
- **Smart Caller Announcement**:
  - Implemented [ContactResolver.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/telephony/ContactResolver.kt) resolving incoming numbers against `ContactsContract.PhoneLookup.DISPLAY_NAME`.
  - Built [SmartCallerAnnouncer.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/telephony/SmartCallerAnnouncer.kt) formatting "[Name] is calling." or "Unknown number is calling." with capped repetition (max 2) and immediate audio cancellation when answered, rejected, or ended.
- **Earbud-Only Public Transit Mode**:
  - Implemented [AudioRouteDetector.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/telephony/AudioRouteDetector.kt) identifying wired headsets and Bluetooth earbuds via `AudioManager` and `AudioDeviceInfo`.
  - Added "Announce Only on Earbuds" setting: keeps speaker completely silent in crowded buses/trains if no earbud is connected.
- **Voice-Controlled Call Answering & Rejection**:
  - Implemented trilingual command recognition in [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) for English ("answer", "receive call", "pick up", "reject", "decline", "ignore", "hang up", "cut call"), Bangla ("কল ধরো", "ফোন ধরো", "রিসিভ করো", "কল কাটো", "ফোন কাটো", "কেটে দাও"), and Banglish ("call dhoro", "phone dhoro", "receive koro", "call kato", "phone kato", "kete dao").
  - Outgoing call commands ("call mom") are cleanly separated from incoming call controls without collision.
  - Implemented [CallController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/telephony/CallController.kt) invoking `TelecomManager.acceptRingingCall()` (API 26+) and `TelecomManager.endCall()` (API 28+) with truthful error reporting if permissions are missing or OEM ROM blocks programmatic execution.
- **Turn-Taking Voice Session During Ringing**:
  - Active assistant voice listening is paused when a call begins ringing to prevent acoustic interference.
  - Pre-call voice session state is cleanly restored when the call transitions back to idle.
- **UI & Telephony Settings**:
  - Added "Hands-Free Calling & Caller Awareness" card to [SettingsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/SettingsScreen.kt) with toggles for Smart Caller Announcement, Earbuds-Only Announcement, Voice Call Control, Announce Unknown Numbers, and Permission status.
  - Wired in `MainActivity.kt` and `AssistantForegroundService.kt`.
- **Help Registry Extension**:
  - Added "calling" category to `HelpRegistry.kt` with trilingual speech examples.
- **Free Hybrid AI Preparation Foundation**:
  - Created decoupled `AiProvider`, `HybridRequestRouter`, `SensitiveDataFilter` (regex + Luhn card validation), `PrivacySettings`, and Cloudflare Worker proxy backend script (`backend/cloudflare-worker/worker.js`) for zero-cost cloud expansion with zero hardcoded credentials.

### Verified & Measured
- Automated Unit Tests: **199/199 passed** across 18 test suites (0 failures, 100% pass rate).
- Release APK Build: `assembleRelease` succeeded with final binary size **1,649,433 bytes (~1.57 MB)** (well within the 5,000,000-byte budget; 3.35 MB headroom).

## [Stage 3C — Critical Build Recovery, Voice Reliability & Conversation Continuity] — 2026-10-28

### Fixed & Recovered
- **Kotlin Lexical Scoping Failure in `MainActivity.kt`**: Resolved compilation failure where `executeCommandAction` was referenced prior to its definition inside `@Composable fun SaarthiApp`; eliminated the duplicate lower declaration.
- **Interface Drift & JVM Clashes in Test Suite**:
  - Added `fun release()` to `VoiceEngine` interface and marked `override` in `AndroidSpeechEngine`.
  - Added `override fun isInitialized(): Boolean = true` to `FakeSpeechSynthesizer`.
  - Resolved JVM platform declaration clash between `isListening` field and getter in `FakeVoiceEngine`.
  - Harmonized `SystemSettingsLauncher` interface with default delegations to resolve abstract member implementations in `SystemSettingsControllerTest.kt`.
- **Multi-Turn Conversational Follow-Up**:
  - Integrated `ConversationContext` into `CommandEngine` and `VoiceSessionCoordinator`.
  - Timer follow-up queries (e.g. "5 minutes" following "How long should I set the timer for?") now resolve directly into `CommandResult.SetTimer(300L)` rather than falling through to generic `NeedsOnlineAI`.
  - Added Bengali and Banglish cancellation phrases ("বাতিল", "lagbe na", "বাদ দাও", "bad dao") to clear pending context.
  - Implemented context isolation ensuring unrelated commands ("what time is it") clear pending follow-up without being hijacked.
- **Continuous Turn-Taking Voice Sessions**:
  - Built [VoiceSessionCoordinator.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/VoiceSessionCoordinator.kt) managing the full turn-taking voice interaction lifecycle.
  - Users no longer need to repeatedly tap the Talk button between turns; the session automatically resumes listening after speech synthesis completes.
  - Implemented mutual exclusion audio ducking to prevent microphone self-hearing during TTS playback.
  - Implemented bounded silence backoff (2 consecutive silences) to prevent infinite listening loops.
- **Stuck `Speaking...` / `Responding...` State Prevention**:
  - Added dynamic Handler watchdog runnable in [TextToSpeechEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt) ($\text{length} \times 120\text{ ms}$ bounded between 4s and 25s) ensuring recovery if an OEM TTS drops `onDone` callbacks.
  - Handled muted voice feedback (`isVoiceFeedbackEnabled == false`) with immediate state transitions.
- **Wi-Fi Toggle Command Routing**:
  - Ensured toggle aliases ("Turn off Wi-Fi", "Turn Wi-Fi off", "ওয়াইফাই বন্ধ করো", "Wi-Fi off koro") route directly to `Settings.Panel.ACTION_WIFI` with truthful spoken guidance explaining Android platform restrictions.

### Verified & Measured
- Automated Unit Tests: **148/148 passed** across 10 test suites (0 failures, 0 skipped, 100% pass rate).
- Release APK Build: `assembleRelease` succeeded with final binary size **1,644,697 bytes (~1.57 MB)** (well within the 5,000,000-byte budget).
- Reality Verification: Documented that wake-word detection is text-matched via platform `SpeechRecognizer` (no acoustic KWS model) and timer countdowns are in-memory coroutines (no `AlarmManager` persistence).

## [Complete Feature Audit & Forensic Verification] — 2026-10-28

### Inspected & Documented
- **Full Project Feature Inventory**: Audited 88 capabilities across Core Assistant, Voice Engine, Wake Word, Command System, Phone Controls, UI, Lifecycle, Privacy, and Releases.
- **Root Cause Analyses of Reported Physical Issues**:
  - Investigated timer follow-up fallback ("Set a timer" -> "5 minutes").
  - Investigated "Turn off Wi-Fi" routing failure.
  - Investigated repeated button tap requirement.
  - Investigated frozen "Speaking..." / "Responding..." UI states.
  - Investigated battery and flashlight hardware execution.
  - Verified Help topic coverage.
- **Defect Discovery**: Documented lexical scoping compilation failure in working copy [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt#L211) and missing/invalid method overrides in [VoiceSessionCoordinatorTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/voice/VoiceSessionCoordinatorTest.kt#L39).
- **Core Audit Deliverables**:
  - `docs/SAARTHI_COMPLETE_FEATURE_AUDIT.md`: Comprehensive audit report and analysis.
  - `docs/SAARTHI_FEATURE_MATRIX.md`: 88-row feature inventory with 7-column verification matrix.
  - `docs/SAARTHI_DEVELOPMENT_ROADMAP.md`: Sequenced engineering backlog from P0 to P5.
  - `docs/SAARTHI_AUDIT_FINDINGS.md`: Detailed defect records, root causes, and doc discrepancies.

## [Stage 3B — System Controls, Timer & Help Integration] — 2026-10-26

### Added
- **Feature A: Centralized Help Discovery & Stage 3A Integration**:
  - Diagnosed and resolved the root cause of missing Battery and Flashlight topics in Help: `CommandEngine.kt` bypassed `HelpRegistry` with a static string and neither Home nor Tools rendered a Help UI.
  - Connected `CommandEngine` directly to [HelpRegistry.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt) for general queries (`CommandResult.ShowHelp`) and topic-specific queries (`"help battery"`, `"help flashlight"`, `"help timer"`, `"help wifi"`).
  - Created [HelpSection.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/HelpSection.kt), an expandable Compose accordion embedded in `ToolsScreen` displaying 10 capability categories with clickable prompt chips in English, Bangla, and Banglish.
- **Feature B: Safe System Settings Shortcuts**:
  - Implemented [SystemSettingsController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/SystemSettingsController.kt) with decoupled `SystemSettingsLauncher` interface.
  - Safe shortcuts for Wi-Fi, Bluetooth, Mobile Network, Sound/Volume, and Brightness using Android panels (`Settings.Panel.*`) on API 29+ and standard settings intents.
  - Zero invasive permissions requested (0 new permissions in `AndroidManifest.xml`).
  - Added honest policy explanations for direct toggle attempts (e.g., `"turn on wifi"` clarifies Android restrictions and offers settings shortcut).
- **Feature C: Practical In-App Countdown Timer**:
  - Implemented [TimerController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/TimerController.kt) featuring countdown ticker on `Dispatchers.Default`, injectable `TimeProvider`, start/pause/resume/cancel/status controls, duplicate command debouncing, and local completion notifications.
  - Created [TimerCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/TimerCard.kt) with circular progress, MM:SS display, pause/resume/cancel controls, and 1m/3m/5m presets.
- **Feature D: Command Routing & Result Contracts**:
  - Extended [IntentType.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt) and [CommandResult.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt) with `OpenSystemSettings`, `SetTimer`, `PauseTimer`, `ResumeTimer`, `CancelTimer`, `GetTimerStatus`.
  - Added Bengali numerals (`০-৯`) and duration regex parsing supporting English, Bangla, and Banglish patterns.
  - Updated [AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) with execution branches for all Stage 3B intents.
- **UI Integration**:
  - Integrated conditional active `TimerCard` banner into [HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt).
  - Integrated `TimerCard`, `SystemSettingsCard.kt`, and `HelpSection.kt` into [ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt).
  - Added custom vector icons in [SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt): `Wifi`, `Bluetooth`, `MobileData`, `Brightness`, `Timer`, `Help`, `Play`, `Pause`.
- **Automated Testing**:
  - Created [SystemSettingsControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/SystemSettingsControllerTest.kt) (13 tests) and [TimerControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/TimerControllerTest.kt) (19 tests).
  - Expanded [CommandEngineTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt) to 50 tests.
  - Full suite passed: 131/131 tests (100% pass rate).

### Verified
- Automated test suite: **131/131 unit tests passed** across all 9 test classes (0 failures, 0 skipped, 2.396s execution).
- Release APK build: `assembleRelease` succeeded; generated APK size **1,628,313 bytes (~1.55 MB)** against 5.0 MB limit (+43,744 bytes / +2.76% from Stage 3A).
- Debug APK build: `assembleDebug` succeeded (13,097,742 bytes).
- Hardware verification status: **NOT VERIFIED** (Pending user testing on real physical device).

---

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
