package com.saarthi.assistant.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.saarthi.assistant.core.ExecutionContract
import com.saarthi.assistant.core.ExecutionFailureType

/**
 * Interface for programmatic call answer and rejection using official Android APIs.
 */
interface CallController {
    fun answerCall(): ExecutionContract
    fun rejectCall(): ExecutionContract
}

/**
 * Production implementation using Android TelecomManager.
 * Strictly adheres to official SDK APIs (acceptRingingCall on API 26+, endCall on API 28+).
 * Never claims success if permission is missing or execution fails.
 */
class AndroidCallController(private val context: Context) : CallController {

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    override fun answerCall(): ExecutionContract {
        val tm = telecomManager ?: return ExecutionContract.Failed(
            spokenFeedback = "Telephony service is unavailable on this device.",
            displayFeedback = "Telephony unavailable",
            errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ExecutionContract.Failed(
                spokenFeedback = "Voice call answering requires Android 8.0 or newer.",
                displayFeedback = "Requires Android 8.0+",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ExecutionContract.Failed(
                spokenFeedback = "Call answering requires the Answer Phone Calls permission.",
                displayFeedback = "Permission required",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }

        return try {
            @Suppress("DEPRECATION")
            tm.acceptRingingCall()
            ExecutionContract.Succeeded(
                spokenFeedback = "Answering the call.",
                displayFeedback = "Call answered"
            )
        } catch (e: SecurityException) {
            ExecutionContract.Failed(
                spokenFeedback = "Permission denied while answering call.",
                displayFeedback = "Permission denied",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                exception = e
            )
        } catch (e: Exception) {
            ExecutionContract.Failed(
                spokenFeedback = "Unable to answer call: ${e.message ?: "Unknown error"}",
                displayFeedback = "Answer failed",
                errorType = ExecutionFailureType.UNKNOWN,
                exception = e
            )
        }
    }

    override fun rejectCall(): ExecutionContract {
        val tm = telecomManager ?: return ExecutionContract.Failed(
            spokenFeedback = "Telephony service is unavailable on this device.",
            displayFeedback = "Telephony unavailable",
            errorType = ExecutionFailureType.HARDWARE_UNAVAILABLE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return ExecutionContract.Failed(
                spokenFeedback = "Rejecting calls via voice requires Android 9 or newer on this device.",
                displayFeedback = "Requires Android 9+",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ExecutionContract.Failed(
                spokenFeedback = "Call rejection requires the Answer Phone Calls permission.",
                displayFeedback = "Permission required",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }

        return try {
            @Suppress("DEPRECATION")
            val ended = tm.endCall()
            if (ended) {
                ExecutionContract.Succeeded(
                    spokenFeedback = "Call declined.",
                    displayFeedback = "Call declined"
                )
            } else {
                ExecutionContract.Failed(
                    spokenFeedback = "Unable to decline call.",
                    displayFeedback = "Action failed",
                    errorType = ExecutionFailureType.UNKNOWN
                )
            }
        } catch (e: SecurityException) {
            ExecutionContract.Failed(
                spokenFeedback = "Permission denied while declining call.",
                displayFeedback = "Permission denied",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION,
                exception = e
            )
        } catch (e: Exception) {
            ExecutionContract.Failed(
                spokenFeedback = "Unable to decline call: ${e.message ?: "Unknown error"}",
                displayFeedback = "Decline failed",
                errorType = ExecutionFailureType.UNKNOWN,
                exception = e
            )
        }
    }
}

/**
 * Test fake for unit tests.
 */
class FakeCallController(
    var shouldSucceedAnswer: Boolean = true,
    var shouldSucceedReject: Boolean = true,
    var hasAnswerPermission: Boolean = true,
    var hasRejectPermission: Boolean = true
) : CallController {

    var answerCallInvoked: Boolean = false
        private set
    var rejectCallInvoked: Boolean = false
        private set

    override fun answerCall(): ExecutionContract {
        answerCallInvoked = true
        if (!hasAnswerPermission) {
            return ExecutionContract.Failed(
                spokenFeedback = "Call answering requires the Answer Phone Calls permission.",
                displayFeedback = "Permission required",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }
        return if (shouldSucceedAnswer) {
            ExecutionContract.Succeeded("Answering the call.", "Call answered")
        } else {
            ExecutionContract.Failed("Unable to answer call.", "Answer failed", ExecutionFailureType.UNKNOWN)
        }
    }

    override fun rejectCall(): ExecutionContract {
        rejectCallInvoked = true
        if (!hasRejectPermission) {
            return ExecutionContract.Failed(
                spokenFeedback = "Call rejection requires the Answer Phone Calls permission.",
                displayFeedback = "Permission required",
                errorType = ExecutionFailureType.SECURITY_RESTRICTION
            )
        }
        return if (shouldSucceedReject) {
            ExecutionContract.Succeeded("Call declined.", "Call declined")
        } else {
            ExecutionContract.Failed("Unable to decline call.", "Action failed", ExecutionFailureType.UNKNOWN)
        }
    }
}
