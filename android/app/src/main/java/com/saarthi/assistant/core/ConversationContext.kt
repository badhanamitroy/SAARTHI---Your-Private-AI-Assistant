package com.saarthi.assistant.core

/**
 * Standard conversational state machine states required for SAARTHI's continuous voice session.
 * Kept synchronized with Android speech recognition, execution lifecycle, and Compose UI.
 */
enum class ConversationState {
    IDLE,
    LISTENING,
    PROCESSING,
    AWAITING_FOLLOW_UP,
    EXECUTING,
    SPEAKING,
    ERROR
}

/**
 * Explicit representation of pending follow-up operations.
 * Prevents unknown-command fallbacks when a user answers a focused follow-up prompt.
 */
sealed class PendingIntent {
    /** No follow-up question is active. */
    data object None : PendingIntent()

    /**
     * Waiting for duration parameter to start a timer.
     * E.g. Assistant asked: "How long should I set the timer for?"
     * Expected responses: "5 minutes", "পাঁচ মিনিট", "30 seconds", etc.
     */
    data class TimerDuration(
        val originalPrompt: String = "Set a timer",
        val timestampMs: Long = System.currentTimeMillis()
    ) : PendingIntent()

    /**
     * Waiting for affirmative/negative confirmation for sensitive action (call, wipe data).
     */
    data class Confirmation(
        val action: SensitiveAction,
        val prompt: String,
        val timestampMs: Long = System.currentTimeMillis()
    ) : PendingIntent()
}

/**
 * Encapsulates the conversational context for continuous interaction.
 * Includes timeout policy to prevent stale context from hijacking subsequent commands.
 */
data class ConversationContext(
    var pendingIntent: PendingIntent = PendingIntent.None,
    val timeoutMs: Long = 30_000L // 30-second context window
) {
    /**
     * Checks whether the current pending intent has timed out.
     */
    fun isExpired(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return when (val pending = pendingIntent) {
            is PendingIntent.None -> true
            is PendingIntent.TimerDuration -> (currentTimeMs - pending.timestampMs) > timeoutMs
            is PendingIntent.Confirmation -> (currentTimeMs - pending.timestampMs) > timeoutMs
        }
    }

    /**
     * Explicitly sets a pending timer duration expectation.
     */
    fun setPendingTimer(prompt: String = "Set a timer", currentTimeMs: Long = System.currentTimeMillis()) {
        pendingIntent = PendingIntent.TimerDuration(originalPrompt = prompt, timestampMs = currentTimeMs)
    }

    /**
     * Explicitly sets a pending confirmation expectation.
     */
    fun setPendingConfirmation(action: SensitiveAction, prompt: String, currentTimeMs: Long = System.currentTimeMillis()) {
        pendingIntent = PendingIntent.Confirmation(action = action, prompt = prompt, timestampMs = currentTimeMs)
    }

    /**
     * Clears all pending follow-up state.
     */
    fun clear() {
        pendingIntent = PendingIntent.None
    }

    /**
     * Returns true if there is an active, unexpired follow-up pending.
     */
    fun hasActiveFollowUp(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return pendingIntent !is PendingIntent.None && !isExpired(currentTimeMs)
    }
}
