package com.saarthi.assistant.telephony

import com.saarthi.assistant.core.ExecutionContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallControllerTest {

    @Test
    fun answerCall_whenPermissionGranted_returnsSucceeded() {
        val controller = FakeCallController(
            shouldSucceedAnswer = true,
            hasAnswerPermission = true
        )

        val result = controller.answerCall()
        assertTrue(result is ExecutionContract.Succeeded)
        assertTrue(controller.answerCallInvoked)
        assertEquals("Answering the call.", (result as ExecutionContract.Succeeded).spokenFeedback)
    }

    @Test
    fun answerCall_whenPermissionDenied_returnsFailedWithHonestNotice() {
        val controller = FakeCallController(
            hasAnswerPermission = false
        )

        val result = controller.answerCall()
        assertTrue(result is ExecutionContract.Failed)
        assertTrue(controller.answerCallInvoked)
        val failed = result as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("permission", ignoreCase = true))
        assertEquals("Permission required", failed.displayFeedback)
    }

    @Test
    fun rejectCall_whenPermissionGranted_returnsSucceeded() {
        val controller = FakeCallController(
            shouldSucceedReject = true,
            hasRejectPermission = true
        )

        val result = controller.rejectCall()
        assertTrue(result is ExecutionContract.Succeeded)
        assertTrue(controller.rejectCallInvoked)
        assertEquals("Call declined.", (result as ExecutionContract.Succeeded).spokenFeedback)
    }

    @Test
    fun rejectCall_whenPermissionDenied_returnsFailedWithHonestNotice() {
        val controller = FakeCallController(
            hasRejectPermission = false
        )

        val result = controller.rejectCall()
        assertTrue(result is ExecutionContract.Failed)
        assertTrue(controller.rejectCallInvoked)
        val failed = result as ExecutionContract.Failed
        assertTrue(failed.spokenFeedback.contains("permission", ignoreCase = true))
        assertEquals("Permission required", failed.displayFeedback)
    }

    @Test
    fun rejectCall_whenPlatformOperationFails_returnsFailed() {
        val controller = FakeCallController(
            shouldSucceedReject = false,
            hasRejectPermission = true
        )

        val result = controller.rejectCall()
        assertTrue(result is ExecutionContract.Failed)
        assertEquals("Action failed", (result as ExecutionContract.Failed).displayFeedback)
    }
}

