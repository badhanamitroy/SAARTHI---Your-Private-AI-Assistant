package com.saarthi.assistant.wakeword

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.SensitiveAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NaturalVoiceSessionTest {

    private lateinit var detector: LocalWakeWordDetector
    private lateinit var coordinator: WakeWordCoordinator
    private val commandEngine = CommandEngine()

    @Before
    fun setUp() {
        detector = LocalWakeWordDetector()
        coordinator = WakeWordCoordinator(
            detector = detector,
            postWakeTimeoutMs = 6000L,
            duplicateDebounceMs = 1200L
        )
        coordinator.start()
    }

    @Test
    fun standby_ignoresArbitraryConversation() {
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)

        // Ordinary speech without wake word must be ignored
        val result1 = coordinator.processUtterance("what time is it")
        assertEquals(UtteranceRoutingResult.Ignored, result1)
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)

        val result2 = coordinator.processUtterance("pass the salt please")
        assertEquals(UtteranceRoutingResult.Ignored, result2)

        val result3 = coordinator.processUtterance("open calculator")
        assertEquals(UtteranceRoutingResult.Ignored, result3)
    }

    @Test
    fun standby_wakeWordStandalone_entersPostWakeWindowWithoutTap() {
        var postWakeWindowStarted = false
        coordinator.onPostWakeWindowStarted = {
            postWakeWindowStarted = true
        }

        val result = coordinator.processUtterance("Hey SAARTHI")
        assertTrue(result is UtteranceRoutingResult.WakeWordOnly)
        assertTrue(postWakeWindowStarted)
        assertEquals(WakeWordState.LISTENING_FOR_COMMAND, coordinator.state)
    }

    @Test
    fun postWakeWindow_acceptsNextCommandWithoutWakePrefix() {
        // Step 1: Wake phrase triggers post-wake window
        coordinator.processUtterance("Hey SAARTHI")
        assertEquals(WakeWordState.LISTENING_FOR_COMMAND, coordinator.state)

        // Step 2: Next utterance is accepted as command without another tap or wake word
        val result = coordinator.processUtterance("what time is it")
        assertTrue(result is UtteranceRoutingResult.PostWakeCommand)
        assertEquals("what time is it", (result as UtteranceRoutingResult.PostWakeCommand).command)
        assertEquals(WakeWordState.PROCESSING_COMMAND, coordinator.state)
    }

    @Test
    fun postWakeWindow_timesOutQuietly() {
        var timeoutFired = false
        coordinator.onPostWakeWindowTimedOut = {
            timeoutFired = true
        }

        coordinator.processUtterance("Hey SAARTHI")
        assertEquals(WakeWordState.LISTENING_FOR_COMMAND, coordinator.state)

        // Simulate post-wake window timeout
        coordinator.onPostWakeTimeout()
        assertTrue(timeoutFired)
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    @Test
    fun singleUtterance_wakeWithCommand_executesDirectly() {
        val result = coordinator.processUtterance("Hey SAARTHI, open calculator")
        assertTrue(result is UtteranceRoutingResult.CommandReady)
        val cmd = (result as UtteranceRoutingResult.CommandReady).command
        assertEquals("open calculator", cmd)
        assertEquals(WakeWordState.PROCESSING_COMMAND, coordinator.state)
    }

    @Test
    fun duplicateExecution_debouncesRapidRepeats() {
        val t0 = 10000L
        val res1 = coordinator.processUtterance("Hey SAARTHI, open calculator", currentTimeMs = t0)
        assertTrue(res1 is UtteranceRoutingResult.CommandReady)

        // Rapid duplicate 400ms later while processing -> dropped
        val res2 = coordinator.processUtterance("Hey SAARTHI, open calculator", currentTimeMs = t0 + 400)
        assertTrue(res2 is UtteranceRoutingResult.DuplicateDropped)

        // Finished processing -> returns to standby
        coordinator.onTtsFinished()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)

        // Same command 600ms later in standby (within 1200ms debounce) -> dropped
        val res3 = coordinator.processUtterance("Hey SAARTHI, open calculator", currentTimeMs = t0 + 600)
        assertTrue(res3 is UtteranceRoutingResult.DuplicateDropped)

        // Different command 700ms later -> accepted
        val res4 = coordinator.processUtterance("Hey SAARTHI, what time is it", currentTimeMs = t0 + 700)
        assertTrue(res4 is UtteranceRoutingResult.CommandReady)
    }

    @Test
    fun sensitiveAction_requiresConfirmation_andHandlesConfirm() {
        // Command asking to call someone
        val cmdResult = commandEngine.process("call mom")
        assertTrue(cmdResult is CommandResult.SensitiveConfirmation)
        val sens = cmdResult as CommandResult.SensitiveConfirmation
        assertTrue(sens.action is SensitiveAction.PlacePhoneCall)
        assertEquals("mom", (sens.action as SensitiveAction.PlacePhoneCall).recipient)

        // Coordinator enters confirmation state
        coordinator.requestConfirmation(sens.action, sens.confirmationPrompt)
        assertEquals(WakeWordState.AWAITING_CONFIRMATION, coordinator.state)

        // User says "yes" to confirm
        val confirmResult = coordinator.processUtterance("yes")
        assertTrue(confirmResult is UtteranceRoutingResult.ConfirmationResponse)
        val resp = confirmResult as UtteranceRoutingResult.ConfirmationResponse
        assertTrue(resp.confirmed)
        assertEquals(sens.action, resp.action)
    }

    @Test
    fun sensitiveAction_cancelledGracefullyOnNo() {
        val sensitiveAction = SensitiveAction.PlacePhoneCall("doctor")
        coordinator.requestConfirmation(sensitiveAction, "Confirm call?")
        assertEquals(WakeWordState.AWAITING_CONFIRMATION, coordinator.state)

        // User says "cancel"
        val cancelResult = coordinator.processUtterance("cancel")
        assertTrue(cancelResult is UtteranceRoutingResult.ConfirmationResponse)
        val resp = cancelResult as UtteranceRoutingResult.ConfirmationResponse
        assertFalse(resp.confirmed)
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
    }

    @Test
    fun sensitiveAction_timeoutClearsActionQuietly() {
        val sensitiveAction = SensitiveAction.ClearData("history")
        coordinator.requestConfirmation(sensitiveAction, "Confirm clear data?")
        assertEquals(WakeWordState.AWAITING_CONFIRMATION, coordinator.state)

        coordinator.onPostWakeTimeout()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
    }

    @Test
    fun sensitiveAction_ambientSpeechCannotTriggerCall() {
        // Ambient speech mentioning "call" without wake phrase is ignored in standby
        val result = coordinator.processUtterance("I will call you back later")
        assertEquals(UtteranceRoutingResult.Ignored, result)

        // Even with wake phrase, non-imperative sentences are not parsed as call commands
        val resultWithWake = commandEngine.process("what do you call this")
        assertFalse(resultWithWake is CommandResult.SensitiveConfirmation)
    }

    @Test
    fun ttsMutualExclusion_stopsDetectorMicrophone() {
        coordinator.processUtterance("Hey SAARTHI, what time is it")
        coordinator.onTtsStarted()
        assertEquals(WakeWordState.TTS_SPEAKING, coordinator.state)
        assertFalse(detector.isListening())

        coordinator.onTtsFinished()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    // -------------------------------------------------------------
    // Wake-Word Matching vs Genuine Acoustic Detection Contract
    // -------------------------------------------------------------
    @Test
    fun wakeWordDetector_isTranscriptBasedMatcher_notRawAcousticModel() {
        // Confirms LocalWakeWordDetector operates strictly on text phrases
        assertEquals("LocalOfflinePhraseMatcher", detector.engineName)
        assertTrue(detector.isAvailable)

        val (hasWake, cmd) = LocalWakeWordDetector.extractWakeWordAndCommand("Hey SAARTHI, what is the battery")
        assertTrue(hasWake)
        assertEquals("what is the battery", cmd)

        // Non-matching text is rejected
        val (noWake, _) = LocalWakeWordDetector.extractWakeWordAndCommand("Hello Google")
        assertFalse(noWake)
    }

    @Test
    fun acousticWakeWordEngine_verifiesAudioParametersAndInferenceContract() {
        val fakeAcousticEngine = object : AcousticWakeWordEngine {
            var framesProcessed = 0
            var isReset = false
            override fun processFrame(pcmBuffer: ShortArray, offset: Int, length: Int): Float {
                framesProcessed++
                return if (length == frameSizeSamples && pcmBuffer[offset] > 0) 0.92f else 0.05f
            }
            override fun reset() { isReset = true }
            override fun release() {}
        }

        // Standard 16 kHz Mono PCM audio parameters
        assertEquals(16000, fakeAcousticEngine.sampleRateHz)
        assertEquals(1280, fakeAcousticEngine.frameSizeSamples) // 80 ms window

        val dummyPcm = ShortArray(1280) { 100 }
        val confidence = fakeAcousticEngine.processFrame(dummyPcm, 0, 1280)
        assertEquals(0.92f, confidence, 0.001f)
        assertEquals(1, fakeAcousticEngine.framesProcessed)

        fakeAcousticEngine.reset()
        assertTrue(fakeAcousticEngine.isReset)
    }
}
