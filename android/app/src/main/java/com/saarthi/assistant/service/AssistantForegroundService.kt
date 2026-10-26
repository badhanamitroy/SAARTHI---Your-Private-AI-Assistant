package com.saarthi.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.saarthi.assistant.MainActivity
import com.saarthi.assistant.R
import com.saarthi.assistant.core.AppLauncher
import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.DeviceHardwareController
import com.saarthi.assistant.core.ExecutionContract
import com.saarthi.assistant.core.SensitiveAction
import com.saarthi.assistant.core.SystemSettingsController
import com.saarthi.assistant.core.TargetApp
import com.saarthi.assistant.core.TimerController
import com.saarthi.assistant.voice.AndroidSpeechEngine
import com.saarthi.assistant.voice.TextToSpeechEngine
import com.saarthi.assistant.wakeword.LocalWakeWordDetector
import com.saarthi.assistant.wakeword.UtteranceRoutingResult
import com.saarthi.assistant.wakeword.WakeWordCoordinator
import com.saarthi.assistant.wakeword.WakeWordState

/**
 * Always-ready Android Foreground Service running SAARTHI's natural voice loop.
 *
 * Natural Voice UX flow:
 * 1. STANDBY: Actively listens for "Hey SAARTHI". Ordinary conversation is discarded without action.
 * 2. POST-WAKE: After "Hey SAARTHI", opens a post-wake listening window (6-second timeout)
 *    to accept the user's command without requiring another button tap.
 * 3. EXECUTION: Dispatches action, provides brief honest spoken feedback, and returns to standby.
 * 4. SENSITIVE GUARD: Actions like phone calls require explicit user confirmation.
 * 5. MUTUAL EXCLUSION: Microphone is strictly disabled during TTS output to prevent feedback loops.
 */
class AssistantForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "saarthi_assistant_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.saarthi.assistant.action.START"
        const val ACTION_STOP = "com.saarthi.assistant.action.STOP"
        const val ACTION_PAUSE = "com.saarthi.assistant.action.PAUSE"
        const val ACTION_RESUME = "com.saarthi.assistant.action.RESUME"
        const val ACTION_TOGGLE_VOICE = "com.saarthi.assistant.action.TOGGLE_VOICE"

        const val POST_WAKE_TIMEOUT_MS = 6000L

        // Observable service state for Compose UI
        val isServiceRunning = mutableStateOf(false)
        val isListeningActive = mutableStateOf(false)
        val isPaused = mutableStateOf(false)
        val isVoiceFeedbackEnabled = mutableStateOf(true)
        val serviceStatusText = mutableStateOf("Ready")
        val serviceLastRecognized = mutableStateOf("")
        val serviceLastResponse = mutableStateOf("")
        val assistantSessionState = mutableStateOf(WakeWordState.IDLE)

        fun startService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun pauseListening(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeListening(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun toggleVoiceFeedback(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_TOGGLE_VOICE
            }
            context.startService(intent)
        }
    }

    private lateinit var speechEngine: AndroidSpeechEngine
    private lateinit var textToSpeechEngine: TextToSpeechEngine
    private lateinit var commandEngine: CommandEngine
    private lateinit var wakeWordCoordinator: WakeWordCoordinator
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isLoopActive = false
    private var isCurrentlyRecognizing = false
    private var isCurrentlySpeaking = false

    private val postWakeTimeoutRunnable = Runnable {
        if (wakeWordCoordinator.state == WakeWordState.LISTENING_FOR_COMMAND ||
            wakeWordCoordinator.state == WakeWordState.AWAITING_CONFIRMATION) {
            wakeWordCoordinator.onPostWakeTimeout()
            serviceStatusText.value = "Listening for 'Hey SAARTHI'..."
            updateNotification("Listening for 'Hey SAARTHI'...")
            scheduleNextRecognition(delayMs = 400)
        }
    }

    override fun onCreate() {
        super.onCreate()
        speechEngine = AndroidSpeechEngine(applicationContext)
        textToSpeechEngine = TextToSpeechEngine(applicationContext)
        commandEngine = CommandEngine()
        wakeWordCoordinator = WakeWordCoordinator(
            detector = LocalWakeWordDetector(),
            postWakeTimeoutMs = POST_WAKE_TIMEOUT_MS
        )

        wakeWordCoordinator.onStateChanged = { _, newState ->
            assistantSessionState.value = newState
        }

        textToSpeechEngine.onSpeechStart = {
            isCurrentlySpeaking = true
            serviceStatusText.value = "Speaking..."
            updateNotification("Speaking response...")
        }

        textToSpeechEngine.onSpeechDone = {
            isCurrentlySpeaking = false
            wakeWordCoordinator.onTtsFinished()
            handlePostSpeechHandoff()
        }

        textToSpeechEngine.onSpeechError = {
            isCurrentlySpeaking = false
            wakeWordCoordinator.onTtsFinished()
            handlePostSpeechHandoff()
        }

        createNotificationChannel()
    }

    private fun handlePostSpeechHandoff() {
        if (isLoopActive && !isPaused.value) {
            when (wakeWordCoordinator.state) {
                WakeWordState.LISTENING_FOR_COMMAND, WakeWordState.AWAITING_CONFIRMATION -> {
                    serviceStatusText.value = "Listening for command..."
                    updateNotification("Listening for command...")
                    scheduleNextRecognition(delayMs = 300)
                }
                else -> {
                    serviceStatusText.value = "Listening for 'Hey SAARTHI'..."
                    updateNotification("Listening for 'Hey SAARTHI'...")
                    scheduleNextRecognition(delayMs = 400)
                }
            }
        } else if (isPaused.value) {
            serviceStatusText.value = "Paused"
            updateNotification("Paused")
        } else {
            serviceStatusText.value = "Ready"
            updateNotification("Ready")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
                isServiceRunning.value = true
                isPaused.value = false
                isLoopActive = true
                wakeWordCoordinator.start()
                serviceStatusText.value = "Listening for 'Hey SAARTHI'..."
                startRecognitionSession()
            }

            ACTION_PAUSE -> {
                isPaused.value = true
                isLoopActive = false
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                wakeWordCoordinator.pause()
                stopRecognitionSession()
                textToSpeechEngine.stop()
                serviceStatusText.value = "Paused"
                updateNotification("Paused")
            }

            ACTION_RESUME -> {
                isPaused.value = false
                isLoopActive = true
                wakeWordCoordinator.resume()
                serviceStatusText.value = "Listening for 'Hey SAARTHI'..."
                updateNotification("Listening for 'Hey SAARTHI'...")
                startRecognitionSession()
            }

            ACTION_TOGGLE_VOICE -> {
                isVoiceFeedbackEnabled.value = !isVoiceFeedbackEnabled.value
                val status = if (isVoiceFeedbackEnabled.value) "Voice feedback ON" else "Voice feedback MUTED"
                serviceStatusText.value = status
                updateNotification(status)
            }

            ACTION_STOP -> {
                stopSelfService()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Listening for 'Hey SAARTHI'...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            serviceStatusText.value = "Microphone permission required"
            updateNotification("Microphone permission required")
            stopSelfService()
        } catch (e: Exception) {
            serviceStatusText.value = "Service error: ${e.localizedMessage ?: "FGS start restricted"}"
            stopSelfService()
        }
    }

    private fun startRecognitionSession() {
        if (!isLoopActive || isPaused.value || isCurrentlySpeaking || isCurrentlyRecognizing) {
            return
        }

        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            serviceStatusText.value = "Speech recognition unavailable"
            updateNotification("Speech service unavailable on device")
            isLoopActive = false
            return
        }

        isCurrentlyRecognizing = true
        isListeningActive.value = true

        speechEngine.startListening(
            onResult = { text ->
                isCurrentlyRecognizing = false
                isListeningActive.value = false
                serviceLastRecognized.value = text
                handleSpokenInput(text)
            },
            onError = { errorMsg ->
                isCurrentlyRecognizing = false
                isListeningActive.value = false
                handleRecognitionError(errorMsg)
            }
        )
    }

    private fun stopRecognitionSession() {
        isCurrentlyRecognizing = false
        isListeningActive.value = false
        speechEngine.stopListening()
    }

    private fun scheduleNextRecognition(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isLoopActive && !isPaused.value && !isCurrentlySpeaking) {
                startRecognitionSession()
            }
        }, delayMs)
    }

    private fun handleSpokenInput(text: String) {
        when (val route = wakeWordCoordinator.processUtterance(text)) {
            is UtteranceRoutingResult.Ignored -> {
                // Background speech without wake phrase during standby: discard quietly
                serviceStatusText.value = "Listening for 'Hey SAARTHI'..."
                scheduleNextRecognition(delayMs = 300)
            }

            is UtteranceRoutingResult.WakeWordOnly -> {
                // Standalone "Hey SAARTHI" -> open post-wake listening window without requiring a tap!
                serviceStatusText.value = "Listening for command..."
                updateNotification("Listening for command...")

                // Start post-wake window timeout
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                mainHandler.postDelayed(postWakeTimeoutRunnable, POST_WAKE_TIMEOUT_MS)

                speakIfEnabled("I'm listening.")
                // If voice is disabled, start next recognition immediately
                if (!isVoiceFeedbackEnabled.value) {
                    scheduleNextRecognition(delayMs = 200)
                }
            }

            is UtteranceRoutingResult.CommandReady -> {
                // "Hey SAARTHI, [command]" in one utterance
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                serviceStatusText.value = "Processing: \"${route.command}\""
                updateNotification("Processing command...")
                executeCommand(route.command)
            }

            is UtteranceRoutingResult.PostWakeCommand -> {
                // User spoke command during active post-wake window without needing another tap
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                serviceStatusText.value = "Processing: \"${route.command}\""
                updateNotification("Processing command...")
                executeCommand(route.command)
            }

            is UtteranceRoutingResult.ConfirmationResponse -> {
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                handleConfirmationResponse(route.confirmed, route.action)
            }

            is UtteranceRoutingResult.DuplicateDropped -> {
                // Dropped duplicate within debounce cooldown
                scheduleNextRecognition(delayMs = 300)
            }
        }
    }

    private fun handleRecognitionError(errorMsg: String) {
        if (errorMsg.contains("permission", ignoreCase = true)) {
            serviceStatusText.value = "Microphone permission required"
            updateNotification("Microphone permission required")
            isPaused.value = true
            isLoopActive = false
        } else if (isLoopActive && !isPaused.value && !isCurrentlySpeaking) {
            // If in post-wake window and timed out without speech, the runnable handles timeout return
            val backoff = if (errorMsg.contains("busy", ignoreCase = true)) 1200L else 500L
            scheduleNextRecognition(delayMs = backoff)
        }
    }

    private fun executeCommand(text: String) {
        when (val result = commandEngine.process(text)) {
            is CommandResult.Reply -> {
                serviceLastResponse.value = result.text
                serviceStatusText.value = "Ready"
                speakIfEnabled(result.text)
            }

            CommandResult.OpenCalculator -> {
                executeAppLaunch(TargetApp.CALCULATOR)
            }

            is CommandResult.OpenApp -> {
                executeAppLaunch(result.targetApp)
            }

            is CommandResult.GetBatteryStatus -> {
                val contract = DeviceHardwareController.getBatteryStatus(this)
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Battery information is unavailable."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            is CommandResult.SetFlashlight -> {
                val contract = DeviceHardwareController.setFlashlight(this, result.enabled)
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not change flashlight state."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            is CommandResult.OpenSystemSettings -> {
                val contract = SystemSettingsController.openSetting(this, result.settingType)
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not open settings."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            is CommandResult.SetTimer -> {
                val contract = TimerController.startTimer(this, result.durationSeconds)
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not set timer."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            CommandResult.PauseTimer -> {
                val contract = TimerController.pauseTimer()
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not pause timer."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            CommandResult.ResumeTimer -> {
                val contract = TimerController.resumeTimer(this)
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not resume timer."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            CommandResult.CancelTimer -> {
                val contract = TimerController.cancelTimer()
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Could not cancel timer."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            CommandResult.GetTimerStatus -> {
                val contract = TimerController.getStatus()
                val feedback = when (contract) {
                    is ExecutionContract.Succeeded -> contract.spokenFeedback
                    is ExecutionContract.Failed -> contract.spokenFeedback
                    else -> "Timer status is unavailable."
                }
                serviceLastResponse.value = feedback
                serviceStatusText.value = "Ready"
                speakIfEnabled(feedback)
            }

            is CommandResult.Clarification -> {
                serviceLastResponse.value = result.question
                serviceStatusText.value = "Clarification needed"
                speakIfEnabled(result.question)
                // Stay in post-wake window for clarification
                wakeWordCoordinator.enterPostWakeCommandWindow()
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                mainHandler.postDelayed(postWakeTimeoutRunnable, POST_WAKE_TIMEOUT_MS)
            }

            is CommandResult.SensitiveConfirmation -> {
                serviceLastResponse.value = result.confirmationPrompt
                serviceStatusText.value = "Confirmation needed"
                wakeWordCoordinator.requestConfirmation(result.action, result.confirmationPrompt)
                speakIfEnabled(result.confirmationPrompt)
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                mainHandler.postDelayed(postWakeTimeoutRunnable, POST_WAKE_TIMEOUT_MS)
            }

            is CommandResult.WakeWordAcknowledged -> {
                serviceLastResponse.value = result.message
                serviceStatusText.value = "Listening for command..."
                wakeWordCoordinator.enterPostWakeCommandWindow()
                speakIfEnabled(result.message)
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                mainHandler.postDelayed(postWakeTimeoutRunnable, POST_WAKE_TIMEOUT_MS)
            }

            CommandResult.StopSpeaking -> {
                textToSpeechEngine.stop()
                serviceLastResponse.value = "Speaking stopped."
                serviceStatusText.value = "Stopped speaking"
                scheduleNextRecognition(delayMs = 400)
            }

            CommandResult.StopListening -> {
                serviceLastResponse.value = "Listening paused."
                isPaused.value = true
                isLoopActive = false
                mainHandler.removeCallbacks(postWakeTimeoutRunnable)
                stopRecognitionSession()
                updateNotification("Paused")
            }

            CommandResult.NeedsOnlineAI -> {
                val msg = "I cannot do that offline. Offline features include opening apps, time, date, and basic math."
                serviceLastResponse.value = msg
                speakIfEnabled(msg)
            }

            CommandResult.Ignore -> {
                scheduleNextRecognition(delayMs = 300)
            }
        }
    }

    private fun handleConfirmationResponse(confirmed: Boolean, action: SensitiveAction?) {
        if (confirmed && action != null) {
            when (action) {
                is SensitiveAction.PlacePhoneCall -> {
                    val reply = "Placing call request for ${action.recipient}."
                    serviceLastResponse.value = reply
                    serviceStatusText.value = "Calling ${action.recipient}"
                    speakIfEnabled(reply)
                }
                is SensitiveAction.ClearData -> {
                    val reply = "Data cleared."
                    serviceLastResponse.value = reply
                    serviceStatusText.value = "Data cleared"
                    speakIfEnabled(reply)
                }
                is SensitiveAction.SendMessage -> {
                    val reply = "Sending message to ${action.recipient}."
                    serviceLastResponse.value = reply
                    serviceStatusText.value = "Message sent"
                    speakIfEnabled(reply)
                }
            }
        } else {
            val reply = "Action cancelled."
            serviceLastResponse.value = reply
            serviceStatusText.value = "Cancelled"
            speakIfEnabled(reply)
        }
    }

    private fun executeAppLaunch(targetApp: TargetApp) {
        when (val contract = AppLauncher.execute(applicationContext, targetApp)) {
            is ExecutionContract.Succeeded -> {
                serviceLastResponse.value = contract.spokenFeedback
                serviceStatusText.value = contract.displayFeedback
                updateNotification(contract.displayFeedback)
                speakIfEnabled(contract.spokenFeedback)
            }
            is ExecutionContract.Failed -> {
                serviceLastResponse.value = contract.spokenFeedback
                serviceStatusText.value = contract.displayFeedback
                updateNotification(contract.displayFeedback)
                speakIfEnabled(contract.spokenFeedback)
            }
            else -> {}
        }
    }

    private fun speakIfEnabled(text: String) {
        if (isVoiceFeedbackEnabled.value) {
            wakeWordCoordinator.onTtsStarted()
            textToSpeechEngine.speak(text)
        } else {
            // Voice feedback muted: schedule next recognition without speaking
            handlePostSpeechHandoff()
        }
    }

    private fun buildNotification(status: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val toggleActionIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = if (isPaused.value) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val toggleActionTitle = if (isPaused.value) "▶ Resume" else "⏸ Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SAARTHI Assistant")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, toggleActionTitle, togglePendingIntent)
            .addAction(0, "⏹ Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SAARTHI Background Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while SAARTHI listens for local commands."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopSelfService() {
        isLoopActive = false
        isServiceRunning.value = false
        isListeningActive.value = false
        isPaused.value = false
        serviceStatusText.value = "Stopped"

        mainHandler.removeCallbacksAndMessages(null)
        wakeWordCoordinator.stop()
        stopRecognitionSession()
        textToSpeechEngine.stop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        isLoopActive = false
        isServiceRunning.value = false
        isListeningActive.value = false
        isPaused.value = false
        serviceStatusText.value = "Stopped"

        mainHandler.removeCallbacksAndMessages(null)
        wakeWordCoordinator.release()
        speechEngine.release()
        textToSpeechEngine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
