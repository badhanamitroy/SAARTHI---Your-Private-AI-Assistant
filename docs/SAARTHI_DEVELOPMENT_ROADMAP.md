# SAARTHI — Phased Development Gap Roadmap & Backlog

**Document Version:** 1.1.0  
**Date:** October 29, 2026  
**Project:** SAARTHI — Your Private AI Assistant  
**Repository Root:** `D:\SAARTHI - Your Private AI Assistant`  
**Android Root:** `D:\SAARTHI - Your Private AI Assistant\android`  

---

## 1. Roadmap Architecture & Prioritization Principles

This roadmap translates the forensic gaps and defects identified during audits into a sequenced, dependency-ordered engineering backlog.

### Milestone Status:
- **Stage 3A:** Battery & Flashlight Hardware Controls — `COMPLETE & VERIFIED`
- **Stage 3B:** Settings Shortcuts, Timer Engine, Help Registry — `COMPLETE & VERIFIED`
- **Stage 3C:** Build Recovery & Conversational Follow-Up — `COMPLETE & VERIFIED`
- **Stage 4 & 4.1:** Hands-Free Calling, Smart Caller Awareness, Speaker Defaults — `COMPLETE & VERIFIED`
- **Stage 4.1.1:** Test Evidence Forensic Audit & Real-Device Checklist — `COMPLETE & VERIFIED`
- **Stage 5A:** Hands-Free Voice Intelligence & Wake-Word Audit — `COMPLETE & VERIFIED`
- **Stage 5B:** Hybrid AI Architecture & Wake-Word Feasibility — `COMPLETE & VERIFIED`

### Priority Classifications:
- **P0 — Critical:** Compilation errors, app crashes, frozen UI states, or misleading action claims.
- **P1 — Core Assistant & Conversational Experience:** Command routing fixes, conversational multi-turn follow-ups, and natural continuous listening without repeated button taps.
- **P2 — Hands-Free Activation:** Investigation and trade-off evaluation for acoustic keyword spotting (KWS).
- **P3 — Phone Controls & System Persistence:** Persistent countdown timers backed by `AlarmManager`, Clock alarms, and Calendar intent providers.
- **P4 — Quality & Multilingual Expansion:** Bengali math operators, dynamic speech recognition locale configuration, release signing, and documentation alignment.
- **P5 — Future Capabilities:** Local offline notes and reminders, speech pitch/rate adjustments.

---

## 2. Prioritized Engineering Backlog

```
+-----+-------------------------------------------------------+-----------+-------------------------+
| PRI | ITEM / MILESTONE                                      | EFFORT    | PHYSICAL PHONE REQ?     |
+-----+-------------------------------------------------------+-----------+-------------------------+
| P0  | Fix Working Copy Lexical Scoping Compilation Failure  | Immediate | No (JVM build verifies) |
| P0  | Fix Uncommitted Test Method Overrides in Voice Test   | Immediate | No (JVM test verifies)  |
| P0  | Add TTS Watchdog & Muted State Fallback Guard         | High      | Yes (Audio check)       |
| P1  | Integrate Multi-Turn Follow-Up Context Engine         | High      | Yes (Voice follow-up)   |
| P1  | Enable Continuous In-Session Voice Turn-Taking        | High      | Yes (Audio loop)        |
| P1  | Formalize Direct-Toggle Settings Routing & Feedback   | Medium    | Yes (Intent check)      |
| P2  | Acoustic Wake-Word Engine Feasibility & Integration  | Very High | Yes (Audio/mic latency) |
| P3  | AlarmManager-Backed Timer Persistence Across Kills    | High      | Yes (Process kill test) |
| P3  | Android Clock / Alarms Provider Integration (Stage3C) | Medium    | Yes (Alarm app check)   |
| P3  | Android Calendar Contract Integration (Stage 3C)      | Medium    | Yes (Calendar app check)|
| P4  | SafeMathParser Bengali Operator Expansion             | Low       | No (JVM unit tests)     |
| P4  | Speech Recognition Bengali Locale Switcher            | Medium    | Yes (ASR engine check)  |
| P4  | Settings Screen Documentation & UI Reconciliation     | Low       | No (UI inspection)      |
| P4  | Release Keystore & Production Signing Configuration   | Low       | No (Build pipeline)     |
| P5  | Offline Local Notes & Reminders Subsystem             | High      | Yes (Storage & Alarm)   |
+-----+-------------------------------------------------------+-----------+-------------------------+
```

---

## 3. Detailed Milestone Specifications

### Milestone 1: P0 Stabilization & Build Restoration

#### Item 1.1: Resolve Lexical Scoping Compilation Failure in `MainActivity.kt`
- **Current Blocker:** `MainActivity.kt:211` fails compilation because `executeCommandAction` is called inside `remember { VoiceSessionCoordinator(...) }` before its lexical definition at line 298.
- **Relevant Files:** [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt)
- **Acceptance Criteria:**
  1. Move `fun executeCommandAction(...)` above the `val voiceSessionCoordinator = remember { ... }` block in `SaarthiApp`, or extract it as a private top-level / companion function.
  2. `.\gradlew.bat compileDebugKotlin` succeeds with 0 errors.
- **Physical Phone Required:** No.

#### Item 1.2: Correct Interface Implementation in `VoiceSessionCoordinatorTest.kt`
- **Current Blocker:** `FakeVoiceEngine.release()` attempts to override non-existent `VoiceEngine.release()`, and `FakeSpeechSynthesizer` fails to implement abstract method `isInitialized(): Boolean`.
- **Relevant Files:** [VoiceSessionCoordinatorTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/voice/VoiceSessionCoordinatorTest.kt)
- **Acceptance Criteria:**
  1. Remove `override` keyword from `FakeVoiceEngine.release()` or declare `fun release()` in `VoiceEngine.kt`.
  2. Implement `override fun isInitialized(): Boolean = true` in `FakeSpeechSynthesizer`.
  3. `.\gradlew.bat test` compiles and passes all unit tests.
- **Physical Phone Required:** No.

#### Item 1.3: TTS Watchdog Timer & Muted Speech State Recovery
- **Current Blocker:** If `TextToSpeech` drops the `onDone` callback or `isVoiceFeedbackEnabled` is false, `micState` remains stuck in `PROCESSING` or `RESPONDING`.
- **Relevant Files:** `TextToSpeechEngine.kt`, `MainActivity.kt`, `VoiceSessionCoordinator.kt`
- **Acceptance Criteria:**
  1. Introduce a coroutine watchdog in `deliverResponse()` that resets `state` to `IDLE` (or triggers `handleSpeechCompleted()`) if TTS does not complete within 10 seconds.
  2. If `isVoiceFeedbackEnabled == false`, immediately transition `micState` to `IDLE` without waiting for TTS callbacks.
- **Physical Phone Required:** Yes (verify on physical hardware with TTS engine).

---

### Milestone 2: P1 Core Assistant & Conversational Experience

#### Item 2.1: Multi-Turn Context Follow-Up Engine
- **Current Blocker:** Saying "Set a timer" followed by "5 minutes" falls through to `NeedsOnlineAI`.
- **Relevant Files:** `ConversationContext.kt`, `CommandEngine.kt`, `MainActivity.kt`, `AssistantForegroundService.kt`
- **Acceptance Criteria:**
  1. When "Set a timer" is spoken without duration, `CommandEngine` sets `pendingIntent = PendingIntent.TimerDuration`.
  2. In the subsequent turn, "5 minutes", "পাঁচ মিনিট", or "30 seconds" is correctly mapped to `CommandResult.SetTimer`.
  3. Context expires automatically after 30 seconds without hijacking unrelated commands.
  4. Unit test suite passes 100% of multi-turn scenarios.
- **Physical Phone Required:** Yes (verify conversational rhythm).

#### Item 2.2: Continuous In-Session Voice Turn-Taking
- **Current Blocker:** User must tap Talk button for every interaction in foreground UI.
- **Relevant Files:** `VoiceSessionCoordinator.kt`, `MainActivity.kt`
- **Acceptance Criteria:**
  1. Tapping Talk once starts a continuous voice session.
  2. After assistant completes speaking a response, speech recognition automatically re-arms for the user's next command.
  3. Session cleanly terminates if user says "stop", taps cancel, or after 2 consecutive silence timeouts.
- **Physical Phone Required:** Yes (microphone acoustic verification).

#### Item 2.3: Wi-Fi & Bluetooth Direct-Toggle Intent Routing
- **Current Blocker:** "Turn off Wi-Fi" and "Bluetooth bondho koro" fall through to `NeedsOnlineAI`.
- **Relevant Files:** `CommandEngine.kt`, `SystemSettingsController.kt`
- **Acceptance Criteria:**
  1. Regex patterns match "turn off/on wifi", "turn off/on bluetooth", "disable mobile data".
  2. Returns `CommandResult.OpenSystemSettings(settingType, isDirectToggleAttempt = true)`.
  3. Spoken feedback explains Android platform security restrictions and opens the relevant settings panel.
- **Physical Phone Required:** Yes (check Android system panel display).

---

### Milestone 3: P2 Hands-Free Activation & Acoustic Wake-Word Engine

#### Item 3.1: Acoustic Keyword Spotting (KWS) Feasibility & Model Packaging
- **Current Blocker:** "Hey SAARTHI" wake word requires continuous platform `SpeechRecognizer` execution, consuming heavy CPU/battery and lacking true acoustic spotting.
- **Relevant Files:** `AcousticWakeWordEngine.kt`, `app/build.gradle.kts`
- **Acceptance Criteria:**
  1. Evaluate an ultra-lightweight on-device KWS engine (e.g. TFLite Micro with a quantized 1.2 MB Mel-frequency model or openWakeWord ONNX).
  2. Keep release APK size strictly below the 5.0 MB hard limit (current APK size: 1.63 MB, providing ~3.37 MB headroom).
  3. Implement raw PCM 16kHz audio capture via `AudioRecord` with circular buffer feeding `processFrame()`.
  4. Preserve microphone mutual exclusion during TTS playback.
- **Physical Phone Required:** Yes (acoustic sensitivity and false-positive rate testing).

---

### Milestone 4: P3 System Persistence & Extended Phone Actions

#### Item 4.1: AlarmManager-Backed Timer Persistence
- **Current Blocker:** Coroutine timer dies if app process is swiped from Recents or killed by OEM battery savers.
- **Relevant Files:** `TimerController.kt`, `AndroidManifest.xml`, new `TimerAlarmReceiver.kt`
- **Acceptance Criteria:**
  1. When timer starts, calculate target trigger time (`System.currentTimeMillis() + durationMs`) and schedule an exact alarm with `AlarmManager.setExactAndAllowWhileIdle()`.
  2. Register a broadcast receiver (`TimerAlarmReceiver`) to post completion notification and play alarm ringtone even if app process is terminated.
  3. In-app Compose UI calculates remaining duration from target timestamp rather than relying solely on coroutine ticks.
- **Physical Phone Required:** Yes (verify survival across swipe-from-recents).

#### Item 4.2: Android Clock / Alarms Provider Integration (Stage 3C)
- **Current Blocker:** Stage 3C feature; currently Clock launcher only opens the clock app without setting specific alarms.
- **Relevant Files:** `AppLauncher.kt`, `CommandEngine.kt`, `IntentType.kt`
- **Acceptance Criteria:**
  1. Recognize commands like "Set an alarm for 7:00 AM" / "সকাল ৭টায় অ্যালার্ম দাও".
  2. Dispatch `AlarmClock.ACTION_SET_ALARM` with `EXTRA_HOUR`, `EXTRA_MINUTES`, and `EXTRA_SKIP_UI = true`.
- **Physical Phone Required:** Yes (verify OEM clock interaction).

#### Item 4.3: Android Calendar Contract Integration (Stage 3C)
- **Current Blocker:** Calendar launcher only opens calendar view without event insertion.
- **Relevant Files:** `AppLauncher.kt`, `CommandEngine.kt`
- **Acceptance Criteria:**
  1. Recognize commands like "Add calendar event Meeting tomorrow at 3 PM".
  2. Dispatch `Intent.ACTION_INSERT` with `CalendarContract.Events.CONTENT_URI`.
- **Physical Phone Required:** Yes.

---

### Milestone 5: P4 Quality, Multilingual Expansion & Release Hardening

#### Item 5.1: SafeMathParser Bengali Operator Expansion
- **Current Blocker:** `SafeMathParser.kt` supports Bengali digits `০-৯`, but fails on Bengali verbal operators (`যোগ`, `বিয়োগ`, `গুণ`, `ভাগ`).
- **Relevant Files:** `SafeMathParser.kt`, `CommandEngine.kt`
- **Acceptance Criteria:**
  1. Preprocess Bengali words: `"যোগ"` $\rightarrow$ `"+"`, `"বিয়োগ"` $\rightarrow$ `"-"`, `"গুণ"` $\rightarrow$ `"*"`, `"ভাগ"` $\rightarrow$ `"/"`.
  2. Add unit tests in `CommandEngineTest.kt` for queries like `"২৫ যোগ ১৭"`.
- **Physical Phone Required:** No (100% unit test verifiable).

#### Item 5.2: Speech Recognition Dynamic Locale Switching
- **Current Blocker:** `AndroidSpeechEngine.kt` leaves `EXTRA_LANGUAGE` to OS default, preventing Bengali voice recognition on English-configured phones.
- **Relevant Files:** `AndroidSpeechEngine.kt`, `SettingsScreen.kt`
- **Acceptance Criteria:**
  1. Add language toggle in Settings (English / Bangla / Auto).
  2. Inject `RecognizerIntent.EXTRA_LANGUAGE = "bn-BD"` or `"en-US"` into the recognition intent.
- **Physical Phone Required:** Yes.

#### Item 5.3: Settings Documentation & Resource Reconciliation
- **Current Blocker:** Documentation claims wake sensitivity and voice pitch/speed sliders that do not exist.
- **Relevant Files:** `docs/SAARTHI_PROJECT_CHANGELOG.md`, `docs/SAARTHI_FILE_CHANGE_INDEX.md`, `SettingsScreen.kt`
- **Acceptance Criteria:**
  1. Update documentation to accurately reflect existing Settings features, or implement TTS pitch/speed controls using `TextToSpeech.setPitch()` and `TextToSpeech.setSpeechRate()`.
- **Physical Phone Required:** No.

