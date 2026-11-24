package com.saarthi.assistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection

/**
 * Universal secure HTTP AI provider client.
 *
 * Implements strict zero-credential client design:
 * - Does NOT embed or bundle any hardcoded secret API keys in the APK.
 * - Connects to a secure backend/proxy endpoint (e.g. Cloudflare Worker or free reverse proxy)
 *   where provider secrets are held server-side.
 * - Supports optional user-supplied BYOK (Bring Your Own Key) saved locally in private SharedPreferences.
 * - Pure Android platform implementation (HttpURLConnection + org.json) with 0-byte APK dependency footprint.
 */
class SecureHttpAiProvider(
    private val endpointUrl: String = DEFAULT_ENDPOINT,
    private val modelName: String = DEFAULT_MODEL,
    private val apiKeySupplier: (() -> String)? = null
) : AiProvider {

    override val providerId: String = "SECURE_HTTP_AI"
    override val providerName: String = "Cloud AI (Secure Proxy)"

    override fun generateResponse(request: AiRequest): AiProviderResult {
        val targetUrl = endpointUrl.trim()
        if (targetUrl.isBlank()) {
            return AiProviderResult.ClientError(
                "No AI service endpoint is configured. Please set a backend endpoint in Settings or configure a free serverless proxy."
            )
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(targetUrl)
            if (!url.protocol.equals("https", ignoreCase = true)) {
                return AiProviderResult.ClientError(
                    "Cloud AI endpoint must use HTTPS to protect your question in transit."
                )
            }
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000 // 10s
                readTimeout = 18000    // 18s
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "SAARTHI-Android/1.0")

                // Add Authorization header ONLY if user locally provided a key
                val userKey = apiKeySupplier?.invoke()?.trim()
                if (!userKey.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $userKey")
                }
            }

            // Build standard OpenAI-compatible payload
            val payload = JSONObject().apply {
                put("model", modelName)
                put("temperature", 0.5)
                put("max_tokens", 512)

                val messagesArray = JSONArray()
                if (!request.systemInstruction.isNullOrBlank()) {
                    messagesArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", request.systemInstruction)
                    })
                }

                // Add bounded conversation context (max 3 past turns)
                request.conversationHistory.takeLast(6).forEach { msg ->
                    messagesArray.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }

                // Add current prompt
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", request.prompt)
                })

                put("messages", messagesArray)
            }

            val requestBody = payload.toString()
            if (requestBody.toByteArray(Charsets.UTF_8).size > MAX_REQUEST_BYTES) {
                return AiProviderResult.ClientError(
                    "This AI request is too large to send safely. Try a shorter question or turn off conversation context."
                )
            }

            // Write request body
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = BufferedReader(InputStreamReader(responseStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }

            when (statusCode) {
                200 -> parseSuccessResponse(responseBody)
                401, 403 -> AiProviderResult.AuthenticationError(
                    "Cloud AI authorization failed (HTTP $statusCode). Verify proxy credentials or endpoint configuration."
                )
                429 -> AiProviderResult.QuotaExceeded(
                    "Cloud AI free rate limit exceeded (HTTP 429). The free quota has been exhausted; please try again later."
                )
                in 500..599 -> AiProviderResult.ServiceError(
                    statusCode = statusCode,
                    message = "Cloud AI service is temporarily unavailable (HTTP $statusCode). Please try again shortly."
                )
                else -> AiProviderResult.ClientError(
                    "Unexpected response from AI service (HTTP $statusCode)."
                )
            }
        } catch (_: SocketTimeoutException) {
            AiProviderResult.NetworkError(
                message = "AI service request timed out. Please check your network and try again.",
                isOffline = false
            )
        } catch (_: UnknownHostException) {
            AiProviderResult.NetworkError(
                message = "Unable to connect to AI server. Please check your internet connection.",
                isOffline = true
            )
        } catch (e: Throwable) {
            AiProviderResult.NetworkError(
                message = "Network error while connecting to AI: ${e.message ?: "Connection failed"}.",
                isOffline = false
            )
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun parseSuccessResponse(body: String): AiProviderResult {
        return try {
            val json = JSONObject(body)

            // 1. Check standard OpenAI format: choices[0].message.content
            if (json.has("choices")) {
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    if (choice.has("message")) {
                        val message = choice.getJSONObject("message")
                        val content = message.optString("content", "").trim()
                        if (content.isNotBlank()) {
                            val tokens = json.optJSONObject("usage")?.optInt("total_tokens", 0) ?: 0
                            return AiProviderResult.Success(
                                AiResponse(text = content, providerId = providerId, tokensUsed = tokens)
                            )
                        }
                    }
                }
            }

            // 2. Fallback for custom proxy formats: "response", "answer", "text"
            val fallbackText = json.optString("response", json.optString("answer", json.optString("text", ""))).trim()
            if (fallbackText.isNotBlank()) {
                return AiProviderResult.Success(
                    AiResponse(text = fallbackText, providerId = providerId)
                )
            }

            AiProviderResult.ClientError("AI server returned an empty or unrecognized response format.")
        } catch (_: Throwable) {
            AiProviderResult.ClientError("Failed to parse AI response JSON.")
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://saarthi-proxy.workers.dev/api/chat"
        const val DEFAULT_MODEL = "llama-3.1-8b-instant"
        private const val MAX_REQUEST_BYTES = 16 * 1024
    }
}
