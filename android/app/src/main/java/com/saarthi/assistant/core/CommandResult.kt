package com.saarthi.assistant.core

sealed class CommandResult {

    data class Reply(
        val text: String
    ) : CommandResult()

    data class Clarification(
        val question: String
    ) : CommandResult()

    data object StopSpeaking : CommandResult()

    data object StopListening : CommandResult()

    data object OpenCalculator : CommandResult()

    data class OpenApp(
        val targetApp: TargetApp
    ) : CommandResult()

    data class SensitiveConfirmation(
        val action: SensitiveAction,
        val confirmationPrompt: String
    ) : CommandResult()

    data class WakeWordAcknowledged(
        val message: String = "I'm listening."
    ) : CommandResult()

    data object GetBatteryStatus : CommandResult()

    data class SetFlashlight(
        val enabled: Boolean
    ) : CommandResult()

    data class OpenSystemSettings(
        val settingType: SystemSettingType
    ) : CommandResult()

    data class SetTimer(
        val durationSeconds: Long,
        val originalQuery: String
    ) : CommandResult()

    data object PauseTimer : CommandResult()

    data object ResumeTimer : CommandResult()

    data object CancelTimer : CommandResult()

    data object GetTimerStatus : CommandResult()

    data object Ignore : CommandResult()

    data object NeedsOnlineAI : CommandResult()
}