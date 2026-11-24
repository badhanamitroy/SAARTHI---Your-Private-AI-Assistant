package com.saarthi.assistant.telephony

import android.content.Context
import android.content.SharedPreferences

/**
 * User configuration for hands-free incoming call handling.
 *
 * Operational Defaults:
 * - callerAnnouncementEnabled: true (Automatically announce incoming calls)
 * - speakerAnnouncementEnabled: true (Play through phone speaker by default when no headset is connected)
 * - earbudsAnnouncementEnabled: true (Play through connected wired/Bluetooth headset)
 * - voiceCallControlEnabled: true (Answer/Reject via voice)
 * - announceUnknownNumbers: true (Announce unknown caller when unlisted)
 */
data class CallPreferences(
    val callerAnnouncementEnabled: Boolean = true,
    val speakerAnnouncementEnabled: Boolean = true,
    val earbudsAnnouncementEnabled: Boolean = true,
    val voiceCallControlEnabled: Boolean = true,
    val announceUnknownNumbers: Boolean = true
) {
    /**
     * Backward-compatible property: true when speaker announcements are suppressed
     * and only headset/earbud announcements are permitted.
     */
    val earbudsOnlyAnnouncement: Boolean
        get() = !speakerAnnouncementEnabled && earbudsAnnouncementEnabled
}

interface CallPreferencesManager {
    fun getPreferences(): CallPreferences
    fun setCallerAnnouncementEnabled(enabled: Boolean)
    fun setSpeakerAnnouncementEnabled(enabled: Boolean)
    fun setEarbudsAnnouncementEnabled(enabled: Boolean)
    fun setVoiceCallControlEnabled(enabled: Boolean)
    fun setAnnounceUnknownNumbers(enabled: Boolean)

    // Backward-compatibility helper for legacy callers
    fun setEarbudsOnlyAnnouncement(enabled: Boolean) {
        setSpeakerAnnouncementEnabled(!enabled)
        setEarbudsAnnouncementEnabled(true)
    }
}

class AndroidCallPreferencesManager(context: Context) : CallPreferencesManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getPreferences(): CallPreferences {
        // If legacy KEY_EARBUDS_ONLY was previously stored, map it to speaker announcement
        val defaultSpeaker = if (prefs.contains(KEY_EARBUDS_ONLY)) {
            !prefs.getBoolean(KEY_EARBUDS_ONLY, false)
        } else {
            true
        }

        return CallPreferences(
            callerAnnouncementEnabled = prefs.getBoolean(KEY_CALLER_ANNOUNCEMENT, true),
            speakerAnnouncementEnabled = prefs.getBoolean(KEY_SPEAKER_ANNOUNCEMENT, defaultSpeaker),
            earbudsAnnouncementEnabled = prefs.getBoolean(KEY_EARBUDS_ANNOUNCEMENT, true),
            voiceCallControlEnabled = prefs.getBoolean(KEY_VOICE_CALL_CONTROL, true),
            announceUnknownNumbers = prefs.getBoolean(KEY_ANNOUNCE_UNKNOWN, true)
        )
    }

    override fun setCallerAnnouncementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALLER_ANNOUNCEMENT, enabled).apply()
    }

    override fun setSpeakerAnnouncementEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SPEAKER_ANNOUNCEMENT, enabled)
            .putBoolean(KEY_EARBUDS_ONLY, !enabled)
            .apply()
    }

    override fun setEarbudsAnnouncementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EARBUDS_ANNOUNCEMENT, enabled).apply()
    }

    override fun setVoiceCallControlEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_CALL_CONTROL, enabled).apply()
    }

    override fun setAnnounceUnknownNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANNOUNCE_UNKNOWN, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "saarthi_telephony_prefs"
        private const val KEY_CALLER_ANNOUNCEMENT = "caller_announcement_enabled"
        private const val KEY_SPEAKER_ANNOUNCEMENT = "speaker_announcement_enabled"
        private const val KEY_EARBUDS_ANNOUNCEMENT = "earbuds_announcement_enabled"
        private const val KEY_EARBUDS_ONLY = "earbuds_only_announcement"
        private const val KEY_VOICE_CALL_CONTROL = "voice_call_control_enabled"
        private const val KEY_ANNOUNCE_UNKNOWN = "announce_unknown_numbers"
    }
}

class InMemoryCallPreferencesManager(
    initial: CallPreferences = CallPreferences()
) : CallPreferencesManager {

    private var current = initial

    override fun getPreferences(): CallPreferences = current

    override fun setCallerAnnouncementEnabled(enabled: Boolean) {
        current = current.copy(callerAnnouncementEnabled = enabled)
    }

    override fun setSpeakerAnnouncementEnabled(enabled: Boolean) {
        current = current.copy(speakerAnnouncementEnabled = enabled)
    }

    override fun setEarbudsAnnouncementEnabled(enabled: Boolean) {
        current = current.copy(earbudsAnnouncementEnabled = enabled)
    }

    override fun setVoiceCallControlEnabled(enabled: Boolean) {
        current = current.copy(voiceCallControlEnabled = enabled)
    }

    override fun setAnnounceUnknownNumbers(enabled: Boolean) {
        current = current.copy(announceUnknownNumbers = enabled)
    }
}
