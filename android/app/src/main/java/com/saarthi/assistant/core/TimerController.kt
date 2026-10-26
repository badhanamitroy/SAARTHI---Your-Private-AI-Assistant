package com.saarthi.assistant.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TimerSnapshot(
    val state: TimerState = TimerState.IDLE,
    val totalSeconds: Long = 0,
    val remainingSeconds: Long = 0,
    val label: String = ""
) {
    val progress: Float = if (totalSeconds > 0) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val formattedRemaining: String = TimerController.formatDuration(remainingSeconds)
    val formattedTotal: String = TimerController.formatDuration(totalSeconds)
}

/**
 * Injectable time provider interface for testability.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * Local on-device countdown timer controller.
 * Operates safely offline with zero invasive alarm permissions, providing stateful
 * countdown, pause/resume, and notification alerts.
 */
object TimerController {

    private const val NOTIFICATION_CHANNEL_ID = "saarthi_timer_channel"
    private const val NOTIFICATION_ID = 2001
    const val MAX_TIMER_SECONDS: Long = 86400L // 24 Hours

    private val _snapshot = mutableStateOf(TimerSnapshot())
    val snapshot: State<TimerSnapshot> get() = _snapshot

    private var timeProvider: TimeProvider = SystemTimeProvider
    private var tickerJob: Job? = null
    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private var lastStartedSeconds: Long = 0
    private var lastStartTimeMillis: Long = 0

    // Callback hook for assistant feedback when timer finishes
    var onTimerFinishedListener: ((TimerSnapshot) -> Unit)? = null

    fun setTimeProviderForTesting(provider: TimeProvider, scope: CoroutineScope? = null) {
        timeProvider = provider
        if (scope != null) {
            coroutineScope = scope
        }
    }

    fun resetForTesting() {
        tickerJob?.cancel()
        tickerJob = null
        _snapshot.value = TimerSnapshot()
        lastStartedSeconds = 0
        lastStartTimeMillis = 0
        timeProvider = SystemTimeProvider
        coroutineScope = CoroutineScope(Dispatchers.Default)
    }

    fun startTimer(
        context: Context?,
        durationSeconds: Long,
        label: String = ""
    ): ExecutionContract {
        if (durationSeconds <= 0) {
            return ExecutionContract.Failed(
                spokenFeedback = "Timer duration must be greater than zero.",
                displayFeedback = "Invalid duration",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            )
        }

        if (durationSeconds > MAX_TIMER_SECONDS) {
            return ExecutionContract.Failed(
                spokenFeedback = "Timer duration cannot exceed 24 hours.",
                displayFeedback = "Duration exceeds 24h limit",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            )
        }

        // Prevent immediate duplicate timer if identical request received within 2 seconds
        val now = timeProvider.currentTimeMillis()
        if (_snapshot.value.state == TimerState.RUNNING &&
            durationSeconds == lastStartedSeconds &&
            (now - lastStartTimeMillis) < 2000L
        ) {
            return ExecutionContract.Succeeded(
                spokenFeedback = "A timer for ${formatDurationSpoken(durationSeconds)} is already running.",
                displayFeedback = "Timer running: ${formatDuration(durationSeconds)}"
            )
        }

        tickerJob?.cancel()

        lastStartedSeconds = durationSeconds
        lastStartTimeMillis = now

        _snapshot.value = TimerSnapshot(
            state = TimerState.RUNNING,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            label = label
        )

        startTicker(context)

        val spokenDuration = formatDurationSpoken(durationSeconds)
        return ExecutionContract.Succeeded(
            spokenFeedback = "Timer set for $spokenDuration.",
            displayFeedback = "Timer: ${formatDuration(durationSeconds)}"
        )
    }

    fun pauseTimer(): ExecutionContract {
        if (_snapshot.value.state != TimerState.RUNNING) {
            return ExecutionContract.Failed(
                spokenFeedback = "There is no active timer to pause.",
                displayFeedback = "No timer running",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            )
        }

        tickerJob?.cancel()
        tickerJob = null

        _snapshot.value = _snapshot.value.copy(state = TimerState.PAUSED)
        return ExecutionContract.Succeeded(
            spokenFeedback = "Timer paused with ${formatDurationSpoken(_snapshot.value.remainingSeconds)} remaining.",
            displayFeedback = "Timer paused: ${_snapshot.value.formattedRemaining}"
        )
    }

    fun resumeTimer(context: Context? = null): ExecutionContract {
        if (_snapshot.value.state == TimerState.RUNNING) {
            return ExecutionContract.Succeeded(
                spokenFeedback = "The timer is already running.",
                displayFeedback = "Timer running: ${_snapshot.value.formattedRemaining}"
            )
        }
        if (_snapshot.value.state != TimerState.PAUSED) {
            return ExecutionContract.Failed(
                spokenFeedback = "There is no paused timer to resume.",
                displayFeedback = "No paused timer",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            )
        }

        _snapshot.value = _snapshot.value.copy(state = TimerState.RUNNING)
        startTicker(context)

        return ExecutionContract.Succeeded(
            spokenFeedback = "Timer resumed. ${formatDurationSpoken(_snapshot.value.remainingSeconds)} remaining.",
            displayFeedback = "Timer resumed: ${_snapshot.value.formattedRemaining}"
        )
    }

    fun cancelTimer(): ExecutionContract {
        if (_snapshot.value.state == TimerState.IDLE) {
            return ExecutionContract.Failed(
                spokenFeedback = "There is no active timer to cancel.",
                displayFeedback = "No active timer",
                errorType = ExecutionFailureType.EVALUATION_ERROR
            )
        }

        tickerJob?.cancel()
        tickerJob = null

        _snapshot.value = TimerSnapshot(state = TimerState.IDLE)
        return ExecutionContract.Succeeded(
            spokenFeedback = "Timer cancelled.",
            displayFeedback = "Timer cancelled"
        )
    }

    fun getStatus(): ExecutionContract {
        val current = _snapshot.value
        return when (current.state) {
            TimerState.IDLE -> ExecutionContract.Succeeded(
                spokenFeedback = "There is no active timer.",
                displayFeedback = "No active timer"
            )
            TimerState.RUNNING -> ExecutionContract.Succeeded(
                spokenFeedback = "${formatDurationSpoken(current.remainingSeconds)} remaining on your timer.",
                displayFeedback = "Timer: ${current.formattedRemaining}"
            )
            TimerState.PAUSED -> ExecutionContract.Succeeded(
                spokenFeedback = "Timer is paused with ${formatDurationSpoken(current.remainingSeconds)} remaining.",
                displayFeedback = "Paused: ${current.formattedRemaining}"
            )
            TimerState.FINISHED -> ExecutionContract.Succeeded(
                spokenFeedback = "Timer has finished.",
                displayFeedback = "Timer finished"
            )
        }
    }

    /**
     * Manual tick step for unit tests. Advances countdown by [seconds].
     */
    fun manualTick(seconds: Long = 1, context: Context? = null) {
        if (_snapshot.value.state != TimerState.RUNNING) return

        val newRemaining = (_snapshot.value.remainingSeconds - seconds).coerceAtLeast(0)
        if (newRemaining <= 0) {
            _snapshot.value = _snapshot.value.copy(
                remainingSeconds = 0,
                state = TimerState.FINISHED
            )
            onFinished(context)
        } else {
            _snapshot.value = _snapshot.value.copy(remainingSeconds = newRemaining)
        }
    }

    private fun startTicker(context: Context?) {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch {
            while (isActive && _snapshot.value.state == TimerState.RUNNING) {
                delay(1000L)
                manualTick(1, context)
                if (_snapshot.value.state == TimerState.FINISHED) {
                    break
                }
            }
        }
    }

    private fun onFinished(context: Context?) {
        tickerJob?.cancel()
        tickerJob = null

        val finishedSnapshot = _snapshot.value
        onTimerFinishedListener?.invoke(finishedSnapshot)

        if (context != null) {
            showTimerFinishedNotification(context, finishedSnapshot)
        }
    }

    private fun showTimerFinishedNotification(context: Context, snapshot: TimerSnapshot) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Timer Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a local timer finishes"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val totalSpoken = formatDurationSpoken(snapshot.totalSeconds)
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Timer Finished")
                .setContentText("Your $totalSpoken timer has completed!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .apply {
                    if (pendingIntent != null) {
                        setContentIntent(pendingIntent)
                    }
                }
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Defensive: notification permission may be denied or notification service unavailable
        }
    }

    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "00:00"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationSpoken(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "0 seconds"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (hours > 0) {
            parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
        }
        if (minutes > 0) {
            parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add("$seconds ${if (seconds == 1L) "second" else "seconds"}")
        }

        return parts.joinToString(", ")
    }
}
