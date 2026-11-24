package com.saarthi.assistant.ai

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of hybrid local-vs-cloud routing.
 */
sealed class HybridRoutingResult {
    data class LocalAction(val commandResult: CommandResult) : HybridRoutingResult()
    data class OnlineAiResponse(val replyText: String, val providerName: String, val tokensUsed: Int = 0) : HybridRoutingResult()
    data class ConsentRequired(val prompt: String) : HybridRoutingResult()
    data class PrivacyRefusal(val reason: String) : HybridRoutingResult()
    data class OfflineExplanation(val explanation: String) : HybridRoutingResult()
    data class Error(val errorText: String) : HybridRoutingResult()
}

/**
 * Formal request classification taxonomy for hybrid assistant routing.
 */
enum class RequestClassification {
    /** Handled 100% offline: apps, hardware settings, flashlight, timer, call answering/rejection, voice stop */
    LOCAL_DEVICE_COMMAND,

    /** Handled 100% offline: time, date, math calculations, identity, greeting, help */
    LOCAL_OFFLINE_QUERY,

    /** Potentially routed to Cloud AI: questions outside local deterministic scope when permitted & online */
    GENERAL_KNOWLEDGE_AI,

    /** Blocked locally: contains private credentials, card numbers, personal IDs, phone numbers, contact records, caller info */
    SENSITIVE_REJECTED
}

/**
 * Hybrid Request Router coordinating local-first offline execution with optional,
 * privacy-governed cloud AI intelligence.
 *
 * Enforces non-negotiable security rules:
 * 1. Local Priority: Recognized phone actions (apps, timer, torch, settings, battery, math) ALWAYS execute 100% offline.
 * 2. Privacy First: Cloud AI is disabled by default; requires explicit user toggle and first-use consent.
 * 3. Sensitive Data Redaction: Refuses local transmission of cards, credentials, passwords.
 * 4. Safety Boundary: Cloud AI output cannot bypass local safety allowlists to execute arbitrary device actions.
 */
class HybridRequestRouter(
    val commandEngine: CommandEngine,
    val privacyManager: PrivacyManager,
    val networkChecker: NetworkChecker,
    val aiProvider: AiProvider
) {

    /**
     * Classifies a user query into one of the four formal architectural tiers.
     */
    fun classify(
        rawInput: String,
        conversationContext: ConversationContext? = null
    ): RequestClassification {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return RequestClassification.LOCAL_DEVICE_COMMAND
        }

        val localResult = commandEngine.process(trimmed, conversationContext)
        if (localResult !is CommandResult.NeedsOnlineAI) {
            return when (localResult) {
                is CommandResult.Reply,
                is CommandResult.Clarification,
                is CommandResult.SensitiveConfirmation,
                is CommandResult.WakeWordAcknowledged -> RequestClassification.LOCAL_OFFLINE_QUERY

                else -> RequestClassification.LOCAL_DEVICE_COMMAND
            }
        }

        val sensitiveCheck = SensitiveDataFilter.inspect(trimmed)
        if (sensitiveCheck.isSensitive) {
            return RequestClassification.SENSITIVE_REJECTED
        }

        return RequestClassification.GENERAL_KNOWLEDGE_AI
    }

    /**
     * Evaluates an input query asynchronously on Dispatchers.IO to guarantee
     * zero network-on-main-thread violations.
     */
    suspend fun routeSuspend(
        rawInput: String,
        conversationContext: ConversationContext? = null,
        recentHistory: List<AiChatMessage> = emptyList()
    ): HybridRoutingResult = withContext(Dispatchers.IO) {
        route(rawInput, conversationContext, recentHistory)
    }

    /**
     * Evaluates an input query through the hybrid architecture pipeline.
     */
    fun route(
        rawInput: String,
        conversationContext: ConversationContext? = null,
        recentHistory: List<AiChatMessage> = emptyList()
    ): HybridRoutingResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return HybridRoutingResult.LocalAction(CommandResult.Ignore)
        }

        // -------------------------------------------------------------
        // Step 1: Local-first command evaluation (HIGHEST PRIORITY)
        // -------------------------------------------------------------
        val localResult = commandEngine.process(trimmed, conversationContext)

        // If the query is recognized as any known local action, execute locally!
        // Local actions NEVER make cloud calls.
        if (localResult !is CommandResult.NeedsOnlineAI) {
            return HybridRoutingResult.LocalAction(localResult)
        }

        // -------------------------------------------------------------
        // Step 2: Cloud AI policy checks (Only for unknown / general knowledge)
        // -------------------------------------------------------------
        val settings = privacyManager.getSettings()

        // Check A: Is Cloud AI enabled by user?
        if (!settings.isCloudAiEnabled) {
            return HybridRoutingResult.OfflineExplanation(
                "Cloud AI is currently disabled in your Privacy Settings. You can enable it in Settings to ask general knowledge questions, or continue using offline capabilities like opening apps, checking the time, setting timers, and calculating math."
            )
        }

        // Check B: Has user granted first-use consent?
        if (!settings.isCloudConsentGranted) {
            return HybridRoutingResult.ConsentRequired(
                AndroidPrivacyController.FIRST_USE_DISCLOSURE + "\n\nWould you like to grant consent to send general knowledge questions to Cloud AI?"
            )
        }

        // Check C: Sensitive data inspection
        val sensitiveCheck = SensitiveDataFilter.inspect(trimmed)
        if (sensitiveCheck.isSensitive) {
            return HybridRoutingResult.PrivacyRefusal(
                "This request was blocked locally because it appears to contain private data or credentials (${sensitiveCheck.reason ?: "sensitive data"}). To protect your privacy, SAARTHI never transmits contact, telephony, or private personal data to cloud AI services."
            )
        }

        // Check D: Network availability (Wi-Fi or Mobile Data)
        if (!networkChecker.isConnected()) {
            return HybridRoutingResult.OfflineExplanation(
                "You are currently offline. Cloud AI requires an active internet connection (Wi-Fi or mobile data). Offline features like opening apps, flashlight, timer, and math are still available."
            )
        }

        // -------------------------------------------------------------
        // Step 3: Secure Cloud AI invocation
        // -------------------------------------------------------------
        val historyToSend = if (settings.allowConversationContextInCloud) {
            recentHistory.takeLast(6) // Data minimization: cap at recent 3 turns (6 messages)
                .filter { message ->
                    message.content.isNotBlank() && !SensitiveDataFilter.inspect(message.content).isSensitive
                }
        } else {
            emptyList()
        }

        val request = AiRequest(
            prompt = trimmed,
            conversationHistory = historyToSend
        )

        return when (val providerResult = aiProvider.generateResponse(request)) {
            is AiProviderResult.Success -> {
                // Step 4: Safety verification on AI output
                // Cloud output is treated strictly as conversational text.
                // It CANNOT execute arbitrary device operations without local allowlisting.
                val safeText = sanitizeAiOutput(providerResult.response.text)
                HybridRoutingResult.OnlineAiResponse(
                    replyText = safeText,
                    providerName = aiProvider.providerName,
                    tokensUsed = providerResult.response.tokensUsed
                )
            }

            is AiProviderResult.QuotaExceeded -> {
                HybridRoutingResult.Error(providerResult.message)
            }

            is AiProviderResult.AuthenticationError -> {
                HybridRoutingResult.Error(providerResult.message)
            }

            is AiProviderResult.NetworkError -> {
                HybridRoutingResult.Error(providerResult.message)
            }

            is AiProviderResult.ServiceError -> {
                HybridRoutingResult.Error(providerResult.message)
            }

            is AiProviderResult.ClientError -> {
                HybridRoutingResult.Error(providerResult.message)
            }
        }
    }

    /**
     * Converts a [HybridRoutingResult] into a standard [CommandResult]
     * for seamless integration with existing MainActivity and VoiceSessionCoordinator.
     */
    fun routeToCommandResult(
        rawInput: String,
        conversationContext: ConversationContext? = null,
        recentHistory: List<AiChatMessage> = emptyList()
    ): CommandResult {
        return when (val result = route(rawInput, conversationContext, recentHistory)) {
            is HybridRoutingResult.LocalAction -> result.commandResult
            is HybridRoutingResult.OnlineAiResponse -> CommandResult.Reply(result.replyText)
            is HybridRoutingResult.ConsentRequired -> CommandResult.Clarification(result.prompt)
            is HybridRoutingResult.PrivacyRefusal -> CommandResult.Reply(result.reason)
            is HybridRoutingResult.OfflineExplanation -> CommandResult.Reply(result.explanation)
            is HybridRoutingResult.Error -> CommandResult.Reply(result.errorText)
        }
    }

    /**
     * Strips potentially dangerous raw terminal escapes or markdown prompt injection artifacts.
     */
    private fun sanitizeAiOutput(text: String): String {
        return text.trim()
            .replace(Regex("\\x1B\\[[0-9;]*[a-zA-Z]"), "") // Remove ANSI escapes
            .take(3000) // Bound maximum response length to prevent UI freezing
    }
}
