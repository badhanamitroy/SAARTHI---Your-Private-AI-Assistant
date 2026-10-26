package com.saarthi.assistant.core

/**
 * Formal action execution contract for SAARTHI.
 * Ensures the assistant never announces success prematurely or claims certainty
 * when the Android OS only provides asynchronous dispatch.
 */
sealed class ExecutionContract {

    /**
     * The command was understood and the action is about to be attempted.
     */
    data class Accepted(
        val commandDescription: String,
        val estimatedAction: String
    ) : ExecutionContract()

    /**
     * The intended operation has been verified to the extent Android allows.
     * Note: In Android, startActivity is an asynchronous request to the OS;
     * success indicates intent resolution and OS dispatch succeeded.
     */
    data class Succeeded(
        val spokenFeedback: String,
        val displayFeedback: String,
        val targetApp: TargetApp? = null,
        val packageName: String? = null
    ) : ExecutionContract()

    /**
     * The operation failed, was unavailable, or could not be verified.
     */
    data class Failed(
        val spokenFeedback: String,
        val displayFeedback: String,
        val errorType: ExecutionFailureType,
        val exception: Throwable? = null
    ) : ExecutionContract()

    /**
     * The command is ambiguous and requires user clarification.
     */
    data class NeedsClarification(
        val question: String
    ) : ExecutionContract()

    /**
     * The action is sensitive or potentially consequential and requires explicit user confirmation.
     */
    data class NeedsConfirmation(
        val action: SensitiveAction,
        val confirmationPrompt: String
    ) : ExecutionContract()
}

/**
 * Specific failure categorizations for honest reporting.
 */
enum class ExecutionFailureType {
    /** No installed application matches the requested target or intent filter. */
    APP_NOT_FOUND,

    /** Intent resolved to a component, but the Activity was not found at launch time. */
    ACTIVITY_NOT_FOUND,

    /** OS SecurityException (e.g. unexported component or missing permission). */
    SECURITY_RESTRICTION,

    /** Android Background Activity Launch (BAL) restriction from service context. */
    BACKGROUND_START_RESTRICTED,

    /** Mathematical or syntax evaluation error. */
    EVALUATION_ERROR,

    /** Command requires cloud/online capabilities not available offline. */
    UNSUPPORTED_OFFLINE,

    /** Physical hardware component (e.g. camera flash, battery sensor) is absent or unavailable. */
    HARDWARE_UNAVAILABLE,

    /** Hardware resource is in active contention or locked by another application. */
    HARDWARE_BUSY,

    /** Sensitive action was aborted or declined by the user. */
    CANCELLED,

    /** General or unexpected error. */
    UNKNOWN
}

/**
 * Representation of sensitive or consequential actions requiring explicit confirmation.
 */
sealed class SensitiveAction {
    data class PlacePhoneCall(val recipient: String) : SensitiveAction()
    data class ClearData(val scope: String) : SensitiveAction()
    data class SendMessage(val recipient: String, val message: String) : SensitiveAction()
}
