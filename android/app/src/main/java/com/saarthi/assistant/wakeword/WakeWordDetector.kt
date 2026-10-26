package com.saarthi.assistant.wakeword

/**
 * Interface abstraction for local wake-phrase detection and command coordination.
 * Implementations must work 100% offline with zero network permissions or cloud keys.
 */
interface WakeWordDetector {

    /** Name / identification of the detector engine. */
    val engineName: String

    /** Indicates whether the detector and required dependencies are loaded and ready. */
    val isAvailable: Boolean

    /**
     * Start listening for configured wake phrases (e.g. "Hey SAARTHI", "Ok SAARTHI").
     */
    fun startListening(
        onWakeWordDetected: (wakePhrase: String) -> Unit,
        onError: (String) -> Unit
    )

    /** Stop listening and release microphone access. */
    fun stopListening()

    /** Check if the detector is currently actively listening. */
    fun isListening(): Boolean

    /** Release native resources, audio buffers, or model memory. */
    fun release()
}
