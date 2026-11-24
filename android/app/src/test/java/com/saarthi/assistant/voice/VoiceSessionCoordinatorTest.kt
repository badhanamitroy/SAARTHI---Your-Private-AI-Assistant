package com.saarthi.assistant.voice

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.ConversationContext
import com.saarthi.assistant.core.ConversationState
import com.saarthi.assistant.core.SystemSettingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeVoiceEngine : VoiceEngine {
    private var listeningState = false
    var startListeningCallCount = 0
    var stopListeningCallCount = 0
    var cancelListeningCallCount = 0
    var onResultCallback: ((String) -> Unit)? = null
    var onErrorCallback: ((String) -> Unit)? = null

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        listeningState = true
        startListeningCallCount++
        onResultCallback = onResult
        onErrorCallback = onError
    }

    override fun stopListening() {
        listeningState = false
        stopListeningCallCount++
    }

    override fun cancelListening() {
        listeningState = false
        cancelListeningCallCount++
    }

    override fun isListening(): Boolean = listeningState

    override fun release() {
        listeningState = false
    }

    fun simulateSpeech(text: String) {
        onResultCallback?.invoke(text)
    }

    fun simulateError(error: String) {
        onErrorCallback?.invoke(error)
    }
}

class FakeSpeechSynthesizer : SpeechSynthesizer {
    var lastSpokenText: String? = null
    var speakCallCount = 0
    var stopCallCount = 0
    override var onSpeechStart: (() -> Unit)? = null
    override var onSpeechDone: (() -> Unit)? = null
    override var onSpeechError: ((String) -> Unit)? = null

    override fun isInitialized(): Boolean = true

    override fun speak(text: String) {
        lastSpokenText = text
        speakCallCount++
        onSpeechStart?.invoke()
    }

    override fun stop() {
        stopCallCount++
    }

    override fun release() {}

    fun simulateDone() {
        onSpeechDone?.invoke()
    }

    fun simulateError(msg: String) {
        onSpeechError?.invoke(msg)
    }
}

class VoiceSessionCoordinatorTest {

    private lateinit var voiceEngine: FakeVoiceEngine
    private lateinit var speechSynthesizer: FakeSpeechSynthesizer
    private lateinit var commandEngine: CommandEngine
    private lateinit var coordinator: VoiceSessionCoordinator

    private var executedResults = mutableListOf<CommandResult>()

    @Before
    fun setUp() {
        voiceEngine = FakeVoiceEngine()
        speechSynthesizer = FakeSpeechSynthesizer()
        commandEngine = CommandEngine()
        executedResults.clear()

        coordinator = VoiceSessionCoordinator(
            voiceEngine = voiceEngine,
            speechSynthesizer = speechSynthesizer,
            commandEngine = commandEngine,
            actionExecutor = { result, onComplete ->
                executedResults.add(result)
                val spoken = when (result) {
                    is CommandResult.Reply -> result.text
                    is CommandResult.Clarification -> result.question
                    is CommandResult.SetTimer -> "Timer set for ${result.durationSeconds} seconds."
                    is CommandResult.GetBatteryStatus -> "Battery is at 85 percent and discharging."
                    is CommandResult.SetFlashlight -> if (result.enabled) "Flashlight turned on." else "Flashlight turned off."
                    is CommandResult.OpenSystemSettings -> if (result.isDirectToggleAttempt) {
                        "Opening ${result.settingType.displayName}. Please tap the toggle, as Android restricts apps from changing it directly."
                    } else {
                        "Opening ${result.settingType.displayName}."
                    }
                    else -> "Action completed."
                }
                val display = "Done"
                onComplete(spoken, display)
            }
        )
    }

    // -------------------------------------------------------------
    // Scenario A: Timer continuity with multi-turn turn-taking
    // -------------------------------------------------------------
    @Test
    fun scenarioA_timerContinuity_resolvesFollowUpAndResumesListening() {
        // Step 1: User starts continuous voice session
        coordinator.startSession()
        assertTrue(coordinator.isSessionActive)
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertEquals(1, voiceEngine.startListeningCallCount)

        // Step 2: User says "Start a timer"
        voiceEngine.simulateSpeech("Start a timer")
        // Dispatches clarification, speechSynthesizer speaks question
        assertEquals(ConversationState.SPEAKING, coordinator.state)
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("How long", ignoreCase = true))

        // Step 3: TTS finishes speaking -> automatically resumes listening for follow-up!
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.AWAITING_FOLLOW_UP, coordinator.state)
        assertTrue(voiceEngine.isListening())

        // Step 4: User says "5 minutes"
        voiceEngine.simulateSpeech("5 minutes")
        assertEquals(2, executedResults.size)
        val timerResult = executedResults.last()
        assertTrue(timerResult is CommandResult.SetTimer)
        assertEquals(300L, (timerResult as CommandResult.SetTimer).durationSeconds)

        // Confirmation spoken
        assertEquals(ConversationState.SPEAKING, coordinator.state)
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("300 seconds"))

        // Step 5: After confirmation finishes, session is still active and listening for next turn!
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(coordinator.isSessionActive)
        assertTrue(voiceEngine.isListening())
    }

    // -------------------------------------------------------------
    // Scenario B: Multi-command continuous session without extra taps
    // -------------------------------------------------------------
    @Test
    fun scenarioB_multiCommandTurnTaking_seamlesslyHandlesBackToBackRequests() {
        coordinator.startSession()

        // Turn 1: Check battery
        voiceEngine.simulateSpeech("Battery koto ache?")
        assertEquals(ConversationState.SPEAKING, coordinator.state)
        assertTrue(executedResults.last() is CommandResult.GetBatteryStatus)
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("85 percent"))

        // Finish Turn 1 TTS -> automatically listens again
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(coordinator.isSessionActive)

        // Turn 2: Flashlight
        voiceEngine.simulateSpeech("Torch on koro")
        assertEquals(ConversationState.SPEAKING, coordinator.state)
        assertTrue(executedResults.last() is CommandResult.SetFlashlight)
        assertTrue((executedResults.last() as CommandResult.SetFlashlight).enabled)
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("Flashlight turned on"))

        // Finish Turn 2 TTS -> automatically listens again
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(coordinator.isSessionActive)
    }

    // -------------------------------------------------------------
    // Scenario C: Wi-Fi toggle request routing and honest explanation
    // -------------------------------------------------------------
    @Test
    fun scenarioC_wifiCommand_routesDirectlyWithHonestExplanation() {
        coordinator.startSession()

        voiceEngine.simulateSpeech("Turn off Wi-Fi")
        assertEquals(1, executedResults.size)
        val result = executedResults.first()
        assertTrue(result is CommandResult.OpenSystemSettings)
        val settings = result as CommandResult.OpenSystemSettings
        assertEquals(SystemSettingType.WIFI, settings.settingType)
        assertTrue(settings.isDirectToggleAttempt)

        // Verifies honest spoken explanation
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("Please tap the toggle"))
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("Android restricts apps"))
    }

    // -------------------------------------------------------------
    // Scenario D: Session control lifecycle (Idempotent start, stop, restart)
    // -------------------------------------------------------------
    @Test
    fun scenarioD_sessionControl_startStopRestartIdempotence() {
        assertFalse(coordinator.isSessionActive)
        assertEquals(ConversationState.IDLE, coordinator.state)

        coordinator.startSession()
        assertTrue(coordinator.isSessionActive)
        assertEquals(ConversationState.LISTENING, coordinator.state)

        // Calling start again while listening is a no-op
        val initialCallCount = voiceEngine.startListeningCallCount
        coordinator.startSession()
        assertEquals(initialCallCount, voiceEngine.startListeningCallCount)

        // Stop session explicitly
        coordinator.stopSession("User cancelled")
        assertFalse(coordinator.isSessionActive)
        assertEquals(ConversationState.IDLE, coordinator.state)
        assertEquals("User cancelled", coordinator.statusText)
        assertFalse(voiceEngine.isListening())

        // Start again
        coordinator.startSession()
        assertTrue(coordinator.isSessionActive)
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(voiceEngine.isListening())
    }

    // -------------------------------------------------------------
    // Scenario E: Error recovery and bounded silence retry
    // -------------------------------------------------------------
    @Test
    fun scenarioE_silenceTimeout_retriesBoundedThenCleanlyStops() {
        coordinator.startSession()
        assertEquals(ConversationState.LISTENING, coordinator.state)

        // First silence timeout -> recoverable retry
        voiceEngine.simulateError("No speech detected")
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(coordinator.isSessionActive)

        // Second silence timeout -> reaches maxConsecutiveSilences (2) -> cleanly stops session
        voiceEngine.simulateError("No speech detected")
        assertFalse(coordinator.isSessionActive)
        assertEquals(ConversationState.IDLE, coordinator.state)
        assertEquals("Ready", coordinator.statusText)
    }

    @Test
    fun scenarioE_permissionDenied_cleanlyStopsSessionWithErrorState() {
        coordinator.startSession()

        voiceEngine.simulateError("Microphone permission not granted")
        assertFalse(coordinator.isSessionActive)
        assertEquals(ConversationState.ERROR, coordinator.state)
        assertEquals("Microphone permission denied", coordinator.statusText)
    }

    // -------------------------------------------------------------
    // Scenario F: Context isolation prevents hijacking unrelated commands
    // -------------------------------------------------------------
    @Test
    fun scenarioF_contextIsolation_unrelatedCommandClearsFollowUp() {
        coordinator.startSession()
        voiceEngine.simulateSpeech("Start a timer")
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.AWAITING_FOLLOW_UP, coordinator.state)
        assertTrue(coordinator.conversationContext.hasActiveFollowUp())

        // User asks time instead of answering timer duration
        voiceEngine.simulateSpeech("what time is it")
        assertFalse("Pending timer follow-up must be cleared", coordinator.conversationContext.hasActiveFollowUp())
        assertTrue(executedResults.last() is CommandResult.Reply)
        val reply = executedResults.last() as CommandResult.Reply
        assertTrue(reply.text.contains("The current time is", ignoreCase = true))
    }

    // -------------------------------------------------------------
    // Scenario G: Duplicate callback debouncing
    // -------------------------------------------------------------
    @Test
    fun scenarioG_duplicateCallbacks_debouncesRapidRepeats() {
        coordinator.startSession()

        val t0 = 10000L
        coordinator.handleSpeechRecognized("Torch on koro", currentTimeMs = t0)
        assertEquals(1, executedResults.size)

        // Rapid duplicate callback from recognition engine at t0 + 200ms
        coordinator.handleSpeechRecognized("Torch on koro", currentTimeMs = t0 + 200L)
        // Must NOT execute twice
        assertEquals(1, executedResults.size)

        // Different command at t0 + 300ms executes
        coordinator.handleSpeechRecognized("what time is it", currentTimeMs = t0 + 300L)
        assertEquals(2, executedResults.size)
    }

    // -------------------------------------------------------------
    // Scenario H: Explicit user-controlled "Stop listening" command
    // -------------------------------------------------------------
    @Test
    fun scenarioH_stopListeningCommand_cleanlyTerminatesSessionWithoutResumingListening() {
        coordinator.startSession()
        assertTrue(coordinator.isSessionActive)

        // User explicitly commands to stop listening
        voiceEngine.simulateSpeech("Stop listening")
        assertFalse("Session must be marked inactive immediately", coordinator.isSessionActive)
        assertEquals(ConversationState.SPEAKING, coordinator.state)

        // Once feedback speech completes, must transition to IDLE and NOT resume listening!
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.IDLE, coordinator.state)
        assertFalse(coordinator.isSessionActive)
        assertFalse(voiceEngine.isListening())
    }

    // -------------------------------------------------------------
    // Scenario I: Action execution exception resilience
    // -------------------------------------------------------------
    @Test
    fun scenarioI_actionExecutionException_handlesFailureGracefullyAndDoesNotFreezeDispatcher() {
        val faultyCoordinator = VoiceSessionCoordinator(
            voiceEngine = voiceEngine,
            speechSynthesizer = speechSynthesizer,
            commandEngine = commandEngine,
            actionExecutor = { _, _ ->
                throw IllegalStateException("Simulated hardware crash")
            }
        )
        faultyCoordinator.startSession()

        // Executing faulty action should not crash or permanently lock dispatcher
        voiceEngine.simulateSpeech("what time is it")
        assertEquals(ConversationState.SPEAKING, faultyCoordinator.state)
        assertTrue(speechSynthesizer.lastSpokenText!!.contains("error occurred", ignoreCase = true))

        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.LISTENING, faultyCoordinator.state)
        assertTrue(faultyCoordinator.isSessionActive)
    }

    // -------------------------------------------------------------
    // Scenario J: Unrecoverable speech recognition error
    // -------------------------------------------------------------
    @Test
    fun scenarioJ_unrecoverableServiceError_immediatelyTransitionsToErrorState() {
        coordinator.startSession()
        assertTrue(coordinator.isSessionActive)

        voiceEngine.simulateError("Speech recognition service is not available on this device.")
        assertFalse("Unrecoverable error must stop session", coordinator.isSessionActive)
        assertEquals(ConversationState.ERROR, coordinator.state)
        assertTrue(coordinator.statusText.contains("not available", ignoreCase = true))
    }

    // -------------------------------------------------------------
    // Scenario K: Call interruption and TTS pause / resume
    // -------------------------------------------------------------
    @Test
    fun scenarioK_callInterruptionDuringSpeaking_pausesAndMutesTTS_resumesCleanlyOnIdle() {
        coordinator.startSession()

        // Assistant is speaking response
        voiceEngine.simulateSpeech("what time is it")
        assertEquals(ConversationState.SPEAKING, coordinator.state)

        // Incoming call rings -> pauseListening() called by CallCoordinator
        coordinator.pauseListening()
        assertEquals(ConversationState.IDLE, coordinator.state)
        assertEquals("Paused", coordinator.statusText)
        assertTrue(speechSynthesizer.stopCallCount >= 1)

        // Call ends -> resumeListening() called by CallCoordinator
        coordinator.resumeListening()
        assertEquals(ConversationState.LISTENING, coordinator.state)
        assertTrue(coordinator.isSessionActive)
        assertTrue(voiceEngine.isListening())
    }

    // -------------------------------------------------------------
    // Scenario L: Duplicate listener prevention during follow-up
    // -------------------------------------------------------------
    @Test
    fun scenarioL_startSession_whenAlreadyAwaitingFollowUp_doesNotTriggerDuplicateEngineCalls() {
        coordinator.startSession()
        val initialStartCount = voiceEngine.startListeningCallCount

        voiceEngine.simulateSpeech("Start a timer")
        speechSynthesizer.simulateDone()
        assertEquals(ConversationState.AWAITING_FOLLOW_UP, coordinator.state)

        // User taps button or event triggers startSession while already waiting for follow-up
        coordinator.startSession()
        // Must not call voiceEngine.startListening again
        assertEquals(initialStartCount + 1, voiceEngine.startListeningCallCount)
        assertEquals(ConversationState.AWAITING_FOLLOW_UP, coordinator.state)
    }
}
