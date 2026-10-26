# SAARTHI — Offline Core Implementation Report

**Document Version:** 1.0.0  
**Date:** October 25, 2026  
**Package:** `com.saarthi.assistant`  
**Target Workspace:** `D:\SAARTHI - Your Private AI Assistant`  

---

## 1. Executive Summary

This report documents the architectural completion, lifecycle stabilization, and offline hardening of the core **SAARTHI** assistant application. Prior to this phase, the application had functional baseline primitives but suffered from:
1. Missing theme application (`SAARTHITheme` omitted from root composition).
2. Native `SpeechRecognizer` resource leaks caused by un-recycled binder allocations on repeated listening calls.
3. Volatile UI state resetting upon device rotation / configuration changes.
4. Missing text-to-speech completion callbacks leaving UI status in a stale state.
5. Rigid, single-string intent matching failing natural speech variations.
6. An oversized release APK (7,877,896 bytes) exceeding the strict 5,000,000-byte limit due to disabled R8 code shrinking and resource optimization.

Through targeted, non-breaking modifications, all offline core acceptance criteria within the codebase have been fully implemented, stabilized, and verified with automated test suites, reducing the release APK to **1,155,637 bytes** (1.10 MiB).

---

## 2. Baseline Inventory & Code Reuse

The implementation reused existing foundational work while addressing architectural gaps:

| Component | Baseline State | Action Taken | Rationale / Evidence |
|---|---|---|---|
| `MainActivity.kt` | Root Activity calling `SaarthiHomeScreen()` directly; `remember` used for all state. | **Refactored & Enhanced** | Wrapped in `SAARTHITheme`, migrated state to `rememberSaveable`, added TTS completion listener, added scrollable column for small screens, and added offline action shortcuts. |
| `CommandEngine.kt` | Hardcoded exact matching; `Locale.getDefault()` used. | **Refactored & Enhanced** | Migrated normalization to `Locale.US`, added punctuation cleaning, expanded greeting/identity/time/stop variations, added blank/whitespace guard clauses. |
| `AndroidSpeechEngine.kt` | New `SpeechRecognizer` allocated on every listen without teardown; no offline preference flag. | **Refactored & Enhanced** | Added explicit cleanup of previous instances, implemented `cancelListening()`, added `EXTRA_PREFER_OFFLINE` hint, improved error reporting for offline scenarios. |
| `VoiceEngine.kt` | Interface with `startListening`, `stopListening`, `isListening`. | **Enhanced** | Added `cancelListening()` contract method. |
| `TextToSpeechEngine.kt` | Native TTS wrapper without completion listener. | **Enhanced** | Added `UtteranceProgressListener` with `onSpeechStart`, `onSpeechDone`, and `onSpeechError` callbacks. |
| `Theme.kt` / `Color.kt` | Fully configured Material 3 theme. | **Preserved & Integrated** | Now active in `MainActivity.kt`. |
| `AssistantResponse.kt` | Unused data class. | **Preserved** | Retained without breaking changes. |
| `app/build.gradle.kts` | Optimization disabled (`enable = false`); release APK was 7.88 MB. | **Optimized** | Enabled R8 code shrinking and resource shrinking, configured debug signing for test release builds. Release APK reduced to 1.16 MB. |

---

## 3. Offline Capability Contract

### 3.1 Tier A: Guaranteed Local Offline Operations
The following capabilities execute 100% on-device with zero network requests and no cloud dependencies:
- **UI & Layout:** Renders `SaarthiHomeScreen` inside `SAARTHITheme` with dynamic Material 3 palettes.
- **State Preservation:** Conversational inputs (`recognizedText`, `typedText`), output responses (`responseText`), and status (`statusText`) survive configuration changes (e.g., screen rotation, multi-window mode) via `rememberSaveable`.
- **Text Command Processing:** User types a query into `OutlinedTextField` and clicks "Send".
- **Local Intent Classification:**
  - *Greetings:* "hello", "hi", "hey", "hello saarthi", "good morning", "good evening", "greetings".
  - *Identity:* "who are you", "what is your name", "what are you", "tell me about yourself".
  - *Clock Time:* "what time is it", "tell me the time", "current time", "what's the time" (computed from device hardware clock via `SimpleDateFormat`).
  - *Interruption:* "stop", "stop speaking", "stop talking", "shut up", "be quiet", "pause", "silence".
  - *Empty/Whitespace Guard:* Prompts user to type or speak if input is empty.
  - *Deterministic Fallback:* Non-matching commands return `CommandResult.NeedsOnlineAI` informing the user that online AI will be connected soon.
- **Speech Output Interruption:** Clicking "⏹ Stop Speaking" or saying a stop command immediately flushes the TTS queue and halts playback.

### 3.2 Tier B: Platform-Dependent Speech Services
- **Speech-to-Text (ASR):** The app uses `android.speech.SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true`. 
  - *Offline Reality:* Android's platform recognizer relies on the device's installed speech recognition engine (typically Google Speech Services). If offline speech recognition language packs are downloaded on the device, speech recognition works offline. If no offline pack exists, the OS recognizer throws `ERROR_NETWORK`, and SAARTHI now displays clear feedback: `"Network error during speech recognition. (Offline voice may require downloaded language pack)."`.
- **Text-to-Speech (TTS):** Requires a system TTS engine (e.g. Google TTS, Samsung TTS) with pre-installed offline voice data. If TTS is not initialized or language is unsupported, the app handles it gracefully without crashing.

---

## 4. File-by-File Changes Summary

### 1. `android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt`
- Switched input normalization from `Locale.getDefault()` to `Locale.US` to eliminate locale-specific lowercasing anomalies (e.g. Turkish dotted/dotless I).
- Added `input.isBlank()` check returning: `"I didn't hear anything. Please type or say a command."`.
- Added `cleaned = input.trimEnd('.', '?', '!', ',', ';')` to strip sentence-ending punctuation.
- Expanded `detectIntent`:
  - Added prefix matching and multi-token phrases for greetings ("hello saarthi", "good morning", etc.).
  - Added "who are you", "what are you", and "tell me about yourself" to identity intent.
  - Added "tell me the time", "what's the time", "what is the time" to time intent.
  - Added "stop talking", "shut up", "be quiet", "silence", "pause" to stop speaking intent.

### 2. `android/app/src/main/java/com/saarthi/assistant/voice/VoiceEngine.kt`
- Added `fun cancelListening()` to the interface contract.

### 3. `android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt`
- Destroyed previous `speechRecognizer` instance before creating a new one in `startListening(...)`, preventing binder callback leaks.
- Used `context.applicationContext` instead of Activity context to eliminate memory leaks.
- Added `RecognizerIntent.EXTRA_PREFER_OFFLINE = true` on API 23+ to signal the platform recognizer to perform on-device transcription when supported.
- Implemented `cancelListening()` calling `speechRecognizer?.cancel()`.
- Clarified `ERROR_NETWORK` message to explain offline language pack requirements.

### 4. `android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt`
- Implemented `UtteranceProgressListener` inside `onInit(status)`.
- Added callbacks `onSpeechStart`, `onSpeechDone`, and `onSpeechError`.
- Invoked `onSpeechDone?.invoke()` in `stop()` to ensure immediate UI status updates upon manual interruption.
- Added `isInitialized(): Boolean` query method.

### 5. `android/app/src/main/java/com/saarthi/assistant/MainActivity.kt`
- Wrapped composition in `SAARTHITheme { Surface(...) { SaarthiHomeScreen() } }`.
- Replaced `remember` with `rememberSaveable` for `recognizedText`, `typedText`, `responseText`, and `statusText`.
- Attached `LaunchedEffect(textToSpeechEngine)` to update `statusText` to `"Speaking..."` while audio is active and restore it to `"Ready"` upon completion.
- Added scrollable modifier (`Modifier.verticalScroll(rememberScrollState())`) to support small screens and landscape orientation.
- Added quick offline action buttons for "Time", "Identity", and "Hello".
- Rendered dynamic "⏹ Stop Speaking" button while TTS playback is active.
- Made "Talk to SAARTHI" toggle between listening and "⏹ Stop Listening".

### 6. `android/app/build.gradle.kts`
- Set `optimization { enable = true }` under `buildTypes.release`.
- Configured `signingConfig = signingConfigs.getByName("debug")` for the release build type to produce a signed, testable release APK.

### 7. `android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt`
- Expanded test suite from 5 to 11 test cases covering all new intent variations, whitespace inputs, and punctuation handling.

---
*Report compiled and verified against codebase on October 25, 2026.*
