package com.saarthi.assistant.telephony

import com.saarthi.assistant.voice.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages audible announcements for incoming calls.
 *
 * Enforces:
 * 1. Speaker announcement by default when no headset/earbuds are connected.
 * 2. Headset/earbud routing when wired or Bluetooth earphones are attached.
 * 3. User privacy controls (honors disabling speaker announcements).
 * 4. Contact resolution vs "Unknown number" announcements.
 * 5. Repetition caps (max 2) to prevent endless talking during ringing.
 * 6. Immediate cancellation when answered, rejected, or hung up.
 * 7. Mutual exclusion and duplicate suppression.
 */
class SmartCallerAnnouncer(
    private val speechSynthesizer: SpeechSynthesizer,
    private val audioRouteDetector: AudioRouteDetector,
    private val preferencesManager: CallPreferencesManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val maxAnnouncements: Int = 2,
    private val repetitionIntervalMs: Long = 3000L
) {

    var isAnnouncing: Boolean = false
        private set

    var currentAnnouncementText: String? = null
        private set

    var announcementCount: Int = 0
        private set

    private var announcementJob: Job? = null

    /**
     * Formats the spoken announcement text according to caller info and preferences.
     */
    fun formatAnnouncement(
        callerName: String?,
        incomingNumber: String?,
        announceUnknown: Boolean
    ): String? {
        val trimmedName = callerName?.trim()
        if (!trimmedName.isNullOrBlank()) {
            return "$trimmedName is calling."
        }

        if (announceUnknown) {
            return "Unknown number is calling."
        }

        return null
    }

    /**
     * Evaluates gating conditions and starts spoken announcement if allowed.
     */
    fun startAnnouncement(callerName: String?, incomingNumber: String?) {
        val prefs = preferencesManager.getPreferences()

        if (!prefs.callerAnnouncementEnabled) {
            return
        }

        // Check current audio routing hardware
        val isHeadset = audioRouteDetector.isEarbudOrHeadsetConnected()

        if (isHeadset) {
            // Audio output routed to connected wired or Bluetooth headset
            if (!prefs.earbudsAnnouncementEnabled) {
                return
            }
        } else {
            // Audio output routed to phone speaker (enabled by default)
            if (!prefs.speakerAnnouncementEnabled) {
                return
            }
        }

        val text = formatAnnouncement(
            callerName = callerName,
            incomingNumber = incomingNumber,
            announceUnknown = prefs.announceUnknownNumbers
        ) ?: return

        stopAnnouncement()

        isAnnouncing = true
        currentAnnouncementText = text
        announcementCount = 0

        announcementJob = scope.launch {
            for (i in 1..maxAnnouncements) {
                if (!isAnnouncing) break
                announcementCount = i
                speechSynthesizer.speak(text)

                if (i < maxAnnouncements) {
                    delay(repetitionIntervalMs)
                }
            }
            isAnnouncing = false
        }
    }

    /**
     * Immediately terminates any active announcement and stops TTS.
     */
    fun stopAnnouncement() {
        isAnnouncing = false
        announcementJob?.cancel()
        announcementJob = null
        currentAnnouncementText = null
        speechSynthesizer.stop()
    }
}
