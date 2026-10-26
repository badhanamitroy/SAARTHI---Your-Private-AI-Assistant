package com.saarthi.assistant.voice

interface VoiceEngine {

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun stopListening()

    fun cancelListening()

    fun isListening(): Boolean
}