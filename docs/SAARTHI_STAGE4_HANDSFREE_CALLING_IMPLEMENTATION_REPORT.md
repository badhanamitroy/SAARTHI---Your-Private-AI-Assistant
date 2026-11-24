# SAARTHI Stage 4 — Hands-Free Calling & Smart Caller Awareness Implementation Report

**Project Root:** `D:\SAARTHI - Your Private AI Assistant`  
**Android Package:** `com.saarthi.assistant`  
**Timestamp:** October 28, 2026  
**Status:** **COMPLETE & BUILD VERIFIED**

---

## 1. Executive Summary & Product Mission

SAARTHI was envisioned as a personal AI assistant built for real moments when your hands are physically occupied — such as standing in an overcrowded public bus holding an overhead grab handle with wired or Bluetooth earbuds on.

Prior to Stage 4, SAARTHI had native offline device utilities (calculator, camera, clock, calendar, system settings panels, battery reporting, flashlight, countdown timer), a turn-taking voice session coordinator, and preparation for a free-tier hybrid AI architecture.

In **Stage 4**, SAARTHI delivers genuine **Hands-Free Calling & Smart Caller Awareness**:
1. **Incoming Call Detection:** Official Android APIs only (`TelephonyManager`, `BroadcastReceiver` listening to `PHONE_STATE`, `TelephonyCallback` / `TelecomManager`). Zero root, zero hidden APIs, zero accessibility hacks.
2. **Smart Caller Announcement:** Resolves contact names through `ContactsContract.PhoneLookup`. If found, announces `"[Caller Name] is calling."` If unlisted/private, announces `"Unknown number is calling."` Announcement repetition is capped at 2 times with pauses, preventing endless talking.
3. **Voice-Controlled Call Answering & Rejection:** Native hands-free commands in English, Bangla, and Banglish:
   - *Answer:* `"answer"`, `"receive call"`, `"pick up"`, `"call dhoro"`, `"phone dhoro"`, `"কল ধরো"`, `"ফোন ধরো"`. Executed via `TelecomManager.acceptRingingCall()` on API 26+.
   - *Reject:* `"reject"`, `"decline"`, `"ignore"`, `"hang up"`, `"cut call"`, `"call kato"`, `"phone kato"`, `"কল কাটো"`, `"ফোন কাটো"`. Executed via `TelecomManager.endCall()` on API 28+.
   - *Honest Fallback:* If permissions are not granted or OS restrictions apply, SAARTHI provides truthful error feedback rather than falsely claiming call operations succeeded.
4. **Earbud-Only Public Transit Optimization:** Automatically detects wired headsets (`AudioDeviceInfo.TYPE_WIRED_HEADSET`) and Bluetooth earbuds (`AudioDeviceInfo.TYPE_BLUETOOTH_A2DP`, `AudioDeviceInfo.TYPE_BLE_HEADSET`) via `AudioManager`. Provides a dedicated privacy setting: **"Announce Only on Earbuds"**. When enabled, announcements remain completely silent if no earbuds are connected, ensuring user privacy in crowded public buses and metro trains.
5. **Turn-Taking Voice Session During Ringing:** When an incoming call arrives, active general assistant speech recognition is suspended to avoid picking up ringing or background bus noise. Once the call ends or transitions to idle, the pre-call session state is cleanly restored.
6. **100% On-Device Privacy for Telephony:** Contact resolution, phone numbers, caller announcements, and call control execute entirely on-device with zero network requests or cloud telemetry.

---

## 2. Architecture & File Inventory

### Newly Created Files:
| File | Role / Responsibility |
|---|---|
| `android/app/src/main/java/com/saarthi/assistant/telephony/CallState.kt` | Enums (`CallState`, `AudioRoute`) and data model (`CallDetails`). |
| `android/app/src/main/java/com/saarthi/assistant/telephony/ContactResolver.kt` | Interface, `AndroidContactResolver` (reads `PhoneLookup` safely with `READ_CONTACTS`), and `FakeContactResolver`. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/AudioRouteDetector.kt` | Interface, `AndroidAudioRouteDetector` (queries `AudioManager` & `AudioDeviceInfo`), and `FakeAudioRouteDetector`. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/CallPreferences.kt` | Data class, `CallPreferencesManager` interface, `AndroidCallPreferencesManager` (SharedPreferences), and `InMemoryCallPreferencesManager`. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/CallController.kt` | Interface, `AndroidCallController` (`acceptRingingCall` & `endCall`), and `FakeCallController`. Honest error reporting. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/SmartCallerAnnouncer.kt` | Spoken formatting (contact vs unknown), audio route gating (earbud privacy), repetition capping (max 2), immediate cancellation on state change. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/CallCoordinator.kt` | Orchestrates incoming call lifecycle, voice session pause/restore, caller announcement, and action dispatch. |
| `android/app/src/main/java/com/saarthi/assistant/telephony/IncomingCallReceiver.kt` | Broadcast receiver registered in manifest for `android.intent.action.PHONE_STATE`. |
| `android/app/src/test/java/com/saarthi/assistant/telephony/SmartCallerAnnouncerTest.kt` | Unit tests for contact name formatting, unknown number gating, earbud privacy filtering, repetition limiter, and cancel. |
| `android/app/src/test/java/com/saarthi/assistant/telephony/CallControllerTest.kt` | Unit tests for call answering, call rejection, permission rejection handling, and platform failure contracts. |
| `android/app/src/test/java/com/saarthi/assistant/telephony/CallCoordinatorTest.kt` | Unit tests for call state transitions (RINGING, OFFHOOK, IDLE), voice session pause/restore, and voice answer/reject dispatch. |
| `android/app/src/test/java/com/saarthi/assistant/telephony/CommandEngineCallTest.kt` | Unit tests for trilingual voice commands (English, Bangla, Banglish) for answering and rejecting calls and non-collision with outgoing call intents. |

### Modified Files:
| File | Modification Details |
|---|---|
| `android/app/src/main/AndroidManifest.xml` | Added telephony permissions (`READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `ANSWER_PHONE_CALLS`, `BLUETOOTH`, `BLUETOOTH_CONNECT`) and registered `IncomingCallReceiver`. |
| `android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt` | Added `CommandResult.AnswerCall` and `CommandResult.RejectCall`. |
| `android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt` | Added `IntentType.CALL_ANSWER` and `IntentType.CALL_REJECT`. |
| `android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt` | Added `isAnswerCallCommand()` and `isRejectCallCommand()`, routed intent parsing in `detectIntent` and `evaluateSingleIntent`, updated compound intent evaluation and unrelated command lists. |
| `android/app/src/main/java/com/saarthi/assistant/core/HelpRegistry.kt` | Added `"calling"` category with trilingual examples ("Answer call", "Call dhoro", "কল ধরো", "Reject call", "Call kato", "কল কাটো"). |
| `android/app/src/main/res/values/strings.xml` | Added strings for Hands-Free Calling & Caller Awareness settings group. |
| `android/app/src/main/java/com/saarthi/assistant/ui/screens/SettingsScreen.kt` | Added "Hands-Free Calling & Caller Awareness" card with toggles for Smart Caller Announcement, Earbuds-Only Announcement, Voice Call Control, Announce Unknown Numbers, and Permission status. |
| `android/app/src/main/java/com/saarthi/assistant/MainActivity.kt` | Initialized telephony controllers and `CallCoordinator`, wired `executeCommandAction` for `AnswerCall` and `RejectCall`, connected settings callbacks, added phone permissions launcher. |
| `android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt` | Added exhaustive branches in `executeCommand` for `AnswerCall` and `RejectCall`. |

---

## 3. Verification & Metrics

### Automated Unit Test Verification
- **Total Test Suites:** 18
- **Total Executed Tests:** 199
- **Failures:** 0
- **Errors:** 0
- **Success Rate:** 100%

```text
TEST-com.saarthi.assistant.ai.HybridRequestRouterTest: 11 tests, 0 failures
TEST-com.saarthi.assistant.ai.PrivacySettingsTest: 8 tests, 0 failures
TEST-com.saarthi.assistant.ai.SecureHttpAiProviderTest: 6 tests, 0 failures
TEST-com.saarthi.assistant.ai.SensitiveDataFilterTest: 10 tests, 0 failures
TEST-com.saarthi.assistant.core.CommandEngineTest: 62 tests, 0 failures
TEST-com.saarthi.assistant.core.DeviceHardwareControllerTest: 12 tests, 0 failures
TEST-com.saarthi.assistant.core.ExecutionContractTest: 8 tests, 0 failures
TEST-com.saarthi.assistant.core.SystemSettingsControllerTest: 10 tests, 0 failures
TEST-com.saarthi.assistant.core.TimerControllerTest: 13 tests, 0 failures
TEST-com.saarthi.assistant.telephony.CallControllerTest: 5 tests, 0 failures
TEST-com.saarthi.assistant.telephony.CallCoordinatorTest: 5 tests, 0 failures
TEST-com.saarthi.assistant.telephony.CommandEngineCallTest: 7 tests, 0 failures
TEST-com.saarthi.assistant.telephony.SmartCallerAnnouncerTest: 7 tests, 0 failures
TEST-com.saarthi.assistant.voice.SpeechPronunciationFormatterTest: 10 tests, 0 failures
TEST-com.saarthi.assistant.voice.VoiceSessionCoordinatorTest: 13 tests, 0 failures
TEST-com.saarthi.assistant.wakeword.NaturalVoiceSessionTest: 6 tests, 0 failures
TEST-com.saarthi.assistant.wakeword.WakeWordCoordinatorTest: 5 tests, 0 failures
TEST-com.saarthi.assistant.ExampleUnitTest: 1 test, 0 failures
Total: 199 tests passed with 0 failures.
```

### Release Build & APK Size Verification
- **Target Constraint:** Strictly below 5,000,000 bytes (5.0 MB).
- **Output Artifact:** `android/app/build/outputs/apk/release/app-release.apk`
- **Measured APK Size:** **1,649,433 bytes** (~1.57 MB).
- **Safety Margin:** 3,350,567 bytes (~3.35 MB headroom below limit).
- **R8 / Minification:** Full resource shrinking and dead-code elimination active. Zero native bloated binaries added.

---

## 4. Hardware and Android Platform Considerations

1. **Android Permission Architecture:**
   - On Android 9+ (API 28+), `READ_CALL_LOG` is required to obtain `EXTRA_INCOMING_NUMBER` from `PHONE_STATE`. If ungranted, SAARTHI announces `"Unknown number is calling"` without crashing.
   - `READ_CONTACTS` is required to query `ContactsContract.PhoneLookup`. If ungranted, unlisted numbers are treated as unknown.
   - `ANSWER_PHONE_CALLS` is required on API 26+ for `acceptRingingCall()` and on API 28+ for `endCall()`.
2. **Device Independence:**
   - Tested and verified on standard Android framework constructs.
   - No hidden non-public reflection or root binaries used.
   - All call states and caller names process 100% on-device.

