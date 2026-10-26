# SAARTHI — Your Private AI Assistant 🚀

<div align="center">

![SAARTHI Logo](Saarthi%20Icon.png)

**A lightweight, 100% offline, privacy-first AI voice and text assistant for Android.**

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit%20Tests-131%20Passed%20(100%25)-brightgreen)](docs/SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md)
[![APK Size](https://img.shields.io/badge/Release%20APK-1.55%20MB%20(Budget%20%3C%205MB)-blue)](docs/SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20%2F%20Zero%20Cloud-success)](#privacy--security-pledge)

</div>

---

## 📖 Overview

**SAARTHI** is an on-device personal AI assistant engineered for low-to-mid-range Android smartphones. Built with native **Kotlin** and **Jetpack Compose (Material 3)**, SAARTHI operates completely offline with **zero cloud dependencies, zero data tracking, and zero invasive permissions**.

Whether via hands-free voice commands or an accessible manual text bar, SAARTHI enables instant device control, fast calculations, countdown timers, safe system settings shortcuts, and deep application launching across **English**, **Bangla (বাংলা)**, and **Banglish (phonetic Latin script)**.

---

## ✨ Key Features

### 🔋 1. Device Hardware Essentials
- **Battery Status & Charging:** Synchronously reads device percentage and charging status (`Charging`, `Discharging`, `Full`, `Not Charging`) via sticky broadcast with **0 runtime permissions**.
- **Camera Flashlight (Torch):** Controls camera LED via Android `CameraManager.setTorchMode` with hardware detection (`FEATURE_CAMERA_FLASH`), lens prioritization, and active `TorchCallback` tracking without opening the camera or requiring camera permissions.
- **Ambiguity Interceptors:** Ambiguous requests like `"flashlight"` or `"torch"` automatically ask for clarification before taking hardware action.

### ⚙️ 2. Safe System Settings Shortcuts
- **Official System Panels (API 29+):** Opens quick system bottom-sheet panels for **Wi-Fi**, **Mobile Data / Internet**, and **Volume / Sound** without requiring deprecated toggle permissions.
- **Direct System Shortcuts:** Instant navigation for **Bluetooth** and **Display Brightness** settings.
- **Honest Platform Explanations:** When direct toggling is requested (e.g., `"turn on wifi"`), SAARTHI clearly explains Android OS platform restrictions and immediately offers the official settings panel.

### ⏱️ 3. Practical In-App Countdown Timer
- **High-Precision Ticker:** Coroutine-driven ticker on `Dispatchers.Default` supporting Start, Pause, Resume, Cancel, and Status queries.
- **Trilingual Duration Parser:** Parses durations in digits, words, and Bengali numerals (`০-৯`) across English, Bangla, and Banglish (e.g., `"Set timer for 2 minutes"`, `"২ মিনিটের টাইমার দাও"`, `"30 second er timer dao"`).
- **Interactive UI & Presets:** Dedicated circular progress card with 1m, 3m, and 5m quick-presets; appears on both the Tools dashboard and as an active banner on the Home dashboard.
- **Debounced Prevention:** Automatically drops duplicate commands fired within a 2.5-second threshold.
- **System Notification:** Alerts upon completion using Android local notifications (`POST_NOTIFICATIONS`).

### 🧮 4. Safe Math Evaluator
- **Offline Arithmetic Engine:** Custom recursive-descent parser evaluating addition, subtraction, multiplication, and division with standard precedence.
- **Defensive Safeguards:** Built-in division-by-zero interceptor and localized number formatting.

### 📱 5. Multi-Tier OEM App Launcher
- **Universal Resolver:** Launches Calculator, Clock, Camera, Settings, and Calendar apps.
- **11+ OEM Vendor Support:** Resolves custom OEM packages across Samsung, Google/Pixel, Xiaomi/Redmi, Oppo/Realme, Vivo, OnePlus, Motorola, Huawei, Honor, Nothing, and Transsion/Infinix/Tecno.

### 💡 6. Centralized Help & Command Discovery
- **Comprehensive Help Registry:** Central single-source-of-truth covering all 10 capabilities.
- **Interactive Accordion:** Expandable Help UI in the Tools screen with clickable prompt chips.
- **Topic-Specific Routing:** Voice and text commands like `"help battery"`, `"help flashlight"`, `"help timer"`, and `"help settings"` route directly to detailed example lists.

### 🎙️ 7. Voice Modality & Assistant Service
- **Mutual Audio Exclusion:** Coordinates listening, offline recognition, and text-to-speech feedback without microphone contention or audio feedback loops.
- **Foreground Service:** Lightweight foreground service for background assistant readiness.

---

## 🗣️ Supported Commands (Trilingual)

| Category | English | Bangla (বাংলা) | Banglish (Latin Script) |
|---|---|---|---|
| **Battery** | `"What is my battery percentage?"` | `"ব্যাটারি কত শতাংশ আছে?"` | `"Battery koto percent ache?"` |
| **Flashlight** | `"Turn on flashlight"` / `"Torch off"` | `"ফ্ল্যাশলাইট জ্বালাও"` / `"টর্চ বন্ধ করো"` | `"Torch on koro"` / `"Flashlight off"` |
| **Wi-Fi Settings** | `"Open Wi-Fi settings"` | `"ওয়াইফাই সেটিংস খোলো"` | `"Wifi settings kholo"` |
| **Bluetooth Settings** | `"Open Bluetooth settings"` | `"ব্লুটুথ সেটিংস খোলো"` | `"Bluetooth settings open koro"` |
| **Mobile Data** | `"Open mobile data settings"` | `"মোবাইল ডাটা সেটিংস খোলো"` | `"Mobile data settings kholo"` |
| **Sound / Volume** | `"Open volume settings"` | `"ভলিউম সেটিংস খোলো"` | `"Volume settings open koro"` |
| **Brightness** | `"Open brightness settings"` | `"ব্রাইটনেস সেটিংস খোলো"` | `"Brightness settings kholo"` |
| **Timer Set** | `"Set a timer for 2 minutes"` | `"২ মিনিটের টাইমার দাও"` | `"2 minute er timer dao"` |
| **Timer Controls** | `"Pause timer"` / `"Resume timer"` | `"টাইমার থামাও"` / `"টাইমার চালু করো"` | `"Timer pause koro"` / `"Timer resume"` |
| **Calculator** | `"Open calculator"` / `"What is 45 * 12?"` | `"ক্যালকুলেটর খোলো"` / `"৪৫ গুণ ১২ কত?"` | `"Calculator kholo"` / `"45 * 12 koto?"` |
| **Help** | `"Help"` / `"What can you do?"` | `"সাহায্য"` / `"তুমি কি করতে পারো?"` | `"Help koro"` / `"Ki korte paro?"` |

---

## 🔒 Privacy & Security Pledge

- **100% Offline & Private:** SAARTHI never transmits voice, audio, text, telemetry, or analytics to any remote server or third-party cloud.
- **Zero Invasive Permissions:**
  - ❌ **NO** `WRITE_SETTINGS` (system settings are never modified silently).
  - ❌ **NO** `CHANGE_WIFI_STATE` or `BLUETOOTH_ADMIN` private toggling.
  - ❌ **NO** Contacts, SMS, Storage, or Location permissions.
- **Minimal Manifest Profile:**
  - `RECORD_AUDIO`: Voice input recognition.
  - `ACCESS_NETWORK_STATE`: Honest offline/airplane-mode detection.
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Background assistant service.
  - `POST_NOTIFICATIONS`: Android 13+ foreground service status and timer completion alerts.

---

## 🏗️ Technical Architecture & Metrics

```
SAARTHI Android Architecture
├── Presentation Layer (Jetpack Compose, Material 3)
│   ├── HomeScreen (Voice Orb, Active Timer Banner, Hardware Quick Tiles)
│   ├── ConversationScreen (Local Message History & TTS Replay)
│   ├── ToolsScreen (Math Solver, Timer Card, Settings Card, Hardware Card, Help Accordion)
│   └── SettingsScreen (Preferences & Privacy Policy)
├── Core Engine Layer
│   ├── CommandEngine (Trilingual NLP Regex & Bengali Numeral Parsing)
│   ├── SystemSettingsController (Decoupled Android Panels & Settings Intents)
│   ├── TimerController (Stateful Coroutine Countdown Engine & Debouncer)
│   ├── DeviceHardwareController (Battery Broadcast & Camera Torch Mode)
│   ├── AppLauncher & CalculatorLauncher (Multi-Tier OEM Intent Resolver)
│   └── HelpRegistry (Central Capability Directory)
└── Voice & Platform Layer
    ├── AndroidSpeechEngine (Platform Offline ASR)
    ├── TextToSpeechEngine (SpeechPronunciationFormatter & Mutual Exclusion)
    └── AssistantForegroundService (Lifecycle & Background Command Dispatcher)
```

### 📊 Verification Metrics
- **Automated Unit Tests:** **131 / 131 tests passed (100% success rate)**
  - `CommandEngineTest`: 50 passed
  - `TimerControllerTest`: 19 passed
  - `DeviceHardwareControllerTest`: 14 passed
  - `SystemSettingsControllerTest`: 13 passed
  - `NaturalVoiceSessionTest`: 11 passed
  - `WakeWordCoordinatorTest`: 9 passed
  - `ExecutionContractTest`: 7 passed
  - `SpeechPronunciationFormatterTest`: 7 passed
  - `ExampleUnitTest`: 1 passed
- **Release APK Footprint:** **1,628,313 bytes (~1.55 MB)**
  - *Target Ceiling:* 5,000,000 bytes (5.0 MB)
  - *Headroom Remaining:* **3.37 MB (67.4% unused buffer)**

---

## 🚀 Getting Started & Building

### Prerequisites
- **JDK:** 17 or higher
- **Android SDK:** Min SDK 26 (Android 8.0 Oreo), Target SDK 36 (Android 16)
- **Build System:** Gradle with Android Gradle Plugin (AGP) 8.9.0+

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/badhanamitroy/SAARTHI---Your-Private-AI-Assistant.git
cd "SAARTHI---Your-Private-AI-Assistant/android"

# Run all 131 automated unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build optimized release APK (R8 minified)
./gradlew assembleRelease
```

The compiled release APK will be located at:
`android/app/build/outputs/apk/release/app-release.apk`

---

## 📂 Repository Structure

```
.
├── Saarthi Icon.png                # Official application icon
├── README.md                       # Master project overview & guide
├── .gitignore                      # Git ignore definitions
├── android/                        # Android Gradle project root
│   ├── app/
│   │   ├── src/main/java/com/saarthi/assistant/
│   │   │   ├── MainActivity.kt     # App entrypoint & composition root
│   │   │   ├── core/               # Controllers, engines, parsers, and contracts
│   │   │   ├── service/            # Assistant foreground service
│   │   │   ├── ui/                 # Compose screens, components, theme, and icons
│   │   │   ├── voice/              # Speech recognition & TTS engines
│   │   │   └── wakeword/           # Wake word state coordinator
│   │   ├── src/main/res/           # Localized strings (EN & BN), layouts, drawables
│   │   └── src/test/java/          # 9 automated unit test suites (131 tests)
│   ├── build.gradle.kts            # Project build configuration
│   └── settings.gradle.kts         # Module definitions
└── docs/                           # Engineering reports and documentation
    ├── SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md  # Stage 3B comprehensive report
    ├── SAARTHI_STAGE3A_IMPLEMENTATION_REPORT.md  # Stage 3A hardware essentials report
    ├── SAARTHI_PROJECT_STATUS.md                 # Live capability verification matrix
    ├── SAARTHI_PROJECT_CHANGELOG.md              # Chronological milestone ledger
    ├── SAARTHI_FILE_CHANGE_INDEX.md              # Authoritative file taxonomy
    └── SAARTHI_PHASE3_READINESS_AUDIT.md         # Source forensics & Phase 3 plan
```

---

## 📜 Documentation Index

For in-depth engineering breakdowns, audits, and verification reports, consult the `docs/` folder:
- [Stage 3B Implementation Report](docs/SAARTHI_STAGE3B_IMPLEMENTATION_REPORT.md)
- [Stage 3A Implementation Report](docs/SAARTHI_STAGE3A_IMPLEMENTATION_REPORT.md)
- [Project Status & Verification Matrix](docs/SAARTHI_PROJECT_STATUS.md)
- [Project Chronological Changelog](docs/SAARTHI_PROJECT_CHANGELOG.md)
- [Repository File Taxonomy & Change Index](docs/SAARTHI_FILE_CHANGE_INDEX.md)

---

## 📄 License & Attribution

Developed with ❤️ by **Badhan Amit Roy**.  
Designed as a private, resilient, and honest companion for every Android user.
