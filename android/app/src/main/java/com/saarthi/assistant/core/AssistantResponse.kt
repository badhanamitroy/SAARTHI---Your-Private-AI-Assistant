package com.saarthi.assistant.core

data class AssistantResponse(
    val text: String,
    val shouldSpeak: Boolean = true,
    val shouldStopSpeaking: Boolean = false,
    val requiresOnlineAi: Boolean = false
)