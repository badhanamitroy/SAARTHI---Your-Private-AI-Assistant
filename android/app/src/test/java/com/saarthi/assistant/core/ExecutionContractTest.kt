package com.saarthi.assistant.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionContractTest {

    @Test
    fun acceptedContract_storesDescriptionAndEstimatedAction() {
        val accepted = ExecutionContract.Accepted(
            commandDescription = "Open calculator",
            estimatedAction = "AppLaunch:CALCULATOR"
        )
        assertEquals("Open calculator", accepted.commandDescription)
        assertEquals("AppLaunch:CALCULATOR", accepted.estimatedAction)
    }

    @Test
    fun succeededContract_providesHonestDispatchedWording() {
        val succeeded = ExecutionContract.Succeeded(
            spokenFeedback = "I sent the request to open Calculator.",
            displayFeedback = "Request sent: Calculator",
            targetApp = TargetApp.CALCULATOR,
            packageName = "com.sec.android.app.popupcalculator"
        )
        // Never falsely claims certainty about screen visibility
        assertTrue(succeeded.spokenFeedback.contains("sent the request to open"))
        assertEquals(TargetApp.CALCULATOR, succeeded.targetApp)
        assertEquals("com.sec.android.app.popupcalculator", succeeded.packageName)
    }

    @Test
    fun failedContract_handlesAppNotFound() {
        val failed = ExecutionContract.Failed(
            spokenFeedback = "No compatible calculator app was found on your phone.",
            displayFeedback = "Calculator not found",
            errorType = ExecutionFailureType.APP_NOT_FOUND
        )
        assertEquals(ExecutionFailureType.APP_NOT_FOUND, failed.errorType)
        assertTrue(failed.spokenFeedback.contains("No compatible calculator app"))
    }

    @Test
    fun failedContract_handlesActivityNotFoundException() {
        val ex = android.content.ActivityNotFoundException("Activity not found")
        val failed = ExecutionContract.Failed(
            spokenFeedback = "No application activity was found to open Camera.",
            displayFeedback = "Activity not found for Camera",
            errorType = ExecutionFailureType.ACTIVITY_NOT_FOUND,
            exception = ex
        )
        assertEquals(ExecutionFailureType.ACTIVITY_NOT_FOUND, failed.errorType)
        assertNotNull(failed.exception)
    }

    @Test
    fun failedContract_handlesSecurityException() {
        val ex = SecurityException("Permission denied")
        val failed = ExecutionContract.Failed(
            spokenFeedback = "Permission restriction prevented opening Settings.",
            displayFeedback = "Security restriction: Settings",
            errorType = ExecutionFailureType.SECURITY_RESTRICTION,
            exception = ex
        )
        assertEquals(ExecutionFailureType.SECURITY_RESTRICTION, failed.errorType)
    }

    @Test
    fun needsConfirmation_storesSensitiveAction() {
        val sensitiveAction = SensitiveAction.PlacePhoneCall("mom")
        val prompt = "You asked to call mom. Are you sure you want to place this call? Say yes to confirm or cancel to abort."
        val contract = ExecutionContract.NeedsConfirmation(
            action = sensitiveAction,
            confirmationPrompt = prompt
        )
        assertEquals(sensitiveAction, contract.action)
        assertTrue(contract.confirmationPrompt.contains("call mom"))
    }

    @Test
    fun individualAppLaunch_targetAppCoverage() {
        val apps = listOf(
            TargetApp.CALCULATOR,
            TargetApp.CLOCK,
            TargetApp.CALENDAR,
            TargetApp.CAMERA,
            TargetApp.SETTINGS
        )
        assertEquals(5, apps.size)
        for (app in apps) {
            val fallback = AppLauncher.getFallbackMessage(app)
            assertTrue("Fallback for ${app.name} must not be blank", fallback.isNotBlank())
        }
    }
}
