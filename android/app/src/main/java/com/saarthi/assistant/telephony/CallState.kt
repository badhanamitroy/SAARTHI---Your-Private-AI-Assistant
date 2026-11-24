package com.saarthi.assistant.telephony

/**
 * Representation of phone call states.
 */
enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK
}

/**
 * Audio output routing options for hands-free caller announcements.
 */
enum class AudioRoute {
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH_EARBUD
}

/**
 * Snapshot of incoming or ongoing call details.
 */
data class CallDetails(
    val state: CallState,
    val incomingNumber: String? = null,
    val callerName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

