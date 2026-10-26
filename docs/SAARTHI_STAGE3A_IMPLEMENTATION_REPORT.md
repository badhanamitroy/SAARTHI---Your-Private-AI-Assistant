# SAARTHI — Stage 3A: Device Hardware Essentials Implementation Report

**Document Version:** 1.0.0  
**Date:** October 26, 2026  
**Project:** `D:\SAARTHI - Your Private AI Assistant`  
**Application ID:** `com.saarthi.assistant`  
**Milestone:** Stage 3A ("Device Hardware Essentials — Battery Status & Flashlight Control")  
**Execution Status:** FULLY IMPLEMENTED, AUTOMATED-TESTED, BUILD-VERIFIED  

---

## 1. Initial State & Objectives

Prior to Stage 3A, SAARTHI was verified to launch stably on physical Android devices following the resolution of the startup `SecurityException`. However, the app had no access to local device hardware sensors or actuators.

### Objectives for Stage 3A:
1. **Feature A — Battery Status:** Implement a lightweight, 100% offline, zero-permission battery status provider reporting percentage and charging state (Charging, Discharging, Full, Not Charging, Unknown) with support for English, Bangla, and Banglish commands.
2. **Feature B — Flashlight Control:** Implement safe, explicit ON/OFF camera torch control using `CameraManager` on API 26+, checking for flash hardware (`FEATURE_CAMERA_FLASH`), handling camera contention (`HARDWARE_BUSY`) and security restrictions without broad `catch (Throwable)` blocks or camera permissions.
3. **Architecture Preservation:** Reuse the existing command routing (`CommandEngine`), execution contracts (`ExecutionContract`), and UI design system without parallel subsystems.
4. **Automated Testing:** Decouple device APIs using interfaces (`BatteryInfoProvider`, `FlashlightController`) to allow complete unit testing without physical Android hardware.
5. **APK Footprint:** Retain release APK size strictly below 5,000,000 bytes.

---

## 2. Existing Architecture Reused

Rather than creating parallel handlers or ad-hoc managers:
1. **Command Router Integration:**
   - Extended [IntentType.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt) with `BATTERY_STATUS`, `FLASHLIGHT_ON`, `FLASHLIGHT_OFF`.
   - Extended [CommandResult.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt) with `GetBatteryStatus` and `SetFlashlight(enabled: Boolean)`.
   - Enhanced [CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) to detect English, Bangla, and Banglish patterns, and enforce clarification on ambiguous commands ("flashlight", "torch").
2. **Formal Execution Contracts:**
   - Extended [ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt) with `ExecutionFailureType.HARDWARE_UNAVAILABLE` and `HARDWARE_BUSY`.
   - Reused `ExecutionContract.Succeeded` and `ExecutionContract.Failed` to provide honest, verified feedback without premature success claims.
3. **UI & Navigation:**
   - Added compact hardware tiles to [HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt) and an expanded hardware card to [ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt).
   - Hooked `Lifecycle.Event.ON_RESUME` in [MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) to refresh state whenever returning to the foreground.

---

## 3. Detailed Implementation Decisions

### 3.1 Battery Status Implementation
- **Provider Mechanism:** Uses `context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` to read the sticky battery broadcast synchronously with **0 permissions** and **0 battery drain**.
- **Percentage Calculation:**
  ```kotlin
  val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
  val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
  val percentage = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else null
  ```
- **Fallback:** On devices where the broadcast returns null, queries `BatteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)` on API 21+.
- **Charging & Plugged Sources:** Maps `EXTRA_STATUS` to `BatteryChargingState` (`CHARGING`, `DISCHARGING`, `FULL`, `NOT_CHARGING`, `UNKNOWN`) and `EXTRA_PLUGGED` to `PluggedSource` (`AC`, `USB`, `WIRELESS`, `NONE`).
- **Honest Feedback:** Never invents numbers. If indeterminate, reports that the phone is charging but percentage is unknown; if entirely unavailable, returns `ExecutionFailureType.HARDWARE_UNAVAILABLE`.

### 3.2 Flashlight Control Implementation
- **Hardware Verification:** Verifies `packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)` and checks `CameraCharacteristics.FLASH_INFO_AVAILABLE` across `CameraManager.cameraIdList`.
- **Lens Selection:** Prioritizes back-facing camera (`LENS_FACING_BACK`) with flash, falling back to the first available flash camera.
- **API Invocation:** Calls `cameraManager.setTorchMode(targetCameraId, enabled)` directly. **Zero runtime camera permissions required on Android 6.0+ (API 23+)**. Does not open the Camera app.
- **Accurate State Tracking:** Registers `CameraManager.TorchCallback` to track torch mode changes initiated by the app, system quick settings, or third-party camera apps.
- **Defensive Error Handling:**
  - `CameraAccessException.CAMERA_IN_USE` / `MAX_CAMERAS_IN_USE` $\rightarrow$ `HARDWARE_BUSY` ("Flashlight is unavailable because the camera is currently in use.")
  - `CameraAccessException.CAMERA_DISABLED` $\rightarrow$ `SECURITY_RESTRICTION` ("Flashlight cannot be turned on due to device policy.")
  - `SecurityException` $\rightarrow$ `SECURITY_RESTRICTION`
  - `IllegalArgumentException`, `IllegalStateException` $\rightarrow$ `HARDWARE_UNAVAILABLE`
  - Avoids broad `catch (Throwable)` blocks.

### 3.3 Ambiguity vs. Explicit Actions
- Unambiguous queries like `"turn on flashlight"`, `"torch off koro"`, `"ফ্ল্যাশলাইট জ্বালাও"` execute immediate explicit ON or OFF actions.
- Ambiguous single-word inputs like `"flashlight"`, `"torch"`, `"টর্চ"`, or `"ফ্ল্যাশলাইট"` are intercepted by `checkAmbiguity()` and return:
  `"Did you want to turn on or turn off the flashlight?"`
  This strictly prevents unintended hardware toggles.

---

## 4. Root-Cause Analysis for Implementation Issues Encountered

During the automated testing and compilation cycles, two specific issues were identified and resolved:

### Issue 1: Missing Bengali Dari (`।`) Punctuation Stripping
- **Symptom:** `CommandEngineTest > flashlightOnCommands_routesToSetFlashlightTrue` failed on `"ফ্ল্যাশলাইট জ্বালাও।"` with `expected:<SetFlashlight(enabled=true)> but was:<NeedsOnlineAI>`.
- **Root Cause:** In `CommandEngine.kt`, `normalizeInput()` and `detectIntent()` stripped standard ASCII punctuation (`'.', '?', '!', ',', ';', ':', '"', '\''`), but omitted the Bengali sentence terminator (Dari / `।` / U+0964). As a result, `"ফ্ল্যাশলাইট জ্বালাও।"` was not normalized to `"ফ্ল্যাশলাইট জ্বালাও"`.
- **Resolution:** Added `'।'` to `trimEnd` in both `normalizeInput()` and `detectIntent()`. Re-running tests confirmed 100% pass rate.

### Issue 2: String Resource Resolution in Compose Component
- **Symptom:** Initial compilation failed in `DeviceHardwareCard.kt` with unresolved references to `battery_charging`, `battery_full`, etc.
- **Root Cause:** Strings were referenced in code before being added to `strings.xml` and `values-bn/strings.xml`.
- **Resolution:** Added all localized strings (`battery_charging`, `battery_full`, `battery_discharging`, `battery_not_charging`, `battery_unknown`, and hardware button titles) to both English and Bengali resource files.

---

## 5. Files Created, Modified, and Removed

| File Path | Action | Description |
|---|---|---|
| [android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/DeviceHardwareController.kt) | **Created** | Battery status reader (`AndroidBatteryInfoProvider`) and camera torch controller (`AndroidFlashlightController`) with dependency injection hooks. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/DeviceHardwareCard.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/DeviceHardwareCard.kt) | **Created** | Reusable Compose UI components: `DeviceHardwareSummaryRow` for Home and `HardwareControlsCard` for Tools. |
| [android/app/src/test/java/com/saarthi/assistant/core/DeviceHardwareControllerTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/DeviceHardwareControllerTest.kt) | **Created** | 14 automated unit tests for battery formatting, clamped bounds, charging states, and flashlight contracts. |
| [android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/ExecutionContract.kt) | Modified | Added `HARDWARE_UNAVAILABLE` and `HARDWARE_BUSY` failure types. |
| [android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/IntentType.kt) | Modified | Added `BATTERY_STATUS`, `FLASHLIGHT_ON`, `FLASHLIGHT_OFF`. |
| [android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandResult.kt) | Modified | Added `GetBatteryStatus` and `SetFlashlight(enabled: Boolean)`. |
| [android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/core/CommandEngine.kt) | Modified | Integrated battery & flashlight matchers, ambiguity checks, and Bengali dari stripping. |
| [android/app/src/main/java/com/saarthi/assistant/MainActivity.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/MainActivity.kt) | Modified | Added hardware state, lifecycle resume observer, command handlers, and direct UI action bindings. |
| [android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/service/AssistantForegroundService.kt) | Modified | Added battery and flashlight execution handling in background service. |
| [android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/HomeScreen.kt) | Modified | Rendered `DeviceHardwareSummaryRow` with battery percentage and flashlight toggle. |
| [android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/screens/ToolsScreen.kt) | Modified | Rendered `HardwareControlsCard` with explicit ON/OFF buttons and status details. |
| [android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/java/com/saarthi/assistant/ui/components/SaarthiIcons.kt) | Modified | Added custom vector icons: `Battery`, `BatteryCharging`, `FlashlightOn`, `FlashlightOff`. |
| [android/app/src/main/res/values/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values/strings.xml) | Modified | Added English hardware strings. |
| [android/app/src/main/res/values-bn/strings.xml](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/main/res/values-bn/strings.xml) | Modified | Added Bengali hardware strings. |
| [android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt](file:///d:/SAARTHI%20-%20Your%20Private%20AI%20Assistant/android/app/src/test/java/com/saarthi/assistant/core/CommandEngineTest.kt) | Modified | Added 6 new test methods covering English, Bangla, and Banglish battery and flashlight queries. |

---

## 6. Test Results & Build Verification

### 6.1 Automated Unit Test Execution
- **Command:** `.\gradlew.bat test --continue`
- **Result:** **BUILD SUCCESSFUL in 5s**
- **Total Test Classes:** 7
- **Total Tests Executed:** **86**
- **Failures:** **0** | **Skipped:** **0** | **Errors:** **0** (100% Pass Rate)

| Test Suite Class | Tests Passed | Status |
|---|---|---|
| `com.saarthi.assistant.core.CommandEngineTest` | **37 / 37** | **PASSED** |
| `com.saarthi.assistant.core.DeviceHardwareControllerTest` | **14 / 14** | **PASSED** |
| `com.saarthi.assistant.core.ExecutionContractTest` | **7 / 7** | **PASSED** |
| `com.saarthi.assistant.wakeword.NaturalVoiceSessionTest` | **11 / 11** | **PASSED** |
| `com.saarthi.assistant.wakeword.WakeWordCoordinatorTest` | **9 / 9** | **PASSED** |
| `com.saarthi.assistant.voice.SpeechPronunciationFormatterTest` | **7 / 7** | **PASSED** |
| `com.saarthi.assistant.ExampleUnitTest` | **1 / 1** | **PASSED** |

### 6.2 Release Build & APK Size Measurement
- **Command:** `.\gradlew.bat assembleRelease`
- **Result:** **BUILD SUCCESSFUL in 1m 34s**
- **Target APK Path:** `android/app/build/outputs/apk/release/app-release.apk`
- **Desktop Staging Path:** `C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk`
- **Measured APK Size:** **1,584,569 bytes (~1.51 MB)**
- **Constraint Budget:** < 5,000,000 bytes
- **Headroom Remaining:** **3,415,431 bytes (68.3% unused budget)**
- **Net Growth from Phase 2:** Only +22,720 bytes (+22.7 KB), proving the extreme efficiency of pure Kotlin zero-dependency implementation.

---

## 7. Verification Status Ledger

| Capability | Status | Evidence |
|---|---|---|
| Battery Command Matching (EN / BN / Banglish) | **AUTOMATED-TESTED** | `CommandEngineTest` lines 543–615 pass across all 38 query variants. |
| Battery Status Contract & Formatting | **AUTOMATED-TESTED** | `DeviceHardwareControllerTest` lines 27–120 pass across all charging states. |
| Flashlight Command Matching (EN / BN / Banglish) | **AUTOMATED-TESTED** | `CommandEngineTest` lines 616–692 pass across all 40 explicit ON/OFF and ambiguous cases. |
| Flashlight Hardware Failure Contracts | **AUTOMATED-TESTED** | `DeviceHardwareControllerTest` lines 122–272 pass for missing hardware, camera in use, and policy disabled. |
| Release Binary shrink & R8 | **BUILD-VERIFIED** | `assembleRelease` produces valid 1.58 MB APK. |
| Physical Device Hardware Verification | **NOT VERIFIED** (Pending user manual test on attached phone) | Unit tests use verified platform API contracts; physical test steps are provided below. |

---

## 8. Exact Steps for Manually Testing on a Physical Phone

1. **Install Updated APK:**
   Copy `C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk` to your phone or install via ADB:
   ```bash
   adb install -r "C:\Users\Dell\OneDrive\Desktop\SAARTHI-release.apk"
   ```
2. **Test Battery Status via UI:**
   - Open SAARTHI.
   - On the Home screen, inspect the Battery tile. Confirm it shows your phone's actual battery percentage and status (e.g. "82% • Discharging" or "Charging").
   - Tap the Battery tile. Confirm the assistant announces the battery status verbally and displays it in the response card.
   - Plug the phone into a charger. Confirm the status updates to "Charging".
3. **Test Battery Status via Voice / Text:**
   - Type or speak: `"what is my battery percentage"`
   - Type or speak: `"battery koto ache?"`
   - Type or speak: `"চার্জ কত আছে?"`
   - Type or speak: `"is my phone charging?"`
   - Verify that SAARTHI responds truthfully with exact on-device values.
4. **Test Flashlight via UI:**
   - On the Home screen, tap the Flashlight tile. Confirm the physical camera LED turns ON immediately and the tile illuminates with an electric cyan border and "Torch ON".
   - Tap the tile again. Confirm the LED turns OFF immediately.
   - Switch to the **Tools** tab. Locate the "Device Hardware Essentials" card.
   - Tap the explicit **[Turn On]** button $\rightarrow$ confirm LED turns ON.
   - Tap the explicit **[Turn Off]** button $\rightarrow$ confirm LED turns OFF.
5. **Test Flashlight via Voice / Text:**
   - Speak/type: `"turn on flashlight"` $\rightarrow$ confirm LED turns on and assistant says *"Flashlight turned on."*
   - Speak/type: `"torch off koro"` $\rightarrow$ confirm LED turns off and assistant says *"Flashlight turned off."*
   - Speak/type: `"ফ্ল্যাশলাইট জ্বালাও"` $\rightarrow$ confirm LED turns on.
   - Speak/type: `"ফ্ল্যাশলাইট বন্ধ করো"` $\rightarrow$ confirm LED turns off.
   - Speak/type ambiguous command: `"torch"` $\rightarrow$ confirm assistant asks: *"Did you want to turn on or turn off the flashlight?"* without toggling the light.
6. **Test Camera Contention:**
   - Open the phone's native Camera app (which locks camera hardware).
   - Switch back to SAARTHI and command `"turn on flashlight"`.
   - Confirm SAARTHI honestly states: *"Flashlight is unavailable because the camera is currently in use."* without crashing.

---

## 9. Next Recommended Task

**Stage 3B: Device Settings Panels & System Shortcuts**
- Official Android System Panels for Wi-Fi (`Settings.Panel.ACTION_WIFI`), Volume (`Settings.Panel.ACTION_VOLUME`), and Internet connectivity (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`).
- Bluetooth Settings intent dispatch (`Settings.ACTION_BLUETOOTH_SETTINGS`).
- Trilingual command routing in `CommandEngine.kt`.
