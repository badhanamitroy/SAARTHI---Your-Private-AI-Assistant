# SAARTHI — Test Results & Verification Report

**Document Version:** 1.2.0  
**Phase:** Background Assistant & Native App Launching Verification  
**Date:** October 25, 2026  
**Execution Environment:** Windows PowerShell, OpenJDK 25 Toolchain, AGP 9.3.2, Gradle 9.5.0  

---

## 1. Automated Unit Test Results

### 1.1 Test Suite Summary
- **Test Task:** `:app:testDebugUnitTest`
- **Total Test Classes:** 2 (`CommandEngineTest`, `ExampleUnitTest`)
- **Total Test Cases:** 26
- **Passed:** 26
- **Failed:** 0
- **Skipped:** 0
- **Execution Time:** ~0.075s
- **Status:** **PASS**

### 1.2 Detailed Test Case Breakdown

| Test Class | Method Name | Tested Scenario | Assertions | Result |
|---|---|---|---|---|
| `CommandEngineTest` | `greetingCommand_returnsGreeting` | Baseline: `"hello"` | Asserts `Reply("Hello! I am SAARTHI. How can I help you?")` | **PASS** |
| `CommandEngineTest` | `nameCommand_returnsAssistantName` | Baseline: `"what is your name"` | Asserts `Reply("My name is SAARTHI, your private AI assistant.")` | **PASS** |
| `CommandEngineTest` | `timeCommand_returnsReply` | Baseline: `"what time is it"` | Asserts result is `Reply` and starts with `"The current time is "` | **PASS** |
| `CommandEngineTest` | `stopCommand_returnsStopSpeaking` | Baseline: `"stop speaking"` | Asserts result is `StopSpeaking` | **PASS** |
| `CommandEngineTest` | `unknownCommand_requiresOnlineAI` | Baseline: `"tell me something about quantum physics"` | Asserts result is `NeedsOnlineAI` | **PASS** |
| `CommandEngineTest` | `greetingVariants_returnGreeting` | 14 Greeting variations (`"hi"`, `"saarthi"`, `"ok saarthi"`, etc.) | Verifies standard greeting response | **PASS** |
| `CommandEngineTest` | `identityVariants_returnAssistantName` | 6 Identity variations (`"who are you"`, `"what's your name"`, etc.) | Verifies assistant identity response | **PASS** |
| `CommandEngineTest` | `timeVariants_returnReply` | 8 Time variations (`"current time"`, `"tell me the time"`, etc.) | Verifies clock-formatted time response | **PASS** |
| `CommandEngineTest` | `dateVariants_returnCurrentDate` | 9 Date variations (`"what is today's date"`, `"current date"`, etc.) | Verifies calendar-formatted date response (`"Today is ..."`) | **PASS** |
| `CommandEngineTest` | `openCalculator_englishVariants_returnsOpenCalculator` | 8 English calculator launch phrases (`"Hey SAARTHI, open calculator."`, `"Open my calculator"`, etc.) | Asserts `CommandResult.OpenCalculator` | **PASS** |
| `CommandEngineTest` | `openCalculator_hinglishVariants_returnsOpenCalculator` | 8 Hinglish calculator launch phrases (`"SAARTHI, calculator kholo."`, `"calculator open karo"`, etc.) | Asserts `CommandResult.OpenCalculator` | **PASS** |
| `CommandEngineTest` | `openCalculator_doesNotTriggerArithmeticEvaluation` | Verifies app-launch intent is prioritized over math evaluation | Asserts `OpenCalculator`, never math execution | **PASS** |
| `CommandEngineTest` | `wakeWordPrefix_stripsCorrectlyForGeneralCommands` | Wake-phrase prefixes (`"Hey SAARTHI, what time is it?"`, `"SAARTHI: what is today's date?"`, etc.) | Asserts clean stripping and accurate command routing | **PASS** |
| `CommandEngineTest` | `stopSpeakingVariants_returnStopSpeaking` | 9 Stop speaking variations (`"be quiet"`, `"shut up"`, `"silence"`, etc.) | Verifies `StopSpeaking` command result | **PASS** |
| `CommandEngineTest` | `stopListeningVariants_returnStopListening` | 4 Stop listening variations (`"stop listening"`, `"cancel voice"`, etc.) | Verifies `StopListening` command result | **PASS** |
| `CommandEngineTest` | `helpVariants_returnSupportedCommands` | 7 Help & capability variations (`"help"`, `"what can you do"`, etc.) | Verifies offline capability discovery text | **PASS** |
| `CommandEngineTest` | `calculatorArithmetic_evaluatesCorrectly` | 10 Math operations (addition, subtraction, multiplication, division, decimals, powers, modulo) | Verifies exact calculated outputs (`"The answer is X."`) | **PASS** |
| `CommandEngineTest` | `calculatorPrecedence_respectsPEMDAS` | Operator precedence (`2 + 3 * 4 = 14`, `(2 + 3) * 4 = 20`, `2 * 3 ^ 2 = 18`) | Verifies strict mathematical order of operations | **PASS** |
| `CommandEngineTest` | `calculatorDivisionByZero_rejectsGracefully` | Division / modulo by zero (`10 / 0`, `5 divided by 0`, `100 % 0`) | Asserts `"Division by zero is not allowed."` | **PASS** |
| `CommandEngineTest` | `calculatorInvalidInput_rejectsGracefully` | Malformed math (`2 +`, `(2 + 3`, `2 ++ 3`, `abc + 5`) | Asserts graceful rejection with syntax/expression error | **PASS** |
| `CommandEngineTest` | `ambiguousCommands_askForClarification` | Underspecified queries (`"date or time"`, `"calculator"`, `"math"`, etc.) | Asserts `Clarification` question without blind action | **PASS** |
| `CommandEngineTest` | `combinedIntents_executeInSequence` | Compound intents (`"time and date"`, `"hello and time"`, `"date and calc"`) | Asserts multi-intent extraction and combined reply | **PASS** |
| `CommandEngineTest` | `unrelatedWords_notMistakenForCommands` | False-positive guards (`"timetable"`, `"timeout"`, `"update"`, `"dates"`) | Asserts `NeedsOnlineAI` without accidental triggering | **PASS** |
| `CommandEngineTest` | `emptyOrWhitespaceInput_returnsPromptToSpeak` | Blank inputs (`""`, `"   "`, `"\t"`, `"\n"`) | Asserts prompt to type or speak | **PASS** |
| `CommandEngineTest` | `punctuationHandling_preservesIntent` | Trailing punctuation (`"hello...?!"`, `"stop speaking!!!"`, `"calc 10+20???"`) | Asserts punctuation stripping preserves intent | **PASS** |
| `ExampleUnitTest` | `addition_isCorrect` | Mathematical boilerplate | `assertEquals(4, 2 + 2)` | **PASS** |

---

## 2. Automated Build Results

| Build Task | Build Command | Outcome | Artifact Generated | Artifact Size |
|---|---|---|---|---|
| **Unit Tests** | `.\gradlew.bat testDebugUnitTest` | **SUCCESS** | Test XML reports in `app/build/test-results/` | N/A (0.075s execution) |
| **Debug Build** | `.\gradlew.bat assembleDebug` | **SUCCESS** | `app/build/outputs/apk/debug/app-debug.apk` | 12,796,189 bytes |
| **Release Build** | `.\gradlew.bat assembleRelease` | **SUCCESS** | `app/build/outputs/apk/release/app-release.apk` | **1,348,593 bytes** |

---

## 3. Physical Device Testing Status & Verification Checklist

### 3.1 Device Availability Check
The environment was queried for connected physical Android hardware:
```bash
adb devices -l
# Output:
# List of devices attached
# (empty)
```

### 3.2 Live Testing Status: **BLOCKED / PENDING HARDWARE**
- **Target Device:** Samsung Galaxy A03
- **Observed State:** No physical Android hardware is connected to this development workstation via USB debugging or wireless ADB.
- **Reporting Gate Adherence:** In accordance with strict guidelines, background listening, wake-word detection, and native calculator launching are reported honestly as **BLOCKED / PENDING ON HARDWARE** until executed on a physical device.

### 3.3 Physical Device Test Protocol (To execute upon device connection)

| Test ID | Test Scenario | Steps | Expected Result | Device Status |
|---|---|---|---|---|
| **DEV-01** | Install & Permissions | Install `app-release.apk`. Tap "🎙 Enable Permissions". Grant Microphone and Notifications. | Both permissions granted cleanly. Status updates to "Microphone enabled". | **BLOCKED** |
| **DEV-02** | Calculator Launch (English) | Say "Hey SAARTHI, open calculator" or "Open my calculator". | System finds installed calculator (e.g. `com.sec.android.app.popupcalculator` on Samsung), speaks "Opening calculator.", and brings calculator activity to foreground. | **BLOCKED** |
| **DEV-03** | Calculator Launch (Hinglish) | Say "SAARTHI, calculator kholo". | Correctly classified as `OPEN_CALCULATOR` (not math calculation). Launches device calculator. | **BLOCKED** |
| **DEV-04** | Calculator Fallback (Missing App) | On a device/profile without an installed calculator, say "open calculator". | Responds honestly: "No compatible calculator app was found on your phone. You can say 'calculate' followed by your numbers to use the built-in calculator." | **BLOCKED** |
| **DEV-05** | Ambiguous "Calculator" | Say or type "calculator". | Returns clarification question: "Did you want to open the calculator app, or calculate an arithmetic expression?" | **BLOCKED** |
| **DEV-06** | Background Assistant Start | Tap "▶ Start Background Assistant". | Persistent notification appears: "SAARTHI Background Assistant - Listening for 'Hey SAARTHI'..." with Pause and Stop action buttons. | **BLOCKED** |
| **DEV-07** | Background Voice Command (App Minimized) | Press Home button to minimize SAARTHI. Say "Hey SAARTHI, what time is it?". | Speech engine processes command from background foreground service, speaks formatted time via local TTS. | **BLOCKED** |
| **DEV-08** | Background Calculator Launch | While on Home screen or inside another app, say "Hey SAARTHI, open calculator". | Calculator app is launched over the current screen via `FLAG_ACTIVITY_NEW_TASK`. | **BLOCKED** |
| **DEV-09** | Notification Controls (Pause / Resume / Stop) | Pull down notification shade. Tap "⏸ Pause". Tap "▶ Resume". Tap "⏹ Stop". | Service state transitions smoothly between Paused, Active, and Stopped. Notification dismisses on Stop. | **BLOCKED** |
| **DEV-10** | Screen-Off Behavior | With background assistant running, press power button to turn screen off. | Device vendor battery policies evaluated; service behavior verified with audio session. | **BLOCKED** |
| **DEV-11** | App Swipe-Away (Recent Tasks) | Start background service, open Recent Apps, swipe away SAARTHI. | Foreground service notification remains active; process is maintained by Android OS due to foreground service notification. | **BLOCKED** |

---
*Report generated and verified against build artifacts on October 25, 2026.*
