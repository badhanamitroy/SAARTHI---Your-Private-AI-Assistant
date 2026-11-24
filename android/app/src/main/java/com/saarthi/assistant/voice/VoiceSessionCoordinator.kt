package com.saarthi.assistant.voice

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.ConversationContext
import com.saarthi.assistant.core.ConversationState
import com.saarthi.assistant.core.PendingIntent

/**
 * Coordinated controller for SAARTHI's continuous conversational voice sessions.
 *
 * Implements the complete Turn-Taking Voice Lifecycle:
 * 1. User starts session explicitly -> LISTENING
 * 2. Speech recognized -> PROCESSING
 * 3. Local action executed -> EXECUTING
 * 4. Response spoken via TTS (with mutual-exclusion microphone ducking) -> SPEAKING
 * 5. TTS finishes -> Automatically resumes command listening without requiring another tap.
 * 6. User can stop session, mute voice, or issue cancellation at any time.
 * 7. Bounded silence retry and graceful error recovery prevent infinite loops or frozen UI states.
 */
class VoiceSessionCoordinator(
    private val voiceEngine: VoiceEngine,
    private val speechSynthesizer: SpeechSynthesizer,
    val commandEngine: CommandEngine,
    val conversationContext: ConversationContext = ConversationContext(),
    private val maxConsecutiveSilences: Int = 2,
    private val duplicateDebounceMs: Long = 1200L,
    private val actionExecutor: ((commandResult: CommandResult, onComplete: (spoken: String, display: String) -> Unit) -> Unit)? = null
) {

    var isSessionActive: Boolean = false
        private set

    var isVoiceFeedbackEnabled: Boolean = true

    var state: ConversationState = ConversationState.IDLE
        private set

    var statusText: String = "Ready"
        private set

    var lastRecognizedText: String = ""
        private set

    var lastSpokenResponse: String = ""
        private set

    // Callbacks for UI synchronization
    var onStateChanged: ((ConversationState) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onTranscriptReceived: ((String) -> Unit)? = null
    var onResponseReceived: ((String) -> Unit)? = null

    // Concurrency & Debounce guards
    private var lastExecutedCommand: String? = null
    private var lastCommandTimestamp: Long = 0L
    private var consecutiveSilences: Int = 0
    private var isDispatchingAction: Boolean = false

    init {
        speechSynthesizer.onSpeechStart = {
            transitionTo(ConversationState.SPEAKING, "Speaking...")
        }

        speechSynthesizer.onSpeechDone = {
            handleSpeechCompleted()
        }

        speechSynthesizer.onSpeechError = { errorMsg ->
            handleSpeechCompleted()
        }
    }

    private fun transitionTo(newState: ConversationState, newStatus: String? = null) {
        val oldState = state
        state = newState
        if (newStatus != null) {
            statusText = newStatus
            onStatusChanged?.invoke(newStatus)
        }
        if (oldState != newState) {
            onStateChanged?.invoke(newState)
        }
    }

    /**
     * Starts an active continuous voice session.
     * Idempotent: safe to call if already active.
     */
    fun startSession() {
        if (isSessionActive && (state == ConversationState.LISTENING || state == ConversationState.AWAITING_FOLLOW_UP)) {
            return
        }

        isSessionActive = true
        consecutiveSilences = 0
        transitionTo(ConversationState.LISTENING, "Listening...")
        startListeningInternal()
    }

    /**
     * Stops the continuous voice session and releases audio components.
     * Idempotent: safe to call repeatedly.
     */
    fun stopSession(reason: String = "Ready") {
        isSessionActive = false
        consecutiveSilences = 0
        isDispatchingAction = false
        conversationContext.clear()
        voiceEngine.stopListening()
        speechSynthesizer.stop()
        transitionTo(ConversationState.IDLE, reason)
    }

    /**
     * Pauses speech recognition and stops any active speech without clearing conversational context.
     * Used during incoming call ringing to avoid talking over the phone.
     */
    fun pauseListening() {
        voiceEngine.stopListening()
        speechSynthesizer.stop()
        if (state == ConversationState.LISTENING || state == ConversationState.AWAITING_FOLLOW_UP || state == ConversationState.SPEAKING) {
            transitionTo(ConversationState.IDLE, "Paused")
        }
    }

    /**
     * Resumes listening within the active session.
     */
    fun resumeListening() {
        if (isSessionActive) {
            consecutiveSilences = 0
            val nextState = if (conversationContext.hasActiveFollowUp()) {
                ConversationState.AWAITING_FOLLOW_UP
            } else {
                ConversationState.LISTENING
            }
            val nextStatus = if (nextState == ConversationState.AWAITING_FOLLOW_UP) {
                "Listening for follow-up..."
            } else {
                "Listening..."
            }
            transitionTo(nextState, nextStatus)
            startListeningInternal()
        }
    }

    /**
     * Cancels any active utterance or pending follow-up.
     */
    fun cancelCurrentOperation() {
        conversationContext.clear()
        voiceEngine.cancelListening()
        speechSynthesizer.stop()
        if (isSessionActive) {
            transitionTo(ConversationState.LISTENING, "Listening...")
            startListeningInternal()
        } else {
            transitionTo(ConversationState.IDLE, "Cancelled")
        }
    }

    private fun startListeningInternal() {
        if (!isSessionActive || isDispatchingAction || state == ConversationState.SPEAKING) {
            return
        }

        voiceEngine.startListening(
            onResult = { text ->
                handleSpeechRecognized(text)
            },
            onError = { error ->
                handleRecognitionError(error)
            }
        )
    }

    /**
     * Called when recognized speech arrives from VoiceEngine.
     */
    fun handleSpeechRecognized(text: String, currentTimeMs: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return
        }

        // Debounce rapid duplicated recognition callbacks
        if (isDuplicate(trimmed, currentTimeMs)) {
            return
        }
        recordCommand(trimmed, currentTimeMs)

        consecutiveSilences = 0
        lastRecognizedText = trimmed
        onTranscriptReceived?.invoke(trimmed)

        transitionTo(ConversationState.PROCESSING, "Processing...")

        // Step 1: Process command through CommandEngine with conversation context
        val commandResult = commandEngine.process(trimmed, conversationContext)

        // If user explicitly commanded to stop listening, mark session inactive
        if (commandResult is CommandResult.StopListening) {
            isSessionActive = false
            conversationContext.clear()
        }

        // Step 2: Update conversation context if clarification requested
        if (commandResult is CommandResult.Clarification) {
            val q = commandResult.question
            if (q.contains("How long", ignoreCase = true) || q.contains("timer", ignoreCase = true)) {
                conversationContext.setPendingTimer(trimmed, currentTimeMs)
            }
        }

        // Step 3: Stop listening while executing and speaking to guarantee mutual exclusion
        voiceEngine.stopListening()
        transitionTo(ConversationState.EXECUTING, "Executing...")

        // Step 4: Dispatch action execution
        if (actionExecutor != null) {
            isDispatchingAction = true
            try {
                actionExecutor.invoke(commandResult) { spoken, display ->
                    isDispatchingAction = false
                    deliverResponse(spoken, display)
                }
            } catch (e: Throwable) {
                isDispatchingAction = false
                deliverResponse("An error occurred while executing the command.", "Execution failed")
            }
        } else {
            // Default synchronous fallback
            val defaultFeedback = when (commandResult) {
                is CommandResult.Reply -> Pair(commandResult.text, "Ready")
                is CommandResult.Clarification -> Pair(commandResult.question, "Clarification needed")
                is CommandResult.NeedsOnlineAI -> Pair(
                    "I cannot do that offline. Offline capabilities include opening apps, checking the time and date, calculating math, and device settings.",
                    "Online AI required"
                )
                is CommandResult.SetTimer -> Pair("Timer set for ${commandResult.durationSeconds} seconds.", "Timer started")
                is CommandResult.CancelTimer -> Pair("Timer cancelled.", "Timer cancelled")
                is CommandResult.PauseTimer -> Pair("Timer paused.", "Timer paused")
                is CommandResult.ResumeTimer -> Pair("Timer resumed.", "Timer resumed")
                is CommandResult.GetTimerStatus -> Pair("Checking timer status.", "Timer status")
                is CommandResult.GetBatteryStatus -> Pair("Checking battery status.", "Battery status")
                is CommandResult.SetFlashlight -> Pair(
                    if (commandResult.enabled) "Turning flashlight on." else "Turning flashlight off.",
                    if (commandResult.enabled) "Flashlight on" else "Flashlight off"
                )
                is CommandResult.OpenSystemSettings -> Pair(
                    if (commandResult.isDirectToggleAttempt) {
                        "Opening ${commandResult.settingType.displayName}. Please tap the toggle, as Android restricts apps from changing it directly."
                    } else {
                        "Opening ${commandResult.settingType.displayName}."
                    },
                    commandResult.settingType.displayName
                )
                is CommandResult.StopSpeaking -> Pair("Speaking stopped.", "Stopped")
                is CommandResult.StopListening -> Pair("Listening stopped.", "Stopped")
                else -> Pair("Action completed.", "Completed")
            }
            deliverResponse(defaultFeedback.first, defaultFeedback.second)
        }
    }

    private fun deliverResponse(spokenFeedback: String, displayFeedback: String) {
        lastSpokenResponse = spokenFeedback
        onResponseReceived?.invoke(spokenFeedback)

        if (isVoiceFeedbackEnabled && spokenFeedback.isNotBlank()) {
            transitionTo(ConversationState.SPEAKING, displayFeedback)
            speechSynthesizer.speak(spokenFeedback)
        } else {
            // No spoken feedback needed: handle post-speech transition immediately
            statusText = displayFeedback
            onStatusChanged?.invoke(displayFeedback)
            handleSpeechCompleted()
        }
    }

    private fun handleSpeechCompleted() {
        if (!isSessionActive) {
            transitionTo(ConversationState.IDLE, if (statusText.isNotBlank()) statusText else "Ready")
            return
        }

        // Automatic continuous listening: resume listening for next turn
        val nextState = if (conversationContext.hasActiveFollowUp()) {
            ConversationState.AWAITING_FOLLOW_UP
        } else {
            ConversationState.LISTENING
        }

        val nextStatus = if (nextState == ConversationState.AWAITING_FOLLOW_UP) {
            "Listening for follow-up..."
        } else {
            "Listening..."
        }

        transitionTo(nextState, nextStatus)
        startListeningInternal()
    }

    private fun handleRecognitionError(error: String) {
        val isSilence = error.contains("No speech", ignoreCase = true) ||
                        error.contains("timeout", ignoreCase = true) ||
                        error.contains("No match", ignoreCase = true)

        val isPermissionDenied = error.contains("permission", ignoreCase = true)
        val isUnrecoverable = error.contains("not available", ignoreCase = true) ||
                              error.contains("unsupported", ignoreCase = true)

        if (isPermissionDenied) {
            stopSession("Microphone permission denied")
            transitionTo(ConversationState.ERROR, "Microphone permission denied")
            return
        }

        if (isUnrecoverable) {
            stopSession(error)
            transitionTo(ConversationState.ERROR, error)
            return
        }

        if (isSilence && isSessionActive) {
            consecutiveSilences++
            if (consecutiveSilences < maxConsecutiveSilences) {
                // Recoverable silence: re-listen
                transitionTo(ConversationState.LISTENING, "Listening...")
                startListeningInternal()
                return
            } else {
                // Bounded silence reached: cleanly close session without loop
                stopSession("Ready")
                return
            }
        }

        // Non-silence error (e.g. client error)
        if (isSessionActive) {
            consecutiveSilences++
            if (consecutiveSilences < maxConsecutiveSilences) {
                transitionTo(ConversationState.LISTENING, "Listening...")
                startListeningInternal()
            } else {
                stopSession(error)
                transitionTo(ConversationState.ERROR, error)
            }
        } else {
            transitionTo(ConversationState.ERROR, error)
        }
    }

    private fun isDuplicate(command: String, currentTimeMs: Long): Boolean {
        return command == lastExecutedCommand && (currentTimeMs - lastCommandTimestamp) < duplicateDebounceMs
    }

    private fun recordCommand(command: String, currentTimeMs: Long) {
        lastExecutedCommand = command
        lastCommandTimestamp = currentTimeMs
    }
}
