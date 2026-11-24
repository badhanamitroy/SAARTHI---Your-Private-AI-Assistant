package com.saarthi.assistant.voice

/**
 * Decoupled interface for speech synthesis / text-to-speech output.
 * Allows pure JVM unit testing of voice sessions and mutual exclusion without Android TTS engine.
 */
interface SpeechSynthesizer {
    var onSpeechStart: (() -> Unit)?
    var onSpeechDone: (() -> Unit)?
    var onSpeechError: ((String) -> Unit)?

    fun isInitialized(): Boolean
    fun speak(text: String)
    fun stop()
    fun release()
}
