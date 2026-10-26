# SAARTHI — Natural Voice UX & Reliable Action Execution Implementation Report

**Document Version:** 1.0.0  
**Phase:** Natural Voice UX & Action Reliability  
**Date:** October 25, 2026  
**Package:** `com.saarthi.assistant`  
**Target SDK:** 36 | **Compile SDK:** 36 | **Min SDK:** 26  

---

## 1. Executive Summary & Verification Matrix

SAARTHI has been transformed from a button-driven voice-command application into an offline-first Android voice assistant inspired by natural assistant interaction models (e.g. JARVIS), respecting Android's real-world background-execution and audio restrictions.

```
+-----------------------------------------------------------------------------------------+
| VERIFICATION GATE                                              | RESULT                |
+-----------------------------------------------------------------------------------------+
| Automated Unit Tests (59/59 passed, 0 failures, 0 errors)       | PASSED                |
| Debug APK Build (`assembleDebug`)                               | PASSED                |
| Release APK Build (`assembleRelease`) with R8 Optimization      | PASSED                |
| Measured Release APK Byte Size                                  | 1,365,761 bytes       |
| Hard Constraint Limit (< 5,000,000 bytes)                       | PASSED (3.63 MB free) |
| Standby Ambient Speech Rejection (No Wake Word)                 | VERIFIED              |
| Post-Wake Listening Window (No Tap Required)                   | VERIFIED              |
| Post-Wake Inactivity Timeout (Quiet Standby Return)            | VERIFIED              |
| Duplicate Rapid Execution Debouncing Guard                      | VERIFIED              |
| Sensitive Action Confirmation (Call/Data Protection)            | VERIFIED              |
| Action Execution Contract (Accepted/Succeeded/Failed)           | VERIFIED              |
| Honest Wording (No False Visibility Claims)                     | VERIFIED              |
| Zero Cloud / Remote AI Dependencies (Zero Network Permission)   | VERIFIED (Pure Local) |
| Physical-Device Verification                                    | BLOCKED (No hardware) |
+-----------------------------------------------------------------------------------------+
```

---

## 2. What Was Broken and Its Evidence

### 2.1 Mandatory Tap-to-Talk Friction
- **Observation:** In the foreground UI, users had to press the `"🎙 Talk to SAARTHI"` button for every single interaction. Once a single utterance completed or timed out, recognition shut off, forcing another tap.
- **Root Cause:** `AndroidSpeechEngine` wraps Android's one-shot `SpeechRecognizer`. `MainActivity` had no continuous listening or coordinator state machine.

### 2.2 Accidental Command Execution from Ambient Conversation
- **Observation:** In `AssistantForegroundService.kt`, speech input was continuously transcribed and routed directly to `commandEngine.process(text)`.
- **Evidence:** If background noise, TV audio, or an nearby person spoke `"what time is it"` or `"calculator"`, SAARTHI immediately executed the command or spoke aloud without any wake word having been spoken.

### 2.3 Premature & Dishonest App Launch Claims
- **Observation:** When the user said `"open calculator"`, SAARTHI immediately announced `"Opening calculator."` and claimed `"Launched Calculator"`, but on modern Android versions (API 29–36) the target application frequently did not appear on screen.
- **Root Cause:**
  1. `context.startActivity(launchIntent)` is an asynchronous IPC message to Android's `ActivityTaskManagerService`. It does NOT guarantee that the activity window became visible.
  2. Android 10+ (API 29+) Background Activity Launch (BAL) restrictions strictly block background Services from launching activities without a visible window or user interaction token. Calling `startActivity` from a background service is often silently ignored or delayed by the OS.
  3. `AppLauncher.kt` caught generic `Exception` and did not categorize `ActivityNotFoundException` or `SecurityException`.
  4. The assistant prematurely claimed certainty before Android could even process the intent.

### 2.4 Unprotected Sensitive Actions
- **Observation:** Background speech containing the word `"call"` could potentially trigger dialing actions without the user's explicit consent.

---

## 3. What Was Changed and Why

### 3.1 Gated Wake-Word Standby & Natural Post-Wake Window
- **Standby Isolation:** When SAARTHI is in `LISTENING_FOR_WAKE_WORD`, incoming utterances are evaluated via `LocalWakeWordDetector.extractWakeWordAndCommand(text)`. Utterances lacking a wake phrase (`"Hey SAARTHI"`, `"Ok SAARTHI"`, `"SAARTHI"`) are classified as `UtteranceRoutingResult.Ignored` and discarded silently without action or speech.
- **Two-Stage Wake (`WakeWordState.LISTENING_FOR_COMMAND`):** If the user says `"Hey SAARTHI"` alone:
  1. SAARTHI transitions to `LISTENING_FOR_COMMAND`.
  2. Acknowledges briefly with `"I'm listening."` (if voice feedback is enabled).
  3. Opens the post-wake recognition session **without requiring any button tap**.
  4. Starts a 6-second post-wake inactivity timer (`POST_WAKE_TIMEOUT_MS = 6000L`).
  5. The user can speak the command naturally (e.g. `"what time is it"`, `"open camera"`).
  6. If no command is spoken within 6 seconds, SAARTHI quietly returns to standby without error noise.
- **Single-Utterance Direct Execution:** If the user speaks `"Hey SAARTHI, open calculator"` in one phrase, the wake word is verified, the command is extracted, and execution begins immediately.

### 3.2 Action Execution Contract (`ExecutionContract.kt`)
Replaced naive boolean/string returns with a formal state machine:
- `ExecutionContract.Accepted`: Command parsed and action dispatch is about to be attempted.
- `ExecutionContract.Succeeded`: Intent resolution verified and request dispatched to Android OS.
- `ExecutionContract.Failed`: Categorized by `ExecutionFailureType` (`APP_NOT_FOUND`, `ACTIVITY_NOT_FOUND`, `SECURITY_RESTRICTION`, `BACKGROUND_START_RESTRICTED`, `EVALUATION_ERROR`, `UNSUPPORTED_OFFLINE`, `CANCELLED`).
- `ExecutionContract.NeedsClarification`: Ambiguous command (e.g. single word `"calculator"` or `"date or time"`).
- `ExecutionContract.NeedsConfirmation`: Sensitive or consequential action requiring affirmative authorization.

### 3.3 Honest Wording & Error Transparency
- `AppLauncher.execute()` verifies `launchIntent.resolveActivity(packageManager)` before calling `startActivity()`.
- Explicitly catches `ActivityNotFoundException` and `SecurityException`.
- Replaced premature certainty (`"Opening calculator."` / `"Launched Calculator"`) with honest wording:
  - Display: `"Request sent: Calculator"`
  - Speech: `"I sent the request to open Calculator."`
- Informs the user when launch fails and provides a clear offline alternative (e.g. `"No compatible calculator app was found on your phone. You can say 'calculate' followed by your numbers to use the built-in calculator."`).

### 3.4 Sensitive Action Authorization (`SensitiveAction.kt`)
- Commands requesting phone calls (e.g. `"call mom"`, `"call John"`) or data clearing (`"clear history"`) do not execute immediately.
- SAARTHI responds with:
  `"You asked to call mom. Are you sure you want to place this call? Say yes to confirm or cancel to abort."`
- The coordinator enters `WakeWordState.AWAITING_CONFIRMATION`:
  - Explicit affirmative (`"yes"`, `"confirm"`, `"haan"`): confirms the action.
  - Negative or unrelated speech (`"no"`, `"cancel"`, `"stop"`): cleanly aborts and returns to standby.

### 3.5 Mutual Exclusion & Duplicate Prevention
- **TTS Feedback Loop Guard:** When TextToSpeech begins speaking (`onSpeechStart`), `speechEngine.stopListening()` is called immediately to prevent audio output from being picked up as a new voice command. After speech completes (`onSpeechDone`), a 400ms cooldown elapses before resuming the microphone.
- **Rapid Debounce Guard:** If an identical command is dispatched within 1200ms (`duplicateDebounceMs`), it is classified as `UtteranceRoutingResult.DuplicateDropped` to prevent double-launches.

### 3.6 User Controls
- Added `isVoiceFeedbackEnabled` toggle accessible in the Compose UI and service actions (`ACTION_TOGGLE_VOICE`). When muted, SAARTHI executes commands and updates the persistent notification/UI without playing TTS audio.

---

## 4. Whether Real Offline Wake-Word Inference Exists

### Technical Reality & Gap Disclosure:
1. **Current State:** A dedicated acoustic deep-learning model (such as an embedded ONNX runtime or TensorFlow Lite Micro model running keyword spotting directly on raw PCM audio buffers) **does NOT currently exist in the SAARTHI APK**.
2. **Why:** Bundling a dedicated acoustic KWS neural network (e.g. openWakeWord or Vosk) along with native C++ inference `.so` libraries for 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) requires between 2.5 MB and 12 MB of binary assets, which would threaten the strict 5,000,000-byte release limit.
3. **What SAARTHI Actually Uses:** SAARTHI implements a deterministic, offline wake-phrase gating state machine (`LocalWakeWordDetector` + `WakeWordCoordinator`) on top of Android's on-device `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE = true`).
4. **Honesty Assurance:** The codebase makes **zero claim** that acoustic DSP neural inference is currently compiled. `LocalWakeWordDetector` explicitly documents this boundary.

---

## 5. How Wake-Word Detection Supplies Audio to the Inference Engine

The architectural contract for real acoustic model streaming is formalized in `AcousticWakeWordEngine.kt`:
```mermaid
graph TD
    Mic[Physical Microphone] --> AR[android.media.AudioRecord]
    AR --> RingBuffer[16kHz 16-bit Mono PCM Circular Buffer]
    RingBuffer --> Stride[80ms Windows / 1280 Samples]
    Stride --> Feat[Feature Extractor: 32-bin Mel Spectrogram]
    Feat --> Engine[AcousticWakeWordEngine.processFrame]
    Engine --> Conf{Confidence >= 0.75?}
    Conf -->|Yes| WakeTrigger[WakeWordCoordinator.onWakeWordDetected]
    Conf -->|No| Discard[Drop Frame]
```

When an openWakeWord TFLite Micro model is integrated in a future phase:
1. `AudioRecord` reads 16,000 Hz, 16-bit linear PCM mono audio in 1280-sample chunks.
2. `processFrame(pcmBuffer, offset, length)` runs streaming tensor inference.
3. Upon probability exceeding the threshold (e.g. 0.75), `WakeWordCoordinator` is notified to open the post-wake command window.

---

## 6. How the Service Operates When the Screen Is Off & Android Limitations

Operating a continuous voice assistant on Android 14+ (API 34) and Android 16 (API 36) involves significant platform-enforced constraints:

1. **Foreground Service Requirement:**
   Declared with `android:foregroundServiceType="microphone"` and requires both `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE` permissions.
2. **Persistent Notification:**
   Ongoing notification in channel `saarthi_assistant_channel` with low importance to avoid chime spam.
3. **Screen-Off Audio HAL Restrictions:**
   On budget OEM devices (e.g. Samsung Galaxy A03 with Unisoc processor), when the screen locks, the hardware audio abstraction layer (HAL) enters deep sleep to prevent battery drain. Without specialized vendor DSP hardware wake locks, the microphone input may stop delivering frames.
4. **Background Activity Launch (BAL) Restrictions:**
   Android strictly blocks background services from launching activities (`startActivity`) if the app does not have a visible window. To respect this honestly:
   - SAARTHI issues the intent with `FLAG_ACTIVITY_NEW_TASK`.
   - Informs the user honestly: `"I sent the request to open Calculator."`
   - When blocked by BAL, the persistent notification provides a single tap to return to SAARTHI.

---

## 7. How Command Execution Reports Success and Failure

The execution path follows strict verification:
```
User Spoken Input
       │
       ▼
Wake-Word Verification (Standby Check)
       │
       ▼
CommandEngine.process(normalized)
       │
       ├── Sensitive Action ──────► NeedsConfirmation ──► User says "Yes" / "No"
       ├── Ambiguous Input  ──────► NeedsClarification ─► Spoken question
       └── Launch / Action  ──────► AppLauncher.execute()
                                           │
                        ┌──────────────────┴──────────────────┐
                        ▼                                     ▼
                Intent Resolved?                      No Target Found
                        │                                     │
                 ┌──────┴──────┐                              ▼
                 ▼             ▼                     ExecutionContract.Failed
            startActivity   SecurityException        (Spoken fallback message)
                 │             │
                 ▼             ▼
       ExecutionContract.   ExecutionContract.
           Succeeded             Failed
       ("Request sent")     ("Permission denied")
```

---

## 8. Preventing Accidental Commands and Sensitive Actions

| Risk | Previous Vulnerability | Implemented Mitigation |
|---|---|---|
| **Ambient Conversation** | Un-gated speech recognition processed any nearby voice. | In `LISTENING_FOR_WAKE_WORD`, all speech lacking `"Hey SAARTHI"` is dropped with `UtteranceRoutingResult.Ignored`. |
| **Accidental Phone Calls** | Speech containing a contact name could trigger a dial request. | Direct `CALL` intents trigger `SensitiveAction.PlacePhoneCall` and require explicit verbal confirmation (`"Yes"` / `"Cancel"`). |
| **Acoustic Self-Triggering** | Assistant's TTS voice was picked up by its own microphone. | Mutual exclusion: `speechEngine.stopListening()` is enforced while TTS speaks, followed by a 400ms cooldown. |
| **Rapid Double-Taps/Repeats** | Repeated speech results triggered duplicate app launches. | 1200ms debouncing window drops identical consecutive commands. |
| **Lingering Listening Sessions** | Dictation ran indefinitely without speech. | 6-second post-wake inactivity timer quietly returns SAARTHI to standby. |

---

## 9. Test Results, Build Results, and APK Size

### 9.1 Unit-Test Results
Command: `./gradlew.bat test`
```
BUILD SUCCESSFUL in 6s
59 tests completed, 0 failed, 0 errors
```

- `CommandEngineTest`: 32 tests passed (Math parsing, compound intents, ambiguity checks, offline help, time, date, greetings).
- `WakeWordCoordinatorTest`: 9 tests passed (Mutual exclusion, handoffs, pausing, resource cleanup).
- `ExecutionContractTest`: 7 tests passed (Contract states, failure types, individual target app fallbacks).
- `NaturalVoiceSessionTest`: 11 tests passed (Standby isolation, post-wake window, timeouts, debouncing, sensitive action confirmation, cancel flows, TTS exclusion).

### 9.2 Release APK Build & Exact Byte Size
Command: `./gradlew.bat assembleRelease`
- Build status: `BUILD SUCCESSFUL in 1m 21s`
- Artifact path: `android/app/build/outputs/apk/release/app-release.apk`
- **Measured Size: 1,365,761 bytes (1.37 MB)**
- **Budget: 5,000,000 bytes**
- **Margin: 3,634,239 bytes (~3.63 MB free)**

### 9.3 Debug APK Build
Command: `./gradlew.bat assembleDebug`
- Build status: `BUILD SUCCESSFUL in 7s`
- Artifact path: `android/app/build/outputs/apk/debug/app-debug.apk`
- Measured Size: 12,796,247 bytes

---

## 10. Behaviors Requiring Physical Android Device Verification

Querying `adb devices -l` showed zero attached devices or emulators. Physical device verification remains **BLOCKED / PENDING HARDWARE**.

### Physical Verification Protocol for Samsung Galaxy A03:
1. `adb install android/app/build/outputs/apk/release/app-release.apk`
2. Launch SAARTHI and tap `"🎙 Enable Permissions"` to grant `RECORD_AUDIO` and `POST_NOTIFICATIONS`.
3. Tap `"▶ Start Background Assistant"`. Verify persistent notification appears in status bar.
4. Speak `"Pass the salt"` nearby. Verify notification remains in standby (`"Listening for 'Hey SAARTHI'..."`) and no response is spoken.
5. Say `"Hey SAARTHI"`. Verify status transitions to `"Listening for command..."` and assistant speaks `"I'm listening."`
6. Without tapping the screen, wait 2 seconds and say `"What time is it?"`. Verify current time is spoken.
7. Say `"Hey SAARTHI, open calculator"`. Verify calculator request is sent and honest feedback is provided.
8. Say `"Hey SAARTHI, call mom"`. Verify assistant speaks confirmation prompt and does not dial until `"Yes"` is spoken.
9. From the app card, test the `"🔊 Voice Feedback: ON / MUTED"` toggle.
10. From notification shade, test `"⏸ Pause"` and `"⏹ Stop"` controls.

---

## 11. Summary of Changes & Next Steps

### Implemented:
- Gated standby wake-phrase detection isolating ambient speech.
- Natural two-stage post-wake listening window eliminating mandatory button taps.
- 6-second inactivity timeout returning quietly to standby.
- Action execution contract with honest OS dispatch wording.
- Sensitive action confirmation protecting phone calls and data operations.
- Mutual exclusion preventing TTS acoustic self-triggering.
- Rapid duplicate command debouncing.
- Release APK size optimized at **1.37 MB** (well below 5 MB ceiling).

### Next Recommended Step:
Connect a physical Samsung Galaxy A03 or Android 14+ device via USB debugging to execute the Section 10 verification protocol on physical hardware.
