# SAARTHI — Stage 3C: Critical Build Recovery, Voice Reliability & Conversation Continuity Implementation Report

**Document Version:** 1.0.0  
**Date:** October 28, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  
**Milestone:** Stage 3C ("Critical Build Recovery, Voice Reliability & Conversation Continuity")  
**Execution Status:** FULLY IMPLEMENTED, AUTOMATED-TESTED, BUILD-VERIFIED  
**Device Verification Status:** NOT VERIFIED (No physical Android device or emulator connected during this headless verification cycle)

---

## 1. Executive Summary & Objective

Stage 3C addresses the critical build failures, conversational gaps, and voice interaction friction identified during the comprehensive Stage 3B feature inventory and readiness audit. Prior to this stage, a user attempting hands-free voice interaction had to repeatedly tap the microphone button after every single assistant response, conversational follow-ups (such as answering "5 minutes" to "How long should I set the timer for?") fell into the generic offline fallback, and compilation failures prevented release builds.

### Core Objectives Delivered:
1. **P0: Compilation & Test Integrity Restored:** Resolved Kotlin compilation failures in `MainActivity.kt` (local lexical function scoping) and interface drift in `VoiceEngine`, `SpeechSynthesizer`, and `SystemSettingsLauncher`.
2. **P1: Conversational Follow-Up Engine:** Integrated `ConversationContext` into `CommandEngine` and `VoiceSessionCoordinator`. Multi-turn dialogues (e.g. "Set a timer" $\rightarrow$ "How long?" $\rightarrow$ "5 minutes") resolve against active pending intents instead of falling into `NeedsOnlineAI`.
3. **P1: Turn-Taking Continuous Voice Sessions:** Built `VoiceSessionCoordinator` implementing a complete turn-taking lifecycle: automatic post-speech listening without tapping Talk, mutual exclusion audio ducking (microphone stops during TTS playback to avoid audio feedback), and explicit session termination.
4. **P1: Responding/Speaking Stuck State Prevention:** Added dynamic speech watchdog timers in `TextToSpeechEngine` and direct state bypass when voice feedback is muted, ensuring the UI never gets stuck in `Speaking...` or `Responding...` if an OEM TTS drops completion callbacks.
5. **P1: Truthful Wi-Fi & System Settings Routing:** Routed toggle commands ("Turn off Wi-Fi", "ওয়াইফাই বন্ধ করো", "Wi-Fi off koro") to official Android system panels (`Settings.Panel.ACTION_WIFI`) with honest spoken feedback clarifying Android platform restrictions.
6. **P2: Reality Verification on Wake-Word & Timer Persistence:** Conducted an exhaustive, truthful audit documenting that wake-word detection is text-matched via platform `SpeechRecognizer` (no on-device acoustic KWS model) and timer countdowns are in-memory coroutines (vulnerable to process termination without `AlarmManager`).
7. **Verification & Quality:** 148 automated unit tests across 10 test suites pass with 100% success (0 failures, 0 skipped). Release APK assembled cleanly at **1,644,697 bytes** (well under the 5,000,000-byte hard budget).

---

## 2. Root-Cause Analysis of Baseline Issues

### 2.1 Kotlin Compilation Failure in `MainActivity.kt`
- **Symptom:** `compileDebugKotlin` failed with `Unresolved reference 'executeCommandAction'` at `MainActivity.kt:211`.
- **Root Cause:** In Kotlin Jetpack Compose, local functions declared inside `@Composable fun SaarthiApp` are strictly lexically scoped. `voiceSessionCoordinator` was initialized via `remember { ... }` prior to the definition of `executeCommandAction`, which was located further down in the file (around line 448).
- **Fix:** Relocated `executeCommandAction` above `voiceSessionCoordinator` in `MainActivity.kt` and eliminated the redundant lower copy.

### 2.2 Test Interface Mismatches
- **Symptom:** `VoiceSessionCoordinatorTest.kt` failed with `'release' overrides nothing` and `Class 'FakeSpeechSynthesizer' does not implement abstract member 'isInitialized'`. Furthermore, `FakeVoiceEngine` had a JVM platform declaration clash between `var isListening` and `fun isListening()`.
- **Root Cause:** Interface declarations in `VoiceEngine.kt` and `SpeechSynthesizer.kt` drifted from test doubles and real implementations (`AndroidSpeechEngine.kt` had `fun release()` without it being in the interface; `SpeechSynthesizer` declared `fun isInitialized()`, missing in the test fake).
- **Fix:** Standardized `VoiceEngine` (`fun isListening(): Boolean`, `fun release()`), `SpeechSynthesizer` (`fun isInitialized(): Boolean`), and harmonized `SystemSettingsLauncher` interface methods with default delegations.

### 2.3 Broken Multi-Turn Conversational Follow-Up
- **Symptom:** Saying "Set a timer" prompted "How long should I set the timer for?", but replying "5 minutes" yielded "I cannot do that offline. Offline capabilities include...".
- **Root Cause:** `CommandEngine.process(rawInput)` did not evaluate against conversational state. When "5 minutes" was received, the duration parser was only evaluated when preceded or followed by the keyword "timer". Without the keyword, the input was treated as unknown and dispatched to `CommandResult.NeedsOnlineAI`.
- **Fix:** Integrated `ConversationContext` holding `PendingIntent.TimerDuration`. Follow-up utterances are checked against active pending intents first. If a duration is found, it starts the timer; if cancelled ("বাতিল", "bad dao", "cancel", "lagbe na"), it cancels; if an unrelated command is spoken ("what time is it"), it clears the context and executes the new command.

### 2.4 Lack of Continuous Turn-Taking Voice Sessions
- **Symptom:** Users had to tap the Talk button before every single command. After SAARTHI spoke a response, the microphone shut down and required another manual tap.
- **Root Cause:** Voice listening was designed as a single-shot transaction rather than a turn-taking state machine.
- **Fix:** Introduced `VoiceSessionCoordinator.kt`, which coordinates the voice session from start to finish. Upon receiving `onSpeechDone` from TTS, it automatically resumes listening (`startListeningInternal()`), enabling continuous conversation until the user says "stop", cancels, or 2 consecutive silence timeouts occur.

### 2.5 Stuck UI Response States
- **Symptom:** On certain OEM Android devices, or when voice feedback was muted in settings, the assistant UI could remain indefinitely frozen in `Speaking...` or `Responding...`.
- **Root Cause:** When `isVoiceFeedbackEnabled == false`, `deliverResponse()` called TTS anyway or failed to trigger completion callbacks. Additionally, some OEM TTS engines silently drop `UtteranceProgressListener.onDone` for short phrases or when audio focus is contested.
- **Fix:** In `VoiceSessionCoordinator.kt`, if voice feedback is muted or the feedback string is empty, the coordinator transitions immediately to `handleSpeechCompleted()`. In `TextToSpeechEngine.kt`, a defensive Handler watchdog runnable was added with dynamic timeout ($\text{length} \times 120\text{ ms}$, bounded between 4s and 25s) to guarantee recovery if an OEM drops the completion callback.

---

## 3. Technical Implementation Details

### 3.1 Architecture Overview

```
                               ┌────────────────────────────────┐
                               │     MainActivity / Compose     │
                               └───────────────┬────────────────┘
                                               │
                                 startSession()│ (Turn-taking)
                                               ▼
                              ┌──────────────────────────────────┐
                              │     VoiceSessionCoordinator      │
                              └───────┬───────────────────┬──────┘
                                      │                   │
                        audio ducking │                   │ transcript
                                      ▼                   ▼
     ┌───────────────────────┐  ┌─────────────┐   ┌────────────────────────┐
     │   SpeechSynthesizer   │  │ VoiceEngine │   │     CommandEngine      │
     │  (TextToSpeechEngine) │  │  (Android)  │   │  (ConversationContext) │
     └───────────────────────┘  └─────────────┘   └───────────┬────────────┘
                 ▲                                            │
                 │ watchdog fallback                          ▼
                 └─────────────────────────────────── Action Execution
                                                      (Timer / Hardware /
                                                       Settings / History)
```

### 3.2 Conversational Context Engine (`ConversationContext.kt` & `CommandEngine.kt`)
- **State Model:** `ConversationContext` maintains:
  - `pendingIntent`: `PendingIntent.TimerDuration(originalQuery, createdAtMs)` or `PendingIntent.None`.
  - `activeFollowUp`: boolean flag indicating whether the assistant is waiting for user parameter resolution.
  - `timeoutMs`: 30-second context window. Follow-up answers received after 30 seconds expire cleanly.
- **Trilingual Parameter Parsing:**
  - Standard digits (`5`, `10`).
  - English number words (`one`, `five`, `thirty`).
  - Bengali script numbers (`৫`, `১০`, `পনেরো`, `বিশ`).
  - Banglish phonetic numbers (`ek`, `panch`, `dosh`, `ponero`).
  - Standalone numbers (e.g. "10") during a pending timer follow-up default to minutes (`600L` seconds).
- **Cancellation & Isolation Safeguards:**
  - Recognizes English ("cancel", "stop", "nevermind", "abort", "dismiss"), Bengali ("বাদ দাও", "থাক", "দরকার নেই", "বন্ধ করো", "ক্যান্সেল", "বাতিল"), and Banglish ("bad dao", "thak", "dorkar nei", "cancel koro", "lagbe na").
  - If an unrelated known command is received (e.g., "what time is it", "battery koto ache", "torch on koro"), the pending follow-up is immediately cleared and the new command executes without delay.

### 3.3 Voice Session Turn-Taking Lifecycle (`VoiceSessionCoordinator.kt`)
- **State Machine:**
  - `IDLE`: Assistant waiting for user invocation.
  - `LISTENING`: Microphone open and streaming to speech recognizer.
  - `PROCESSING`: Transcript received; evaluating intent via `CommandEngine`.
  - `EXECUTING`: Local hardware, timer, or settings contract executing.
  - `SPEAKING`: Text-to-speech audio synthesizing; microphone muted to avoid feedback.
  - `AWAITING_FOLLOW_UP`: Follow-up question spoken; listening for parameter.
  - `ERROR`: Recoverable or unrecoverable error state.
- **Mutual Exclusion Ducking:** The microphone is actively stopped (`voiceEngine.stopListening()`) before TTS begins, preventing SAARTHI from listening to its own voice output.
- **Automatic Turn Resumption:** When TTS completes (`onSpeechDone`), `handleSpeechCompleted()` evaluates `isSessionActive`. If active, it smoothly transitions back to `LISTENING` (or `AWAITING_FOLLOW_UP`).
- **Debounce Protection:** Rapid duplicate recognition callbacks (within 1,200 ms) are dropped to prevent executing duplicate actions.
- **Bounded Silence Backoff:** Bounded retry counter (`maxConsecutiveSilences = 2`). If 2 consecutive silences occur with no user input, the session cleanly transitions to `IDLE` rather than looping indefinitely.

### 3.4 Speech Watchdog & Audio Recovery (`TextToSpeechEngine.kt`)
- Attached an automated watchdog to `mainHandler` upon queuing utterances:
  ```kotlin
  val timeoutMs = (speechText.length * 120L).coerceIn(4000L, 25000L)
  val watchdog = Runnable {
      watchdogRunnable = null
      onSpeechDone?.invoke()
  }
  watchdogRunnable = watchdog
  mainHandler.postDelayed(watchdog, timeoutMs)
  ```
- Cancelled immediately whenever `onDone`, `onError`, `onStop`, or `stop()` is triggered.
- If an OEM engine fails to report completion, the watchdog fires and cleanly restores conversation flow.

### 3.5 Truthful Wi-Fi & System Settings Routing (`SystemSettingsController.kt`)
- All direct Wi-Fi toggle commands ("turn off wifi", "turn wifi off", "ওয়াইফাই বন্ধ করো", "wifi off koro") route directly to `CommandResult.OpenSystemSettings(SystemSettingType.WIFI, isDirectToggleAttempt = true)`.
- Dispatches Android's official system panel: `Intent(Settings.Panel.ACTION_WIFI)` on Android 10+ (API 29+) with fallback to `Settings.ACTION_WIFI_SETTINGS`.
- Truthful Spoken Feedback:
  > *"Opening Wi-Fi settings. Please tap the Wi-Fi toggle, as Android restricts apps from toggling Wi-Fi directly."*

---

## 4. Deep Reality Verification on Wake-Word & Timer Persistence

As mandated by project integrity requirements, the implementation audit investigated two documented capabilities to separate reality from design claims:

### 4.1 Reality of Hands-Free Wake-Word ("Hey SAARTHI")
- **Claim in older docs:** "On-device acoustic wake word detection model running locally."
- **Source Code Truth:**
  - `AcousticWakeWordEngine.kt` is merely an empty Kotlin interface.
  - No `.tflite`, `.onnx`, Porcupine, or snowboy acoustic model binary exists in `app/src/main/assets` or `res/raw`.
  - Wake-word detection is performed by `LocalWakeWordDetector.kt` using standard Android platform `SpeechRecognizer`. It listens for speech, transcribes it to text, and evaluates regex patterns:
    `^(hey saarthi|ok saarthi|okay saarthi|namaste saarthi|saarthi)`
- **Impact & Honest Assessment:**
  - Requires continuous microphone streaming to Android's platform speech service.
  - Consumes significantly more battery than a low-power hardware-accelerated DSP keyword spotting (KWS) model.
  - Subject to platform background execution limits when the screen is turned off.

### 4.2 Reality of Timer Persistence
- **Claim in older docs:** "Timer runs in background and rings when done."
- **Source Code Truth:**
  - `TimerController.kt` manages countdown state in memory via a Kotlin Coroutine job (`tickerJob = coroutineScope.launch { ... delay(1000) ... }`).
  - Does NOT register alarms with Android OS `AlarmManager` (`setExactAndAllowWhileIdle()`).
  - Does NOT schedule deferred background work with `WorkManager`.
- **Impact & Honest Assessment:**
  - If the application remains in memory or `AssistantForegroundService` is active with a foreground notification, the timer counts down and rings.
  - If the user swipes SAARTHI from the Android Recent Apps screen and the OS terminates the process under low-memory conditions, the in-memory coroutine dies and the timer will fail to fire.

---

## 5. Automated Test Suite Verification

All 10 unit test suites were compiled and executed via `.\gradlew.bat testDebugUnitTest` with 100% pass rate:

| Test Suite | Tests Run | Failures | Skipped | Time (s) | Status |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `com.saarthi.assistant.core.CommandEngineTest` | 58 | 0 | 0 | 0.321 | **PASSED** |
| `com.saarthi.assistant.core.DeviceHardwareControllerTest` | 14 | 0 | 0 | 0.009 | **PASSED** |
| `com.saarthi.assistant.core.ExecutionContractTest` | 7 | 0 | 0 | 0.005 | **PASSED** |
| `com.saarthi.assistant.core.SystemSettingsControllerTest` | 14 | 0 | 0 | 0.040 | **PASSED** |
| `com.saarthi.assistant.core.TimerControllerTest` | 19 | 0 | 0 | 0.113 | **PASSED** |
| `com.saarthi.assistant.voice.SpeechPronunciationFormatterTest` | 7 | 0 | 0 | 0.003 | **PASSED** |
| `com.saarthi.assistant.voice.VoiceSessionCoordinatorTest` | 8 | 0 | 0 | 0.010 | **PASSED** |
| `com.saarthi.assistant.wakeword.NaturalVoiceSessionTest` | 11 | 0 | 0 | 0.013 | **PASSED** |
| `com.saarthi.assistant.wakeword.WakeWordCoordinatorTest` | 9 | 0 | 0 | 0.004 | **PASSED** |
| `com.saarthi.assistant.ExampleUnitTest` | 1 | 0 | 0 | 0.000 | **PASSED** |
| **Total** | **148** | **0** | **0** | **0.518** | **100% PASS** |

### Key Scenarios Automated:
- `scenarioA_timerContinuity_resolvesFollowUpAndResumesListening`: Full multi-turn session ("Set a timer" $\rightarrow$ "How long?" $\rightarrow$ "5 minutes" $\rightarrow$ "Timer set for 300 seconds" $\rightarrow$ auto re-listen).
- `scenarioB_multiCommandTurnTaking_seamlesslyHandlesBackToBackRequests`: Battery status check immediately followed by flashlight toggle without tapping Talk.
- `scenarioC_wifiCommand_routesDirectlyWithHonestExplanation`: Truthful platform restriction feedback verification.
- `scenarioD_sessionControl_startStopRestartIdempotence`: Start, stop, cancel, and idempotent re-invocation.
- `scenarioE_silenceTimeout_retriesBoundedThenCleanlyStops`: Bounded silence backoff avoiding infinite loops.
- `scenarioF_contextIsolation_unrelatedCommandClearsFollowUp`: Unrelated commands ("what time is it") isolate context and do not trigger timer creation.
- `scenarioG_duplicateCallbacks_debouncesRapidRepeats`: Rapid duplicate speech recognizer callbacks suppressed.
- `stage3c_timerFollowUp_cancellationPhrases_clearsContext`: Verification of Bengali and Banglish cancellation phrases ("বাতিল", "lagbe na", "বাদ দাও").

---

## 6. Build & Binary Size Verification

Both Debug and Release APKs were compiled and verified:

```
> Task :app:compileReleaseKotlin
> Task :app:minifyReleaseWithR8
> Task :app:optimizeReleaseResources
> Task :app:packageRelease
> Task :app:assembleRelease
BUILD SUCCESSFUL in 3m 11s
```

### Artifact Inspection:

| Artifact | Path | Size (Bytes) | Size (MB) | Budget Limit | Status |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | 13,355,757 | 12.74 MB | N/A | Valid |
| **Release APK** | `app/build/outputs/apk/release/app-release.apk` | **1,644,697** | **1.57 MB** | < 5,000,000 bytes | **PASSED (32.9% of budget)** |

---

## 7. Device Verification Status

- **Status:** **NOT VERIFIED ON PHYSICAL HARDWARE**
- **Hardware Connection Audit:** Querying `adb devices` in this host development environment confirmed that no physical Android handset or active emulator instance was connected.
- **Mitigation:** Comprehensive automated unit tests (148 tests) using realistic mocked Android contexts, robuster Android system panel intent builders, and pure-logic state machine coordinators verify all behavioral contracts. Physical testing must be scheduled when target hardware is connected.

---

## 8. Development Roadmap & Next Steps

Based on the verified reality of the codebase, the following items are recommended for the next development milestones:

1. **Stage 4A — AlarmManager Integration for Timer Persistence (P1):**
   - Refactor `TimerController` to register alarms with Android `AlarmManager.setExactAndAllowWhileIdle()` and a broadcast receiver `TimerExpiredReceiver`.
   - Ensure timers reliably ring even if the user closes SAARTHI or the OS kills the background process.
2. **Stage 4B — True Offline Acoustic Wake-Word Engine (P2):**
   - Package a quantized, ultra-low-power TensorFlow Lite (or ONNX) keyword spotting model for "Hey SAARTHI".
   - Decouple wake-word detection from full speech recognition to reduce CPU and battery consumption.
3. **Stage 4C — Bengali Speech Pack & Offline TTS Guidance (P2):**
   - Provide in-app guidance directing users to download Android's offline Bengali speech recognition pack and offline TTS voice data in device settings.

