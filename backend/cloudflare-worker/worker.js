/**
 * SAARTHI AI — Secure Serverless Proxy Worker (Cloudflare Workers)
 *
 * Free Tier Limits:
 * - 100,000 requests per day at zero cost (no credit card required).
 *
 * Security & Privacy Architecture:
 * - Secret API keys (GROQ_API_KEY) are stored exclusively in Cloudflare server-side secrets.
 * - Zero secrets embedded in the Android APK.
 * - Message bodies and personal data are NEVER logged.
 * - Enforces request size limits (max 16 KB) and basic abuse protection.
 */

export default {
  async fetch(request, env) {
    // 1. CORS Preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, User-Agent",
        },
      });
    }

    // 2. Only allow POST /api/chat
    const url = new URL(request.url);
    if (request.method !== "POST" || (url.pathname !== "/api/chat" && url.pathname !== "/")) {
      return new Response(JSON.stringify({ error: "Endpoint not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      });
    }

    // 3. Payload size guard (max 16 KB)
    const contentLength = parseInt(request.headers.get("content-length") || "0", 10);
    if (contentLength > 16384) {
      return new Response(JSON.stringify({ error: "Payload exceeds 16 KB limit" }), {
        status: 413,
        headers: { "Content-Type": "application/json" },
      });
    }

    try {
      const body = await request.json();
      const prompt = body.prompt || (body.messages && body.messages[body.messages.length - 1]?.content);

      if (!prompt || typeof prompt !== "string" || prompt.trim() === "") {
        return new Response(JSON.stringify({ error: "Missing or invalid prompt in request" }), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        });
      }

      // 4. Retrieve server-side secrets (Never exposed to Android client)
      // Priority: Groq Free Developer Tier -> Gemini Free Tier -> Cloudflare Workers AI
      const groqKey = env.GROQ_API_KEY;
      const geminiKey = env.GEMINI_API_KEY;

      const requestedProvider = (request.headers.get("X-Provider") || body.provider || "").toLowerCase();
      const messages = body.messages || [
        { role: "system", content: "You are SAARTHI, a helpful, concise personal AI assistant." },
        { role: "user", content: prompt.trim() },
      ];

      // Route A: Gemini 2.5 Flash via OpenAI compatibility layer
      if ((requestedProvider === "gemini" && geminiKey) || (!groqKey && geminiKey)) {
        const geminiModel = body.model || "gemini-2.5-flash";
        const geminiResponse = await fetch("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${geminiKey}`,
            "User-Agent": "SAARTHI-Proxy/1.0",
          },
          body: JSON.stringify({
            model: geminiModel,
            messages: messages,
            temperature: 0.5,
            max_tokens: 512,
          }),
        });

        const geminiData = await geminiResponse.json();
        if (!geminiResponse.ok) {
          const status = geminiResponse.status;
          const errMsg = status === 429 ? "Gemini free rate limit reached" : "Gemini API error";
          return new Response(JSON.stringify({ error: errMsg, status }), {
            status: status,
            headers: { "Content-Type": "application/json" },
          });
        }

        return new Response(JSON.stringify(geminiData), {
          status: 200,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      // Route B: Groq Free Developer Tier
      if (groqKey) {
        const model = body.model || "llama-3.1-8b-instant";
        const groqResponse = await fetch("https://api.groq.com/openai/v1/chat/completions", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${groqKey}`,
            "User-Agent": "SAARTHI-Proxy/1.0",
          },
          body: JSON.stringify({
            model: model,
            messages: messages,
            temperature: 0.5,
            max_tokens: 512,
          }),
        });

        const groqData = await groqResponse.json();
        if (!groqResponse.ok) {
          const status = groqResponse.status;
          const errMsg = status === 429 ? "Upstream rate limit reached" : "Upstream AI provider error";
          return new Response(JSON.stringify({ error: errMsg, status: status }), {
            status: status,
            headers: { "Content-Type": "application/json" },
          });
        }

        return new Response(JSON.stringify(groqData), {
          status: 200,
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
        });
      }

      // Route C: Cloudflare Workers AI Built-in (Zero API keys required)
      if (env.AI) {
        const aiResponse = await env.AI.run("@cf/meta/llama-3.2-3b-instruct", {
          messages: messages,
          max_tokens: 512,
        });

        return new Response(JSON.stringify({
          choices: [{ message: { content: aiResponse.response || "" } }],
          provider: "Cloudflare Workers AI",
        }), {
          status: 200,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      return new Response(
        JSON.stringify({ error: "No proxy backend AI provider configured (GROQ_API_KEY, GEMINI_API_KEY, or Workers AI)" }),
        { status: 500, headers: { "Content-Type": "application/json" } }
      );
    } catch (err) {
      return new Response(JSON.stringify({ error: "Internal proxy server error" }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }
  },
};
