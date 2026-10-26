package com.saarthi.assistant.wakeword

/**
 * Explicit lifecycle states for offline wake-word detection and natural command coordination.
 * Ensures mutually exclusive access to the microphone between wake-word listening,
 * speech recognition, and TTS audio playback, while avoiding accidental triggers.
 */
enum class WakeWordState {
    /** Initial or released state; no audio hardware active. */
    IDLE,

    /** Monitoring for wake phrases ("Hey SAARTHI", "Ok SAARTHI"). Ordinary speech ignored. */
    LISTENING_FOR_WAKE_WORD,

    /** Wake phrase detected by the engine; prepares handoff or post-wake window. */
    WAKE_WORD_DETECTED,

    /** Post-wake active listening window; waiting for user command without requiring a tap. */
    LISTENING_FOR_COMMAND,

    /** Sensitive action requested; awaiting explicit user confirmation (yes/no). */
    AWAITING_CONFIRMATION,

    /** Wake-word listening paused; releasing microphone before speech recognition starts. */
    HANDOFF_TO_SPEECH_RECOGNIZER,

    /** Main speech recognition engine active to record user command. */
    SPEECH_RECOGNITION_ACTIVE,

    /** Command received; offline CommandEngine processing intent. */
    PROCESSING_COMMAND,

    /** TextToSpeech speaking reply; microphone suspended to prevent acoustic feedback. */
    TTS_SPEAKING,

    /** Optional short follow-up listening window for consecutive commands. */
    FOLLOW_UP_ACTIVE,

    /** Cooldown period after TTS playback before resuming wake-word listening. */
    COOLDOWN,

    /** Background listening paused by user or system. */
    PAUSED,

    /** Engine encountered a recoverable or fatal error. */
    ERROR
}
