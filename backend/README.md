# SAARTHI AI — Secure Serverless Backend Proxy

## Architecture & Security Philosophy

The SAARTHI Android application strictly adheres to the principle of **Zero Embedded Secrets**:
- **No secret API keys, tokens, or passwords** are embedded into the Android APK, `BuildConfig`, source code, resources, or logs.
- When Cloud AI is explicitly enabled by the user in Privacy Settings, the Android app connects to a secure backend proxy over HTTPS.
- The proxy holds the upstream AI provider secret (`GROQ_API_KEY`) safely in server-side encrypted storage.
- The proxy sanitizes requests, enforces a 16 KB payload ceiling, and never logs conversation bodies.

---

## 100% Free Deployment with Cloudflare Workers (No Credit Card Required)

Cloudflare Workers provides **100,000 requests per day** on its free plan with zero billing requirements and zero automatic upgrades.

### Prerequisites:
1. Free Cloudflare account: [https://dash.cloudflare.com/sign-up](https://dash.cloudflare.com/sign-up) (no credit card needed).
2. Free Groq developer account: [https://console.groq.com](https://console.groq.com) (free tier, no credit card needed). Generate an API key.

### Step-by-Step Deployment:

1. **Install Wrangler (Cloudflare CLI):**
   ```bash
   npm install -g wrangler
   ```

2. **Authenticate with Cloudflare:**
   ```bash
   wrangler login
   ```

3. **Navigate to backend directory:**
   ```bash
   cd "D:\SAARTHI - Your Private AI Assistant\backend\cloudflare-worker"
   cp wrangler.toml.example wrangler.toml
   ```

4. **Store your Groq API key securely as a Cloudflare Secret:**
   ```bash
   wrangler secret put GROQ_API_KEY
   # Paste your free Groq API key when prompted
   ```

5. **Deploy the Worker:**
   ```bash
   wrangler deploy
   ```

6. **Copy your Worker URL:**
   Wrangler will output your live URL:
   `https://saarthi-proxy.<your-subdomain>.workers.dev`

7. **Configure SAARTHI Android App:**
   In SAARTHI's **Settings $\rightarrow$ Privacy & Data $\rightarrow$ Cloud AI Endpoint**, enter:
   `https://saarthi-proxy.<your-subdomain>.workers.dev/api/chat`
   and toggle **Enable Cloud AI** to ON.
