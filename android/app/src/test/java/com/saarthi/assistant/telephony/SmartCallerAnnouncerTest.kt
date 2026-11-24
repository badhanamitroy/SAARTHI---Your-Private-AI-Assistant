package com.saarthi.assistant.telephony

import com.saarthi.assistant.voice.SpeechSynthesizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartCallerAnnouncerTest {

    private class FakeSpeechSynthesizer : SpeechSynthesizer {
        override var onSpeechStart: (() -> Unit)? = null
        override var onSpeechDone: (() -> Unit)? = null
        override var onSpeechError: ((String) -> Unit)? = null

        val spokenUtterances = mutableListOf<String>()
        var stopCount = 0

        override fun isInitialized(): Boolean = true

        override fun speak(text: String) {
            spokenUtterances.add(text)
            onSpeechStart?.invoke()
            onSpeechDone?.invoke()
        }

        override fun stop() {
            stopCount++
        }

        override fun release() {}
    }

    private lateinit var fakeSynthesizer: FakeSpeechSynthesizer
    private lateinit var fakeRouteDetector: FakeAudioRouteDetector
    private lateinit var fakePrefs: InMemoryCallPreferencesManager

    @Before
    fun setUp() {
        fakeSynthesizer = FakeSpeechSynthesizer()
        fakeRouteDetector = FakeAudioRouteDetector(activeRoute = AudioRoute.SPEAKER)
        fakePrefs = InMemoryCallPreferencesManager(
            CallPreferences(
                callerAnnouncementEnabled = true,
                speakerAnnouncementEnabled = true,
                earbudsAnnouncementEnabled = true,
                voiceCallControlEnabled = true,
                announceUnknownNumbers = true
            )
        )
    }

    @Test
    fun formatAnnouncement_knownCaller_formatsName() {
        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        val result = announcer.formatAnnouncement("Amit Hasan", "+8801712345678", announceUnknown = true)
        assertEquals("Amit Hasan is calling.", result)
    }

    @Test
    fun formatAnnouncement_unknownCallerAllowed_formatsUnknownNumber() {
        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        val result = announcer.formatAnnouncement(null, "+8801999999999", announceUnknown = true)
        assertEquals("Unknown number is calling.", result)
    }

    @Test
    fun formatAnnouncement_unknownCallerDisallowed_returnsNull() {
        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        val result = announcer.formatAnnouncement(null, "+8801999999999", announceUnknown = false)
        assertNull(result)
    }

    @Test
    fun startAnnouncement_defaultPreferences_announcesThroughSpeakerWhenNoEarbuds() = runBlocking {
        // Disconnected earbuds (route = SPEAKER) + default settings (speakerAnnouncementEnabled = true)
        fakeRouteDetector.activeRoute = AudioRoute.SPEAKER

        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        announcer.startAnnouncement("Amit Hasan", "+8801712345678")
        Thread.sleep(50)

        // Must announce over speaker by default when no earbuds are connected
        assertEquals(1, fakeSynthesizer.spokenUtterances.size)
        assertEquals("Amit Hasan is calling.", fakeSynthesizer.spokenUtterances[0])
    }

    @Test
    fun startAnnouncement_defaultPreferences_announcesThroughEarbudsWhenEarbudsConnected() = runBlocking {
        // Connected Bluetooth earbuds + default settings (earbudsAnnouncementEnabled = true)
        fakeRouteDetector.activeRoute = AudioRoute.BLUETOOTH_EARBUD

        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        announcer.startAnnouncement("Mother", "+8801811111111")
        Thread.sleep(50)

        // Must announce over earbuds
        assertEquals(1, fakeSynthesizer.spokenUtterances.size)
        assertEquals("Mother is calling.", fakeSynthesizer.spokenUtterances[0])
    }

    @Test
    fun startAnnouncement_speakerDisabled_remainsSilentWhenNoEarbuds() = runBlocking {
        // User explicitly turned off speaker announcement for privacy
        fakePrefs.setSpeakerAnnouncementEnabled(false)
        fakeRouteDetector.activeRoute = AudioRoute.SPEAKER

        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        announcer.startAnnouncement("Amit Hasan", "+8801712345678")
        Thread.sleep(50)

        // Must remain silent when speaker announcement is disabled and earbuds are disconnected
        assertTrue(fakeSynthesizer.spokenUtterances.isEmpty())
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun startAnnouncement_earbudsDisabled_remainsSilentWhenEarbudsConnected() = runBlocking {
        // User explicitly turned off earbuds announcement
        fakePrefs.setEarbudsAnnouncementEnabled(false)
        fakeRouteDetector.activeRoute = AudioRoute.WIRED_HEADSET

        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        announcer.startAnnouncement("Amit Hasan", "+8801712345678")
        Thread.sleep(50)

        assertTrue(fakeSynthesizer.spokenUtterances.isEmpty())
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun startAnnouncement_announcementDisabled_doesNotSpeak() = runBlocking {
        fakePrefs.setCallerAnnouncementEnabled(false)

        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs
        )

        announcer.startAnnouncement("Mother", "+8801811111111")
        Thread.sleep(50)

        assertTrue(fakeSynthesizer.spokenUtterances.isEmpty())
        assertFalse(announcer.isAnnouncing)
    }

    @Test
    fun startAnnouncement_repetition_cappedAtMaxAnnouncements() = runBlocking {
        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs,
            maxAnnouncements = 2,
            repetitionIntervalMs = 30L
        )

        announcer.startAnnouncement("Amit Hasan", "+8801712345678")

        // Wait for first announcement and second announcement
        Thread.sleep(120)

        // Capped at exactly 2
        assertEquals(2, fakeSynthesizer.spokenUtterances.size)
        assertEquals("Amit Hasan is calling.", fakeSynthesizer.spokenUtterances[0])
        assertEquals("Amit Hasan is calling.", fakeSynthesizer.spokenUtterances[1])
    }

    @Test
    fun stopAnnouncement_immediatelyTerminatesSpeaking() = runBlocking {
        val announcer = SmartCallerAnnouncer(
            speechSynthesizer = fakeSynthesizer,
            audioRouteDetector = fakeRouteDetector,
            preferencesManager = fakePrefs,
            maxAnnouncements = 2,
            repetitionIntervalMs = 200L
        )

        announcer.startAnnouncement("Father", "+8801700000000")
        Thread.sleep(30)
        assertEquals(1, fakeSynthesizer.spokenUtterances.size)

        // Call stopped by user answering or rejecting
        announcer.stopAnnouncement()
        assertFalse(announcer.isAnnouncing)
        assertTrue(fakeSynthesizer.stopCount >= 1)

        // Ensure no 2nd announcement fires
        Thread.sleep(300)
        assertEquals(1, fakeSynthesizer.spokenUtterances.size)
    }
}
