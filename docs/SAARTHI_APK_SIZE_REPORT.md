# SAARTHI — Core APK Size Measurement & Compliance Report

**Document Version:** 1.2.0  
**Phase:** Background Assistant & Native App Launching Verification  
**Date:** October 25, 2026  
**Artifact Evaluated:** `android/app/build/outputs/apk/release/app-release.apk`  
**Hard Size Limit Constraint:** **5,000,000 bytes** (5.0 MB)  

---

## 1. Compliance Verdict: **PASSED**

The final optimized release APK with the full background foreground service (`AssistantForegroundService`), native package-visibility calculator launcher (`CalculatorLauncher`), multi-intent routing, and custom hi-res brand icons measures **1,348,593 bytes** (approx. **1.29 MiB / 1.35 MB**), well below the strict 5,000,000-byte threshold. The core application utilizes only **26.97%** of the allowed size budget.

```
+-----------------------------------------------------------------------------------------+
| Release Constraint: 5,000,000 bytes                                                     |
| Measured Release APK: 1,348,593 bytes [#############..........] 27.0% (3.65 MB Under Budget) |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Artifact Measurement Comparison

| Build Variant / Milestone | Exact File Path | Size in Bytes | Size in MiB | Size in MB | Limit Compliance |
|---|---|---|---|---|---|
| **Debug APK (Full Background + Launchers)** | `android/app/build/outputs/apk/debug/app-debug.apk` | 12,796,189 B | 12.20 MiB | 12.80 MB | N/A (Debug build with Compose tooling) |
| **Initial Baseline Release** | `android/app/build/outputs/apk/release/app-release.apk` | 1,155,637 B | 1.10 MiB | 1.16 MB | **PASSED** (Baseline) |
| **Phase 1 Release (Offline Core)** | `android/app/build/outputs/apk/release/app-release.apk` | 1,172,021 B | 1.12 MiB | 1.17 MB | **PASSED** |
| **Custom Brand Icon Addition** | `android/app/build/outputs/apk/release/app-release.apk` | 1,348,137 B | 1.29 MiB | 1.35 MB | **PASSED** |
| **Background Assistant + Calculator Launcher** | `android/app/build/outputs/apk/release/app-release.apk` | **1,348,593 B** | **1.29 MiB** | **1.35 MB** | **PASSED** (3,651,407 bytes under budget) |

- **Net Code Size Growth for Background Service + App Launcher:** Only **+456 bytes** of stripped bytecode!
- R8 whole-program optimization and dead-code stripping kept the service, state-machine handlers, and dynamic launcher logic exceptionally lightweight.

---

## 3. Size Optimization Engineering Analysis

### 3.1 Why the Background Assistant Remains Under 1.35 MB
1. **Zero External Speech Frameworks:** By leveraging the Android platform `android.speech.SpeechRecognizer` and `android.speech.tts.TextToSpeech` rather than bundling a 40–150 MB embedded model (e.g. Vosk, Whisper, or Porcupine native binaries), the core background assistant uses **0 bytes** of external native weight.
2. **Platform Native Notification Subsystem:** Uses standard AndroidX `NotificationCompat` and `NotificationChannel` already bundled with core libraries.
3. **Dynamic Package Visibility Queries:** Package queries in `<queries>` are manifest metadata and compile to minimal binary XML table entries in `resources.arsc`.

---

## 4. Architectural Policy on Future AI Models

1. **Core APK Zero-Model Footprint:** The core APK **must never bundle** embedded offline LLM weights (e.g., GGUF 1.5B/3B models which are 1–3 GB) or heavy ASR models (e.g., Whisper-base at ~140 MB). Bundling such models in the core APK would violate the 5 MB hard limit by multiple orders of magnitude.
2. **Optional Modular Downloads:** Any future custom on-device offline models (e.g., Vosk small ~40 MB or Whisper-tiny ~39 MB) must be distributed as optional on-demand downloads stored in app external storage (`context.getExternalFilesDir(...)`), keeping the downloadable base APK strictly under 1.5 MB.

---
*Report generated and measured from artifact on October 25, 2026.*
