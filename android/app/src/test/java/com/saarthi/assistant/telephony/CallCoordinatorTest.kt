package com.saarthi.assistant.telephony

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.ExecutionContract
import com.saarthi.assistant.voice.SpeechSynthesizer
import com.saarthi.assistant.voice.VoiceEngine
import com.saarthi.assistant.voice.VoiceSessionCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallCoordinatorTest {

    private class TestVoiceEngine : VoiceEngine {
        var listeningState = false
        var stopCount = 0
        override fun startListening(
            onResult: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            listeningState = true
        }
        override fun stopListening() {
            listeningState = false
            stopCount++
        }
        override fun cancelListening() {
            listeningState = false
        }
        override fun isListening(): Boolean = listeningState
        override fun release() {}
    }

    private class TestSynthesizer : SpeechSynthesizer {
        override var onSpeechStart: (() -> Unit)? = null
        override var onSpeechDone: (() -> Unit)? = null
        override var onSpeechError: ((String) -> Unit)? = null
        val spoken = mutableListOf<String>()
        var stopCount = 0
        override fun isInitialized(): Boolean = true
        override fun speak(text: String) { spoken.add(text) }
        override fun stop() { stopCount++ }
        override fun release() {}
    }

    private lateinit var contactResolver: FakeContactResolver
    private lateinit var audioRouteDetector: FakeAudioRouteDetector
    private lateinit var prefsManager: InMemoryCallPreferencesManager
    private lateinit var callController: FakeCallController
    private lateinit var announcer: SmartCallerAnnouncer
    private lateinit var voiceEngine: TestVoiceEngine
    private lateinit var synthesizer: TestSynthesizer
    private lateinit var voiceCoordinator: VoiceSessionCoordinator
    private lateinit var callCoordinator: CallCoordinator

    @Before
    fun setUp() {
        contactResolver = FakeContactResolver(
            mapOf(
                "+8801712345678" to "Amit Hasan",
                "+8801811111111" to "Mother"
            )
        )
        audioRouteDetector = FakeAudioRouteDetector(AudioRoute.SPEAKER)
        prefsManager = InMemoryCallPreferencesManager()
        callController = FakeCallController()
        synthesizer = TestSynthesizer()
        voiceEngine = TestVoiceEngine()
        voiceCoordinator = VoiceSessionCoordinator(
            voiceEngine = voiceEngine,
            speechSynthesizer = synthesizer,
            commandEngine = CommandEngine()
        )
        announcer = SmartCallerAnnouncer(
            speechSynthesizer = synthesizer,
            audioRouteDetector = audioRouteDetector,
            preferencesManager = prefsManager
        )
        callCoordinator = CallCoordinator(
            contactResolver = contactResolver,
            audioRouteDetector = audioRouteDetector,
            callPreferencesManager = prefsManager,
            callController = callController,
            smartCallerAnnouncer = announcer,
            voiceSessionCoordinator = voiceCoordinator
        )
    }

    @Test
    fun onCallRinging_resolvesContactNameAndPausesVoiceSession() {
        voiceCoordinator.startSession()
        assertTrue(voiceCoordinator.isSessionActive)

        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")

        assertEquals(CallState.RINGING, callCoordinator.currentCallState)
        val details = callCoordinator.currentCallDetails
        assertNotNull(details)
        assertEquals("Amit Hasan", details?.callerName)
        assertEquals("+8801712345678", details?.incomingNumber)

        // Pre-call voice session listening is paused during ringing
        assertFalse(voiceEngine.isListening())
    }

    @Test
    fun onCallOffhook_stopsAnnouncementAndUpdatesState() {
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801811111111")
        callCoordinator.onCallStateChanged(CallState.OFFHOOK, "+8801811111111")

        assertEquals(CallState.OFFHOOK, callCoordinator.currentCallState)
        assertFalse(announcer.isAnnouncing)
        assertTrue(synthesizer.stopCount >= 1)
    }

    @Test
    fun onCallIdle_stopsAnnouncementAndRestoresPreCallVoiceSession() {
        voiceCoordinator.startSession()

        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")
        assertFalse(voiceEngine.isListening())

        callCoordinator.onCallStateChanged(CallState.IDLE, "+8801712345678")
        assertEquals(CallState.IDLE, callCoordinator.currentCallState)
        assertNull(callCoordinator.currentCallDetails)

        // Restores active voice session
        assertTrue(voiceCoordinator.isSessionActive)
        assertTrue(voiceEngine.isListening())
    }

    @Test
    fun handleVoiceCallAnswer_stopsAnnouncementAndInvokesCallController() {
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")

        val result = callCoordinator.handleVoiceCallAnswer()
        assertTrue(result is ExecutionContract.Succeeded)
        assertTrue(callController.answerCallInvoked)
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun handleVoiceCallReject_stopsAnnouncementAndInvokesCallController() {
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")

        val result = callCoordinator.handleVoiceCallReject()
        assertTrue(result is ExecutionContract.Succeeded)
        assertTrue(callController.rejectCallInvoked)
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun onCallRinging_whenAlreadyOffhook_doesNotAnnounceOrInterruptActiveCall() {
        // Active call already underway
        callCoordinator.onCallStateChanged(CallState.OFFHOOK, "+8801712345678")
        assertEquals(CallState.OFFHOOK, callCoordinator.currentCallState)
        val initialSpokenCount = synthesizer.spoken.size

        // Incoming Call Waiting arrives
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801811111111")
        Thread.sleep(50)

        // State remains OFFHOOK and no new announcement spoken
        assertEquals(CallState.OFFHOOK, callCoordinator.currentCallState)
        assertEquals(initialSpokenCount, synthesizer.spoken.size)
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun onCallRinging_duplicateBroadcastSameCall_doesNotTriggerDuplicateAnnouncement() {
        // Initial broadcast for ringing call
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")
        Thread.sleep(50)
        assertEquals(1, synthesizer.spoken.size)

        // Duplicate broadcast with identical state and number
        callCoordinator.onCallStateChanged(CallState.RINGING, "+8801712345678")
        Thread.sleep(50)

        // Spoken count does not increase because duplicate was rejected
        assertEquals(1, synthesizer.spoken.size)
    }
}
