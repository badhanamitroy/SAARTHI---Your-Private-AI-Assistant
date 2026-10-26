package com.saarthi.assistant.wakeword

import com.saarthi.assistant.core.SensitiveAction

sealed class UtteranceRoutingResult {
    /** Utterance lacked wake phrase during standby; silently ignored to prevent accidental execution. */
    data object Ignored : UtteranceRoutingResult()

    /** Standalone wake phrase detected ("Hey SAARTHI"); ready to record command without another tap. */
    data class WakeWordOnly(val wakePhrase: String) : UtteranceRoutingResult()

    /** Wake phrase and command arrived together ("Hey SAARTHI, open calculator"). */
    data class CommandReady(val command: String, val wakePhrase: String) : UtteranceRoutingResult()

    /** Spoken input received in active post-wake window. */
    data class PostWakeCommand(val command: String) : UtteranceRoutingResult()

    /** Response to sensitive action confirmation prompt. */
    data class ConfirmationResponse(val confirmed: Boolean, val action: SensitiveAction?) : UtteranceRoutingResult()

    /** Duplicate command detected within debouncing cooldown; dropped. */
    data class DuplicateDropped(val command: String) : UtteranceRoutingResult()
}

/**
 * State coordinator managing offline wake-word listening, mutual exclusion with SpeechRecognizer,
 * natural post-wake command window, sensitive action confirmations, and debouncing.
 */
class WakeWordCoordinator(
    private val detector: WakeWordDetector,
    private val postWakeTimeoutMs: Long = 6000L,
    private val duplicateDebounceMs: Long = 1200L
) {

    var state: WakeWordState = WakeWordState.IDLE
        private set

    var onStateChanged: ((oldState: WakeWordState, newState: WakeWordState) -> Unit)? = null
    var onWakeWordTriggered: ((wakePhrase: String) -> Unit)? = null
    var onCommandReadyToRecord: (() -> Unit)? = null
    var onPostWakeWindowStarted: (() -> Unit)? = null
    var onPostWakeWindowTimedOut: (() -> Unit)? = null
    var onConfirmationRequested: ((action: SensitiveAction, prompt: String) -> Unit)? = null

    // Session tracking
    private var pendingSensitiveAction: SensitiveAction? = null
    private var lastExecutedCommand: String? = null
    private var lastCommandTimestamp: Long = 0L

    // Follow-up mode flag (optional consecutive commands)
    var isFollowUpEnabled: Boolean = false

    private fun transitionTo(newState: WakeWordState) {
        val oldState = state
        if (oldState != newState) {
            state = newState
            onStateChanged?.invoke(oldState, newState)
        }
    }

    fun start() {
        if (state == WakeWordState.PAUSED || state == WakeWordState.IDLE || state == WakeWordState.ERROR) {
            detector.startListening(
                onWakeWordDetected = { phrase ->
                    onWakeWordDetected(phrase)
                },
                onError = { _ ->
                    transitionTo(WakeWordState.ERROR)
                }
            )
            transitionTo(WakeWordState.LISTENING_FOR_WAKE_WORD)
        }
    }

    private fun onWakeWordDetected(phrase: String) {
        if (state != WakeWordState.LISTENING_FOR_WAKE_WORD) return

        transitionTo(WakeWordState.WAKE_WORD_DETECTED)
        onWakeWordTriggered?.invoke(phrase)

        // Stop wake-word detector to release audio hardware for speech recognizer
        detector.stopListening()
        transitionTo(WakeWordState.HANDOFF_TO_SPEECH_RECOGNIZER)

        onCommandReadyToRecord?.invoke()
    }

    /**
     * Enters the post-wake active listening window where the assistant waits
     * for the user's spoken command without requiring a tap.
     */
    fun enterPostWakeCommandWindow() {
        transitionTo(WakeWordState.LISTENING_FOR_COMMAND)
        onPostWakeWindowStarted?.invoke()
    }

    /**
     * Called when the post-wake listening window expires with no user command.
     * Silently returns to standby wake-word monitoring.
     */
    fun onPostWakeTimeout() {
        if (state == WakeWordState.LISTENING_FOR_COMMAND || state == WakeWordState.AWAITING_CONFIRMATION) {
            pendingSensitiveAction = null
            onPostWakeWindowTimedOut?.invoke()
            resumeWakeWordListening()
        }
    }

    /**
     * Routes an incoming spoken transcript according to the active assistant state.
     * Prevents arbitrary conversation from triggering commands during standby.
     */
    fun processUtterance(text: String, currentTimeMs: Long = System.currentTimeMillis()): UtteranceRoutingResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return UtteranceRoutingResult.Ignored
        }

        return when (state) {
            WakeWordState.LISTENING_FOR_WAKE_WORD -> {
                // Must have wake word to execute anything in standby
                val (hasWake, command) = LocalWakeWordDetector.extractWakeWordAndCommand(trimmed)
                if (!hasWake) {
                    // Silently ignore ambient conversation / TV speech
                    UtteranceRoutingResult.Ignored
                } else if (command.isNullOrBlank()) {
                    // Standalone wake phrase: "Hey SAARTHI"
                    detector.stopListening()
                    transitionTo(WakeWordState.LISTENING_FOR_COMMAND)
                    onWakeWordTriggered?.invoke(trimmed)
                    onPostWakeWindowStarted?.invoke()
                    UtteranceRoutingResult.WakeWordOnly(trimmed)
                } else {
                    // Wake phrase with attached command: "Hey SAARTHI, open calculator"
                    if (isDuplicateCommand(command, currentTimeMs)) {
                        UtteranceRoutingResult.DuplicateDropped(command)
                    } else {
                        detector.stopListening()
                        transitionTo(WakeWordState.PROCESSING_COMMAND)
                        recordExecutedCommand(command, currentTimeMs)
                        UtteranceRoutingResult.CommandReady(command, trimmed)
                    }
                }
            }

            WakeWordState.LISTENING_FOR_COMMAND, WakeWordState.SPEECH_RECOGNITION_ACTIVE -> {
                // Already in post-wake window or active dictation session: accepts command without wake prefix
                val actualCommand = LocalWakeWordDetector.stripWakeWord(trimmed).ifBlank { trimmed }
                if (isDuplicateCommand(actualCommand, currentTimeMs)) {
                    UtteranceRoutingResult.DuplicateDropped(actualCommand)
                } else {
                    transitionTo(WakeWordState.PROCESSING_COMMAND)
                    recordExecutedCommand(actualCommand, currentTimeMs)
                    UtteranceRoutingResult.PostWakeCommand(actualCommand)
                }
            }

            WakeWordState.AWAITING_CONFIRMATION -> {
                val normalized = trimmed.lowercase()
                val isYes = normalized in listOf("yes", "yeah", "yep", "sure", "confirm", "proceed", "haan", "ha")
                val isNo = normalized in listOf("no", "nope", "cancel", "abort", "don't", "stop", "na", "nahi")

                val targetAction = pendingSensitiveAction
                pendingSensitiveAction = null

                if (isYes) {
                    transitionTo(WakeWordState.PROCESSING_COMMAND)
                    UtteranceRoutingResult.ConfirmationResponse(confirmed = true, action = targetAction)
                } else if (isNo) {
                    resumeWakeWordListening()
                    UtteranceRoutingResult.ConfirmationResponse(confirmed = false, action = targetAction)
                } else {
                    // Unrelated speech during confirmation: cancel to be safe and return to standby
                    resumeWakeWordListening()
                    UtteranceRoutingResult.ConfirmationResponse(confirmed = false, action = targetAction)
                }
            }

            WakeWordState.PROCESSING_COMMAND -> {
                val (hasWake, cmd) = LocalWakeWordDetector.extractWakeWordAndCommand(trimmed)
                val actual = if (hasWake && !cmd.isNullOrBlank()) cmd else trimmed
                if (isDuplicateCommand(actual, currentTimeMs)) {
                    UtteranceRoutingResult.DuplicateDropped(actual)
                } else {
                    UtteranceRoutingResult.Ignored
                }
            }

            else -> {
                UtteranceRoutingResult.Ignored
            }
        }
    }

    /**
     * Request confirmation before executing a sensitive action.
     */
    fun requestConfirmation(action: SensitiveAction, prompt: String) {
        pendingSensitiveAction = action
        transitionTo(WakeWordState.AWAITING_CONFIRMATION)
        onConfirmationRequested?.invoke(action, prompt)
    }

    private fun isDuplicateCommand(command: String, currentTimeMs: Long): Boolean {
        if (command == lastExecutedCommand && (currentTimeMs - lastCommandTimestamp) < duplicateDebounceMs) {
            return true
        }
        return false
    }

    private fun recordExecutedCommand(command: String, currentTimeMs: Long) {
        lastExecutedCommand = command
        lastCommandTimestamp = currentTimeMs
    }

    fun onSpeechRecognitionStarted() {
        if (state == WakeWordState.HANDOFF_TO_SPEECH_RECOGNIZER ||
            state == WakeWordState.LISTENING_FOR_WAKE_WORD ||
            state == WakeWordState.LISTENING_FOR_COMMAND) {
            transitionTo(WakeWordState.SPEECH_RECOGNITION_ACTIVE)
        }
    }

    fun onSpeechRecognitionFinished(hasCommand: Boolean) {
        if (state == WakeWordState.SPEECH_RECOGNITION_ACTIVE) {
            if (hasCommand) {
                transitionTo(WakeWordState.PROCESSING_COMMAND)
            } else {
                // No command detected or timeout: return to wake-word listening
                resumeWakeWordListening()
            }
        }
    }

    fun onTtsStarted() {
        transitionTo(WakeWordState.TTS_SPEAKING)
        // Ensure detector is stopped while TTS plays to eliminate feedback loops
        detector.stopListening()
    }

    fun onTtsFinished() {
        if (state == WakeWordState.TTS_SPEAKING || state == WakeWordState.PROCESSING_COMMAND) {
            transitionTo(WakeWordState.COOLDOWN)
            if (isFollowUpEnabled) {
                transitionTo(WakeWordState.FOLLOW_UP_ACTIVE)
                enterPostWakeCommandWindow()
            } else {
                resumeWakeWordListening()
            }
        }
    }

    fun pause() {
        detector.stopListening()
        transitionTo(WakeWordState.PAUSED)
    }

    fun resume() {
        if (state == WakeWordState.PAUSED) {
            detector.startListening(
                onWakeWordDetected = { phrase ->
                    onWakeWordDetected(phrase)
                },
                onError = { _ ->
                    transitionTo(WakeWordState.ERROR)
                }
            )
            transitionTo(WakeWordState.LISTENING_FOR_WAKE_WORD)
        }
    }

    fun stop() {
        detector.stopListening()
        pendingSensitiveAction = null
        transitionTo(WakeWordState.IDLE)
    }

    fun release() {
        detector.release()
        pendingSensitiveAction = null
        transitionTo(WakeWordState.IDLE)
    }

    private fun resumeWakeWordListening() {
        if (state != WakeWordState.PAUSED && state != WakeWordState.IDLE) {
            detector.startListening(
                onWakeWordDetected = { phrase ->
                    onWakeWordDetected(phrase)
                },
                onError = { _ ->
                    transitionTo(WakeWordState.ERROR)
                }
            )
            transitionTo(WakeWordState.LISTENING_FOR_WAKE_WORD)
        }
    }
}
