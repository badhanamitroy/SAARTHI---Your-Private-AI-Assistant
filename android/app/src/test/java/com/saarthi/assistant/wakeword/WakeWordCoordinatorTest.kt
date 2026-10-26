package com.saarthi.assistant.wakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WakeWordCoordinatorTest {

    private lateinit var detector: LocalWakeWordDetector
    private lateinit var coordinator: WakeWordCoordinator

    @Before
    fun setUp() {
        detector = LocalWakeWordDetector()
        coordinator = WakeWordCoordinator(detector)
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(WakeWordState.IDLE, coordinator.state)
        assertFalse(detector.isListening())
    }

    @Test
    fun start_transitionsToListeningForWakeWord() {
        coordinator.start()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    @Test
    fun wakeWordDetected_triggersHandoffAndReleasesDetectorMic() {
        var triggeredPhrase = ""
        var handoffReady = false

        coordinator.onWakeWordTriggered = { phrase ->
            triggeredPhrase = phrase
        }
        coordinator.onCommandReadyToRecord = {
            handoffReady = true
        }

        coordinator.start()
        detector.simulateDetection("Hey SAARTHI")

        assertEquals("Hey SAARTHI", triggeredPhrase)
        assertTrue(handoffReady)
        // Detector must be stopped during handoff to release audio hardware
        assertFalse(detector.isListening())
        assertEquals(WakeWordState.HANDOFF_TO_SPEECH_RECOGNIZER, coordinator.state)
    }

    @Test
    fun speechRecognitionLifecycle_transitionsCorrectly() {
        coordinator.start()
        detector.simulateDetection("Hey SAARTHI")

        // Speech recognition starts recording command
        coordinator.onSpeechRecognitionStarted()
        assertEquals(WakeWordState.SPEECH_RECOGNITION_ACTIVE, coordinator.state)

        // Command recognized -> transitions to processing
        coordinator.onSpeechRecognitionFinished(hasCommand = true)
        assertEquals(WakeWordState.PROCESSING_COMMAND, coordinator.state)
    }

    @Test
    fun speechRecognitionTimeout_resumesWakeWordListening() {
        coordinator.start()
        detector.simulateDetection("Hey SAARTHI")

        coordinator.onSpeechRecognitionStarted()
        assertEquals(WakeWordState.SPEECH_RECOGNITION_ACTIVE, coordinator.state)

        // Timeout (no command heard) -> automatically returns to listening for wake-word
        coordinator.onSpeechRecognitionFinished(hasCommand = false)
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    @Test
    fun ttsCoordination_enforcesMutualExclusion() {
        coordinator.start()
        detector.simulateDetection("Hey SAARTHI")
        coordinator.onSpeechRecognitionStarted()
        coordinator.onSpeechRecognitionFinished(hasCommand = true)

        // TTS starts speaking response
        coordinator.onTtsStarted()
        assertEquals(WakeWordState.TTS_SPEAKING, coordinator.state)
        // Wake-word detector must NOT be active while TTS speaks (prevents acoustic feedback loop)
        assertFalse(detector.isListening())

        // TTS completes playback
        coordinator.onTtsFinished()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    @Test
    fun pauseAndResume_controlsListeningState() {
        coordinator.start()
        assertTrue(detector.isListening())

        coordinator.pause()
        assertEquals(WakeWordState.PAUSED, coordinator.state)
        assertFalse(detector.isListening())

        coordinator.resume()
        assertEquals(WakeWordState.LISTENING_FOR_WAKE_WORD, coordinator.state)
        assertTrue(detector.isListening())
    }

    @Test
    fun stop_releasesResourcesToIdle() {
        coordinator.start()
        coordinator.stop()

        assertEquals(WakeWordState.IDLE, coordinator.state)
        assertFalse(detector.isListening())
    }

    @Test
    fun errorHandling_transitionsToErrorState() {
        coordinator.start()
        detector.simulateError("Microphone permission denied")

        assertEquals(WakeWordState.ERROR, coordinator.state)
    }
}
