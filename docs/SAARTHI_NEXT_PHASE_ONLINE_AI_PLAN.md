# SAARTHI — Next Phase Online AI Architectural Proposal

**Document Version:** 1.0.0  
**Date:** October 25, 2026  
**Status:** **PROPOSAL ONLY — STRICTLY PENDING EXPLICIT USER APPROVAL**  
**Policy:** Zero code changes or network dependencies will be added until authorized.

---

## 1. Architectural Vision & Core Philosophy

SAARTHI was designed as **"Your Private AI Assistant"**. The introduction of online AI must not compromise the local, privacy-first offline foundation established in Phase 0–6.

### Guiding Principles:
1. **Offline by Default:** All offline commands (time, greetings, identity, stop, device control) must continue to be processed on-device with zero network latency and zero data leakage.
2. **Explicit Consent & Transparency:** Queries sent to an online AI backend must be visually differentiated from offline queries.
3. **Graceful Fallback:** If the network is unavailable or the user disables online features, the application must smoothly return to purely offline processing without crashing or displaying blocking dialogs.
4. **Preservation of the 5 MB Core APK Limit:** Adding networking must not inflate the core APK size beyond 5,000,000 bytes.

---

## 2. Proposed Architecture

```mermaid
flowchart TD
    UserInput([User Command: Voice / Text]) --> CommandEngine[Local CommandEngine]
    CommandEngine --> IntentCheck{Local Intent Match?}
    
    IntentCheck -- Yes (Greeting, Time, Identity, Stop) --> LocalReply[Execute Local Reply / TTS]
    IntentCheck -- No (Unknown Query) --> NetworkToggle{Online AI Enabled<br/>& Network Available?}
    
    NetworkToggle -- No (Airplane mode / Offline) --> OfflineFallback[Local Offline Response:<br/>'I do not recognize that offline. Connect to internet for online AI.']
    NetworkToggle -- Yes --> PrivacySanitizer[Privacy Sanitizer & Redactor]
    
    PrivacySanitizer --> EncryptedClient[Secure HTTPS Client (OkHttp / Ktor)]
    EncryptedClient --> BackendGateway[Secure Backend Proxy / AI Gateway]
    BackendGateway --> LLMProvider[AI Provider: Gemini API / LLM]
    
    LLMProvider --> BackendGateway
    BackendGateway --> EncryptedClient
    EncryptedClient --> UIResponse[Display Markdown Response & Synthesize Audio]
```

### 2.1 Component Breakdown
1. **`NetworkMonitor`:**
   - Uses Android's `ConnectivityManager.NetworkCallback` to observe active network capability (`NET_CAPABILITY_INTERNET`, `NET_CAPABILITY_VALIDATED`) in real time.
2. **`OnlineAiGateway` (Interface):**
   - Defines a clean contract: `suspend fun query(prompt: String): Result<String>`.
3. **`GeminiClient` (or Proxy Implementation):**
   - Connects over TLS 1.3 to either Google Gemini API directly or a lightweight self-hosted proxy gateway.
4. **Privacy Sanitizer:**
   - Filters local device identifiers, IMEIs, contacts, or sensitive metadata before query payload assembly.

---

## 3. Privacy, Data Transmission & Security

### 3.1 Data Transmission Security
- **Strict HTTPS / TLS 1.3:** All transmissions encrypted in transit with certificate pinning to prevent MitM attacks.
- **Zero Local Logging of Online Prompts:** Online prompts will not be written to persistent log files or sent to third-party telemetry services.
- **Opt-in Network Toggle:** A user-facing switch in settings: `"Allow Online AI queries when offline intents do not match"`.

### 3.2 Secure Backend & API Key Handling
- **The Risk:** Embedding raw API keys inside the Android client APK (`BuildConfig` or `local.properties`) is inherently vulnerable to decompilation and extraction via `apktool` / `jadx`.
- **Recommended Approaches:**
  1. *Approach A (Production Recommended - Secure Proxy):* The app talks to a secure personal cloud worker (Cloudflare Worker or GCP Cloud Function) that holds the API key in secret management and proxies requests to Gemini.
  2. *Approach B (BYOK - Bring Your Own Key):* For personal/private developer usage, provide an encrypted Settings input field where the user inputs their own Gemini API key, stored in Android's `EncryptedSharedPreferences` / Android Keystore.

---

## 4. Manifest Permissions & Network Error Handling

### 4.1 Required Manifest Addition
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
*Note:* Neither permission is "dangerous" (runtime prompt not required by Android OS); they are granted at install time.

### 4.2 Network Error Taxonomy
- `NO_NETWORK` $\rightarrow$ *"You are currently offline. Please reconnect to Wi-Fi or mobile data for online answers."*
- `TIMEOUT` $\rightarrow$ *"Online AI took too long to respond. Please check your connection."*
- `SERVER_ERROR` $\rightarrow$ *"The AI service is temporarily unavailable. Please try again later."*
- `QUOTA_EXCEEDED` $\rightarrow$ *"API quota limit reached. Please verify your account."*

---

## 5. Incremental APK Size Impact Analysis

To preserve the **5,000,000-byte hard limit** (current release APK is **1,155,637 bytes**), we evaluate library options:

| Networking Library | Estimated Download Size | R8 Stripped APK Impact | Compatibility | Recommendation |
|---|---|---|---|---|
| **OkHttp 4.x / 5.x** | ~800 KB raw JAR | **+120 KB – 180 KB** | Industry standard, zero issues | **Recommended** |
| **Ktor Client (CIO / Android engine)** | ~1.2 MB raw | **+250 KB – 350 KB** | Kotlin-idiomatic | Feasible |
| **Google GenAI SDK (`google-genai`)** | ~2.5 MB dependencies | **+600 KB – 900 KB** | High dependency surface | Caution |
| **Retrofit + Moshi** | ~600 KB raw | **+180 KB – 220 KB** | Clean interface | Feasible |

**Verdict:** Utilizing standard OkHttp or Ktor with R8 shrinking will increase the release APK by at most ~200 KB, keeping the final release APK around **1.35 MB**, well below the 5 MB limit.

---

## 6. Testing Strategy Before Online Integration

Prior to merging online AI:
1. **MockWebServer Unit Tests:** Verify request formatting, token parsing, and timeout handling using `MockWebServer` with zero live internet traffic.
2. **Offline Resilience Suite:** Ensure that with Wi-Fi/cellular disabled, the app does not freeze or block the UI thread during query dispatch.
3. **Regression Suite:** Re-run all 12 offline command unit tests to ensure local intent speed is unaffected (<1ms).

---
*End of Online AI Architectural Proposal — Awaiting explicit user direction.*
