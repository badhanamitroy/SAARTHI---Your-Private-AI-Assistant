package com.saarthi.assistant.telephony

import com.saarthi.assistant.core.ExecutionContract
import com.saarthi.assistant.voice.VoiceSessionCoordinator

/**
 * Coordinates incoming call detection, smart caller announcement,
 * hands-free voice answering/rejecting, and voice session turn-taking.
 *
 * Rules:
 * 1. Automatically announces incoming calls through phone speaker or earbuds by default.
 * 2. Does NOT interrupt an active, in-progress call with an incoming announcement (Call Waiting).
 * 3. Suppresses duplicate broadcasts for the same ongoing ringing event.
 * 4. Stops speech immediately when call is answered, rejected, or disconnected.
 * 5. Restores voice session listening after call completes if previously active.
 */
class CallCoordinator(
    val contactResolver: ContactResolver,
    val audioRouteDetector: AudioRouteDetector,
    val callPreferencesManager: CallPreferencesManager,
    val callController: CallController,
    val smartCallerAnnouncer: SmartCallerAnnouncer,
    private val voiceSessionCoordinator: VoiceSessionCoordinator? = null
) {

    var currentCallState: CallState = CallState.IDLE
        private set

    var currentCallDetails: CallDetails? = null
        private set

    private var wasVoiceSessionActiveBeforeCall: Boolean = false

    var onCallDetailsChanged: ((CallDetails?) -> Unit)? = null

    init {
        instance = this
    }

    /**
     * Entry point invoked when TelephonyManager state changes.
     */
    fun onCallStateChanged(state: CallState, incomingNumber: String?) {
        val previousState = currentCallState

        when (state) {
            CallState.RINGING -> {
                // Requirement 8: Do not interrupt an active call with an incoming-call announcement
                if (previousState == CallState.OFFHOOK) {
                    return
                }

                // Requirement 6: Duplicate suppression for repeated broadcasts of the same ringing call
                if (previousState == CallState.RINGING && currentCallDetails?.incomingNumber == incomingNumber) {
                    return
                }

                currentCallState = CallState.RINGING
                handleRinging(incomingNumber)
            }
            CallState.OFFHOOK -> {
                currentCallState = CallState.OFFHOOK
                handleOffhook()
            }
            CallState.IDLE -> {
                currentCallState = CallState.IDLE
                handleIdle(previousState)
            }
        }
    }

    private fun handleRinging(incomingNumber: String?) {
        // Track pre-call voice session state
        wasVoiceSessionActiveBeforeCall = voiceSessionCoordinator?.isSessionActive == true

        // Pause active assistant listening so ringing/announcement doesn't trigger spurious commands
        voiceSessionCoordinator?.pauseListening()

        val resolvedName = contactResolver.resolveContactName(incomingNumber)
        val details = CallDetails(
            state = CallState.RINGING,
            incomingNumber = incomingNumber,
            callerName = resolvedName
        )
        currentCallDetails = details
        onCallDetailsChanged?.invoke(details)

        // Trigger smart announcement
        smartCallerAnnouncer.startAnnouncement(
            callerName = resolvedName,
            incomingNumber = incomingNumber
        )
    }

    private fun handleOffhook() {
        smartCallerAnnouncer.stopAnnouncement()
        val details = currentCallDetails?.copy(state = CallState.OFFHOOK) ?: CallDetails(state = CallState.OFFHOOK)
        currentCallDetails = details
        onCallDetailsChanged?.invoke(details)
    }

    private fun handleIdle(previousState: CallState) {
        smartCallerAnnouncer.stopAnnouncement()
        currentCallDetails = null
        onCallDetailsChanged?.invoke(null)

        // Restore voice session if it was running before the call arrived
        if (wasVoiceSessionActiveBeforeCall && previousState != CallState.IDLE) {
            wasVoiceSessionActiveBeforeCall = false
            voiceSessionCoordinator?.resumeListening()
        }
    }

    /**
     * Executes voice-controlled call answer.
     */
    fun handleVoiceCallAnswer(): ExecutionContract {
        smartCallerAnnouncer.stopAnnouncement()
        return callController.answerCall()
    }

    /**
     * Executes voice-controlled call rejection.
     */
    fun handleVoiceCallReject(): ExecutionContract {
        smartCallerAnnouncer.stopAnnouncement()
        return callController.rejectCall()
    }

    companion object {
        @Volatile
        var instance: CallCoordinator? = null
    }
}
