package com.saarthi.assistant.ai

/**
 * Message in an AI conversation context.
 */
data class AiChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * Request payload sent to an AI provider.
 */
data class AiRequest(
    val prompt: String,
    val conversationHistory: List<AiChatMessage> = emptyList(),
    val systemInstruction: String? = "You are SAARTHI, a helpful, private, and concise personal AI assistant. Keep responses clear and direct.",
    val timeoutMs: Long = 15000L
)

/**
 * Successful response from an AI provider.
 */
data class AiResponse(
    val text: String,
    val providerId: String,
    val tokensUsed: Int = 0
)

/**
 * Result sealed hierarchy for AI provider operations.
 */
sealed class AiProviderResult {

    data class Success(
        val response: AiResponse
    ) : AiProviderResult()

    data class NetworkError(
        val message: String,
        val isOffline: Boolean = false
    ) : AiProviderResult()

    data class QuotaExceeded(
        val message: String = "The free AI usage quota has been temporarily reached. Please try again later or continue using offline capabilities."
    ) : AiProviderResult()

    data class AuthenticationError(
        val message: String = "Authentication with the cloud AI service failed. Please check your service configuration in Settings."
    ) : AiProviderResult()

    data class ServiceError(
        val statusCode: Int,
        val message: String
    ) : AiProviderResult()

    data class ClientError(
        val message: String
    ) : AiProviderResult()
}

/**
 * Decoupled interface for online AI providers.
 * Allows pure unit testing with mocks and provider swaps without touching UI or command logic.
 */
interface AiProvider {
    val providerId: String
    val providerName: String

    /**
     * Executes generation request. Blocking/synchronous; must be called from Dispatchers.IO or background thread.
     */
    fun generateResponse(request: AiRequest): AiProviderResult
}
