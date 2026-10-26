# SAARTHI: Verified Technical Architecture & Forensic Source Audit

**Document Identifier:** `SAARTHI-VERIFIED-DOC-002`  
**Target Repository:** `D:\SAARTHI - Your Private AI Assistant`  
**Package / Namespace:** `com.saarthi.assistant`  
**Verification Date:** October 25, 2026  
**Audit Protocol:** Second-Pass Forensic Verification (Strict Read-Only)  
**Verification Baseline:** Evaluated against `docs/PROJECT_TECHNICAL_DOCUMENTATION.md`  

---

## 1. Executive Summary & Implementation Maturity

### 1.1 Application Overview
**SAARTHI** is an Android voice and text conversational assistant application developed in Kotlin using Jetpack Compose. Its design vision is to serve as an offline-first, privacy-conscious personal companion capable of recognizing voice commands, determining intent locally on-device, and speaking synthesized responses without mandatory dependency on external cloud services.

### 1.2 Implementation Maturity Level
The codebase is an **Alpha-stage functional prototype**.
- **Implemented & Functional:**
  - Single-screen Material 3 Compose interface with text and speech interaction modes.
  - Native speech recognition wrapping Android's `SpeechRecognizer` API.
  - Native text-to-speech synthesis wrapping Android's `TextToSpeech` API.
  - Pure Kotlin offline deterministic intent parsing engine (`CommandEngine`) supporting greeting, assistant identity, current clock time, and speech interruption.
  - Unit test suite for offline command processing.
- **Stubbed / Placeholder:**
  - Fallback mechanism for unhandled queries (`CommandResult.NeedsOnlineAI`), returning a static string indicating future online AI connectivity.
- **Unimplemented / Absent:**
  - Network HTTP/API client libraries.
  - Cloud AI integration services.
  - Local database or persistent storage layer (no Room, SQLite, or DataStore).
  - Conversation history thread view or multi-turn conversational context.
  - Custom UI theme application (the defined theme is omitted from root composition).

---

## 2. Verified Project Inventory

### 2.1 Workspace Structure
The workspace root directory is `D:\SAARTHI - Your Private AI Assistant` and contains three top-level directories:
- `android/` — The complete native Android project source tree, Gradle build scripts, and resource directories.
- `.vscode/` — Editor configuration setting for Java build configuration.
- `docs/` — Contains previous technical documentation (`PROJECT_TECHNICAL_DOCUMENTATION.md`).

### 2.2 Complete Source File Directory

```
D:\SAARTHI - Your Private AI Assistant
├── .vscode/
│   └── settings.json
├── docs/
│   └── PROJECT_TECHNICAL_DOCUMENTATION.md
└── android/
    ├── .gitignore
    ├── build.gradle.kts
    ├── gradle.properties
    ├── gradlew
    ├── gradlew.bat
    ├── local.properties
    ├── settings.gradle.kts
    ├── gradle/
    │   ├── gradle-daemon-jvm.properties
    │   ├── libs.versions.toml
    │   └── wrapper/
    │       ├── gradle-wrapper.jar
    │       └── gradle-wrapper.properties
    └── app/
        ├── .gitignore
        ├── build.gradle.kts
        ├── src/
        │   ├── main/
        │   │   ├── AndroidManifest.xml
        │   │   ├── keepRules/
        │   │   │   └── rules.keep
        │   │   ├── java/com/saarthi/assistant/
        │   │   │   ├── MainActivity.kt
        │   │   │   ├── core/
        │   │   │   │   ├── AssistantResponse.kt
        │   │   │   │   ├── CommandEngine.kt
        │   │   │   │   ├── CommandResult.kt
        │   │   │   │   └── IntentType.kt
        │   │   │   ├── ui/theme/
        │   │   │   │   ├── Color.kt
        │   │   │   │   ├── Theme.kt
        │   │   │   │   └── Type.kt
        │   │   │   └── voice/
        │   │   │       ├── AndroidSpeechEngine.kt
        │   │   │       ├── TextToSpeechEngine.kt
        │   │   │       └── VoiceEngine.kt
        │   │   └── res/
        │   │       ├── drawable/
        │   │       │   ├── ic_launcher_background.xml
        │   │       │   └── ic_launcher_foreground.xml
        │   │       ├── mipmap-anydpi/
        │   │       │   ├── ic_launcher.xml
        │   │       │   └── ic_launcher_round.xml
        │   │       ├── mipmap-[h,m,xh,xxh,xxxh]dpi/
        │   │       │   ├── ic_launcher.webp
        │   │       │   └── ic_launcher_round.webp
        │   │       ├── values/
        │   │       │   ├── colors.xml
        │   │       │   ├── strings.xml
        │   │       │   └── themes.xml
        │   │       └── xml/
        │   │           ├── backup_rules.xml
        │   │           └── data_extraction_rules.xml
        │   ├── test/java/com/saarthi/assistant/
        │   │   ├── ExampleUnitTest.kt
        │   │   └── core/
        │   │       └── CommandEngineTest.kt
        │   └── androidTest/java/com/saarthi/assistant/
        │       └── ExampleInstrumentedTest.kt
        └── build/ (pre-existing build outputs)
```

---

## 3. Actual Technology Stack & Build Configuration

### 3.1 Technology & Dependency Manifest

| Component | Verified Specification | Source Reference |
|---|---|---|
| **Language** | Kotlin `2.2.10` | `android/gradle/libs.versions.toml:9` |
| **Build System** | Gradle `9.5.0` | `android/gradle/wrapper/gradle-wrapper.properties:5` |
| **Android Gradle Plugin** | AGP `9.3.2` | `android/gradle/libs.versions.toml:2` |
| **Java Toolchain** | JDK `25` (Foojay Disco API) | `android/gradle/gradle-daemon-jvm.properties:12` |
| **Bytecode Target** | Java 11 (`JavaVersion.VERSION_11`) | `android/app/build.gradle.kts:30-31` |
| **Compile SDK** | API Level `36` (Android 16 preview) | `android/app/build.gradle.kts:8-10` |
| **Target SDK** | API Level `36` | `android/app/build.gradle.kts:15` |
| **Min SDK** | API Level `26` (Android 8.0 Oreo) | `android/app/build.gradle.kts:14` |
| **UI Framework** | Jetpack Compose (BOM `2026.02.01`) | `android/gradle/libs.versions.toml:10, 19` |
| **Design Library** | Material Design 3 (`androidx.compose.material3:material3`) | `android/app/build.gradle.kts:41` |
| **Activity Integration** | `androidx.activity:activity-compose:1.8.0` | `android/gradle/libs.versions.toml:8, 18` |
| **Core Utilities** | `androidx.core:core-ktx:1.10.1` | `android/gradle/libs.versions.toml:3, 13` |
| **Lifecycle Runtime** | `androidx.lifecycle:lifecycle-runtime-ktx:2.6.1` | `android/gradle/libs.versions.toml:7, 17` |
| **Unit Testing** | JUnit `4.13.2` | `android/gradle/libs.versions.toml:4, 14` |
| **Instrumented Testing**| AndroidX Test JUnit `1.1.5` / Espresso Core `3.5.1` | `android/gradle/libs.versions.toml:5-6` |

---

## 4. Application Architecture & Lifecycle

### 4.1 Structural Architecture
The application adheres to a lightweight reactive single-activity architecture, divided into three conceptual packages:
1. **Presentation Layer (`com.saarthi.assistant` & `...ui.theme`):** Composed of `MainActivity` and `SaarthiHomeScreen`. Holds UI state in memory and handles permission callbacks.
2. **Voice Layer (`com.saarthi.assistant.voice`):** Adapts Android platform `SpeechRecognizer` (`AndroidSpeechEngine`) and `TextToSpeech` (`TextToSpeechEngine`) into reactive callbacks.
3. **Domain Layer (`com.saarthi.assistant.core`):** Contains `CommandEngine`, `CommandResult`, and `IntentType`. Completely decoupled from the Android runtime, allowing zero-overhead JVM unit testing.

### 4.2 Mermaid Architecture Diagram

```mermaid
graph TD
    subgraph UI_Layer ["Presentation Layer (MainActivity.kt)"]
        Activity["MainActivity : ComponentActivity"]
        Screen["SaarthiHomeScreen() @Composable"]
        StateContainers["remember { mutableStateOf(...) }<br/>• recognizedText<br/>• typedText<br/>• responseText<br/>• statusText<br/>• microphoneGranted"]
        PermLauncher["rememberLauncherForActivityResult<br/>(RequestPermission)"]
    end

    subgraph Voice_Adapters ["Voice Subsystem (com.saarthi.assistant.voice)"]
        VoiceEngineInterface["<<interface>> VoiceEngine"]
        SpeechEngine["AndroidSpeechEngine"]
        TTSEngine["TextToSpeechEngine<br/>(implements OnInitListener)"]
    end

    subgraph Domain_Core ["Domain Core (com.saarthi.assistant.core)"]
        CommandEngine["CommandEngine"]
        Classifier["detectIntent(input: String)"]
        IntentEnum["<<enum>> IntentType<br/>• GREETING<br/>• GET_ASSISTANT_NAME<br/>• GET_CURRENT_TIME<br/>• STOP_SPEAKING<br/>• UNKNOWN"]
        ResultHierarchy["<<sealed>> CommandResult<br/>• Reply(text: String)<br/>• StopSpeaking<br/>• NeedsOnlineAI"]
        OrphanedModel["AssistantResponse (Unused Data Class)"]
    end

    subgraph Android_OS ["Android Platform Framework"]
        PlatformASR["android.speech.SpeechRecognizer"]
        PlatformTTS["android.speech.tts.TextToSpeech"]
        AudioRecord["RECORD_AUDIO Permission & Mic Hardware"]
        AudioSpeaker["Audio Hardware Output"]
    end

    Activity --> Screen
    Screen --> StateContainers
    Screen --> PermLauncher
    PermLauncher -.->|Requests| AudioRecord

    Screen -->|Calls startListening()| SpeechEngine
    SpeechEngine -.->|Implements| VoiceEngineInterface
    SpeechEngine -->|Creates & Listens| PlatformASR
    PlatformASR -->|Captures Audio| AudioRecord

    Screen -->|Passes text to process()| CommandEngine
    CommandEngine --> Classifier
    Classifier --> IntentEnum
    CommandEngine --> ResultHierarchy

    ResultHierarchy -->|Reply / NeedsOnlineAI| TTSEngine
    ResultHierarchy -->|StopSpeaking| TTSEngine
    TTSEngine -->|speak() / stop()| PlatformTTS
    PlatformTTS --> AudioSpeaker
```

---

## 5. Voice Recognition & Speech Synthesis Internals

### 5.1 Speech Recognition Lifecycle (`AndroidSpeechEngine.kt`)
1. **Availability Check:** Inspects `SpeechRecognizer.isRecognitionAvailable(context)`. If false, invokes `onError("Speech recognition is not available on this device.")`.
2. **Instance Allocation:**
   ```kotlin
   speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
   ```
   *Forensic Finding:* Calling `startListening(...)` repeatedly creates a new `SpeechRecognizer` without releasing previous instances, overwriting `speechRecognizer` reference and leaking callbacks.
3. **Intent Configuration:**
   - Action: `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
   - Language Model: `RecognizerIntent.LANGUAGE_MODEL_FREE_FORM`
   - Partial Results: Disabled (`EXTRA_PARTIAL_RESULTS = false`)
   - Max Results: 3 (`EXTRA_MAX_RESULTS = 3`)
4. **Result Processing:**
   - `onResults(Bundle?)` extracts `results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)`.
   - Passes `matches?.firstOrNull()` to `onResult(String)` callback.
5. **Resource Teardown:**
   - `release()` calls `speechRecognizer?.destroy()`, nullifies reference, and sets `listening = false`.

### 5.2 Speech Synthesis Lifecycle (`TextToSpeechEngine.kt`)
1. **Binding:** Instantiates `TextToSpeech(context.applicationContext, this)`.
2. **Initialization Callback:** `onInit(status: Int)`:
   - Configures language: `textToSpeech.setLanguage(Locale.getDefault())`.
   - Validates availability against `LANG_MISSING_DATA` and `LANG_NOT_SUPPORTED`.
3. **Playback:**
   - `speak(text: String)` checks `initialized && text.isNotBlank()`.
   - Calls `textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SAARTHI_RESPONSE")`.
   - *Forensic Finding:* No `UtteranceProgressListener` is attached. When audio finishes playing, no event is emitted to reset `statusText`.
4. **Interruption & Teardown:**
   - `stop()` calls `textToSpeech.stop()`.
   - `release()` calls `textToSpeech.stop()`, `textToSpeech.shutdown()`, and marks `initialized = false`.

---

## 6. Supported Commands & Intent Matching Rules

All intent parsing is located in `CommandEngine.kt:53-83`.

### 6.1 Normalization
```kotlin
val input = command.trim().lowercase(Locale.getDefault())
```
*Forensic Finding:* Normalization strips leading/trailing whitespace and converts to lower case using `Locale.getDefault()`. In locales such as Turkish (`tr_TR`), ASCII character `"I"` becomes `"ı"` (dotless i), preventing matches against `"hi"`.

### 6.2 Decision Table

| Input String Pattern | Detected `IntentType` | Output `CommandResult` | Verbal & Display Output |
|---|---|---|---|
| Exactly `"hello"`, `"hi"`, or `"hey"` | `GREETING` | `CommandResult.Reply` | *"Hello! I am SAARTHI. How can I help you?"* |
| Contains `"what is your name"` or `"your name"` | `GET_ASSISTANT_NAME` | `CommandResult.Reply` | *"My name is SAARTHI, your private AI assistant."* |
| Exactly `"time"`, or contains `"what time"` or `"current time"` | `GET_CURRENT_TIME` | `CommandResult.Reply` | *"The current time is [hh:mm a]."* (via `SimpleDateFormat`) |
| Exactly `"stop"` or `"stop speaking"` | `STOP_SPEAKING` | `CommandResult.StopSpeaking` | Halts TTS playback. Displays *"Speaking stopped."* |
| Any other input | `UNKNOWN` | `CommandResult.NeedsOnlineAI` | *"I do not know that yet. Online AI will be connected soon."* |

---

## 7. UI Composition, State Management & Theme Integration

### 7.1 Composition Tree (`MainActivity.kt`)
`SaarthiHomeScreen` renders a vertically centered `Column` layout containing:
1. `Text("SAARTHI", style = headlineLarge)`
2. `Text("Your Private AI Assistant", style = bodyLarge)`
3. `Text(statusText, style = bodyMedium)`
4. `OutlinedTextField` bound to `typedText`
5. `Button("Send")` triggering `processCommand(typedText)`
6. Conditional `Text("You said: $recognizedText")`
7. Conditional `Text("SAARTHI: $responseText")`
8. `Button(if (microphoneGranted) "🎙 Talk to SAARTHI" else "🎙 Enable Microphone")`

### 7.2 Theme Disconnect Finding
- `android/app/src/main/java/com/saarthi/assistant/ui/theme/Theme.kt` provides `SAARTHITheme` with dynamic light/dark palettes.
- In `MainActivity.kt:39-41`, `setContent` does not apply `SAARTHITheme`:
  ```kotlin
  override fun onCreate(savedInstanceState: android.os.Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
          SaarthiHomeScreen() // SAARTHITheme is omitted!
      }
  }
  ```
- **Consequence:** Default Material 3 styling is used instead of the project theme tokens.

---

## 8. Database and Persistence Status

- **Status:** **Completely absent.**
- **Verification:** Exhaustive search across Gradle manifests and Kotlin sources confirms zero presence of:
  - SQLite or Android Room Database (`androidx.room`).
  - Jetpack DataStore (`androidx.datastore`).
  - SharedPreferences (`android.content.SharedPreferences`).
  - Internal or external file storage writers.
- **Data Longevity:** Conversational state resides strictly in volatile JVM heap memory within Compose state containers. Process termination or screen rotation resets the entire conversational context.

---

## 9. Permissions, Privacy & Security Considerations

### 9.1 Permissions Manifest

| Permission Name | Level | Status | Location | Notes |
|---|---|---|---|---|
| `android.permission.RECORD_AUDIO` | Dangerous | Declared & Requested | `AndroidManifest.xml:5`, `MainActivity.kt:94-106` | Requested dynamically before listening. |
| `android.permission.INTERNET` | Normal | **Missing** | `AndroidManifest.xml` | Required for future online AI integration. |

### 9.2 Privacy Reality: Offline vs. Cloud ASR
Although the project is presented as "Your Private AI Assistant", the speech-to-text implementation uses Android's native `SpeechRecognizer`. In the standard Android ecosystem, `SpeechRecognizer` delegates to the system ASR provider (commonly Google Speech Services). Unless the user's device has offline speech recognition packages pre-installed and forced in system settings, speech audio may be transmitted to Google's cloud servers for transcription. 

**Conclusion:** The application cannot guarantee 100% offline private voice processing in its current state without an embedded, on-device ASR engine (such as Vosk or whisper.tflite).

---

## 10. Online AI Integration Status

- **Status:** **Placeholder / Not Implemented.**
- **Evidence:**
  - `CommandResult.NeedsOnlineAI` is defined as a sealed class object.
  - In `MainActivity.kt:136-146`, this branch sets:
    ```kotlin
    responseText = "I do not know that yet. Online AI will be connected soon."
    statusText = "Online AI required"
    ```
  - No HTTP networking dependencies exist in `libs.versions.toml`.
  - No network client classes, API keys, or remote endpoints exist.

---

## 11. Existing Test Coverage

### 11.1 Test Suite Breakdown

| Test File | Test Method | Asserted Behavior | Isolation Status |
|---|---|---|---|
| `CommandEngineTest.kt` | `greetingCommand_returnsGreeting` | `input = "hello"` $\rightarrow$ `CommandResult.Reply(...)` | Fully isolated JVM test |
| `CommandEngineTest.kt` | `nameCommand_returnsAssistantName` | `input = "what is your name"` $\rightarrow$ `CommandResult.Reply(...)` | Fully isolated JVM test |
| `CommandEngineTest.kt` | `timeCommand_returnsReply` | `input = "what time is it"` $\rightarrow$ `result is CommandResult.Reply` | Fully isolated JVM test |
| `CommandEngineTest.kt` | `stopCommand_returnsStopSpeaking` | `input = "stop speaking"` $\rightarrow$ `CommandResult.StopSpeaking` | Fully isolated JVM test |
| `CommandEngineTest.kt` | `unknownCommand_requiresOnlineAI` | `input = "tell me something..."` $\rightarrow$ `CommandResult.NeedsOnlineAI` | Fully isolated JVM test |
| `ExampleUnitTest.kt` | `addition_isCorrect` | `assertEquals(4, 2 + 2)` | Template boilerplate |
| `ExampleInstrumentedTest.kt` | `useAppContext` | Verifies package name on device | Template boilerplate |

### 11.2 Historical Test Execution Evidence
Inspection of cached XML test reports in `android/app/build/test-results/testDebugUnitTest/` confirms that all 5 tests in `CommandEngineTest` passed on September 3, 2026 (`hostname="BADHAN-LAPTOP"`).

---

## 12. Confirmed Bugs and Potential Risks

1. **Bug: Custom Theme Bypassed in Root Composition:**
   - File: `android/app/src/main/java/com/saarthi/assistant/MainActivity.kt:39-41`
   - Cause: `setContent { SaarthiHomeScreen() }` lacks `SAARTHITheme { ... }`.
2. **Bug: Repeated SpeechRecognizer Allocations:**
   - File: `android/app/src/main/java/com/saarthi/assistant/voice/AndroidSpeechEngine.kt:30`
   - Cause: `SpeechRecognizer.createSpeechRecognizer(context)` is called on every listen click without releasing existing instances.
3. **Bug: Missing TTS Completion Listener:**
   - File: `android/app/src/main/java/com/saarthi/assistant/voice/TextToSpeechEngine.kt:1-48`
   - Cause: No `UtteranceProgressListener` registered; `statusText` never resets to "Ready" after speech output completes.
4. **Risk: Complete Conversation State Loss on Rotation:**
   - File: `android/app/src/main/java/com/saarthi/assistant/MainActivity.kt:50-73`
   - Cause: State is stored via `remember { mutableStateOf(...) }` instead of `rememberSaveable` or an AndroidX `ViewModel`.
5. **Risk: Future Runtime Crash on Online AI Network Calls:**
   - File: `android/app/src/main/AndroidManifest.xml`
   - Cause: Missing `android.permission.INTERNET`.

---

## 13. Incomplete and Unimplemented Features

1. **Online AI Service Client:** Unmatched queries output a placeholder prompt without attempting any network connection.
2. **Conversational Threading / History:** UI displays only the single most recent command and response.
3. **Voice Level RMS Visualization:** Explicitly stubbed with `// Future: voice level visualization` in `AndroidSpeechEngine.kt:44`.
4. **Live Audio Streaming Transcription:** Explicitly stubbed with `// Future: live transcription` in `AndroidSpeechEngine.kt:79`.
5. **Unused Code:** `core/AssistantResponse.kt` data class is never referenced.

---

## 14. Dependency & Compatibility Observations

- **SDK 36 Target:** Declaring `compileSdk = 36` and `targetSdk = 36` aligns with Android 16 previews.
- **Java Toolchain Compatibility:** `gradle-daemon-jvm.properties` targets JDK 25 via Foojay Disco API, while compiling down to Java 11 bytecode compatibility.
- **AGP 9.3.2 & Gradle 9.5.0:** Employs cutting-edge Gradle 9.x build tools with configuration cache enabled (`org.gradle.configuration-cache=true`).

---

## 15. File-to-Feature Cross-Reference

| Feature / Responsibility | Primary Source File | Associated Files / Dependencies |
|---|---|---|
| **Activity Lifecycle & UI Root** | `MainActivity.kt` | `AndroidManifest.xml` |
| **Command Engine Logic** | `core/CommandEngine.kt` | `core/CommandResult.kt`, `core/IntentType.kt` |
| **Voice Recognition Engine** | `voice/AndroidSpeechEngine.kt` | `voice/VoiceEngine.kt`, `MainActivity.kt` |
| **Speech Synthesis Engine** | `voice/TextToSpeechEngine.kt` | `MainActivity.kt` |
| **Theme and Styling** | `ui/theme/Theme.kt` | `ui/theme/Color.kt`, `ui/theme/Type.kt` |
| **Build Configuration** | `app/build.gradle.kts` | `gradle/libs.versions.toml`, `settings.gradle.kts` |
| **Unit Verification** | `CommandEngineTest.kt` | `core/CommandEngine.kt` |

---

## 16. Prioritized Recommendations for Future Development

1. **Immediate Stability:**
   - Wrap `SaarthiHomeScreen()` inside `SAARTHITheme { ... }` in `MainActivity.kt`.
   - Update `AndroidSpeechEngine.kt` to check `speechRecognizer?.destroy()` prior to creating new instances.
   - Attach an `UtteranceProgressListener` in `TextToSpeechEngine.kt` to update UI status upon speech completion.
2. **Architectural Separation:**
   - Introduce `SaarthiViewModel` to preserve UI state across Activity recreation and decouple engine orchestration from the Compose layout tree.
3. **Intent Engine Enhancements:**
   - Expand `CommandEngine.detectIntent` to use regex patterns or tokenized matching to support conversational variations ("Tell me the time", "Hey Saarthi").
   - Prune the unused `AssistantResponse.kt` data class.
4. **Online AI Integration:**
   - Add `<uses-permission android:name="android.permission.INTERNET" />` to `AndroidManifest.xml`.
   - Add Retrofit or Ktor dependencies in `libs.versions.toml` to connect to external LLM backends.
5. **True Offline Privacy:**
   - Evaluate bundling an embedded on-device speech model (such as Vosk or whisper.tflite) to guarantee zero audio data leakage regardless of device OS settings.

---

## 17. Verification Limitations & Unresolved Questions

1. **Read-Only Scope:** No build scripts, tests, or application binaries were executed. All findings are derived through rigorous static analysis, cross-file symbol tracing, and pre-existing build artifact inspection.
2. **Absence of Version Control:** The project is not tracked under Git; revision logs and commit diffs could not be examined.
3. **Hardware Runtime Nuances:** Physical microphone acoustic behavior, vendor-specific speech recognition provider behavior (e.g., Samsung vs. Google vs. Huawei), and physical device audio routing could not be tested in a headless environment.

---
*End of Verified Technical Architecture Document.*
