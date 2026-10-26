package com.saarthi.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saarthi.assistant.core.AppLauncher
import com.saarthi.assistant.core.BatteryStatusResult
import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.DeviceHardwareController
import com.saarthi.assistant.core.ExecutionContract
import com.saarthi.assistant.core.HistoryManager
import com.saarthi.assistant.core.SystemSettingType
import com.saarthi.assistant.core.SystemSettingsController
import com.saarthi.assistant.core.TargetApp
import com.saarthi.assistant.core.TimerController
import com.saarthi.assistant.core.rememberNetworkStatus
import com.saarthi.assistant.service.AssistantForegroundService
import com.saarthi.assistant.ui.components.MicState
import com.saarthi.assistant.ui.components.SaarthiBottomBar
import com.saarthi.assistant.ui.components.SaarthiTab
import com.saarthi.assistant.ui.screens.ConversationScreen
import com.saarthi.assistant.ui.screens.HomeScreen
import com.saarthi.assistant.ui.screens.SettingsScreen
import com.saarthi.assistant.ui.screens.ToolsScreen
import com.saarthi.assistant.ui.theme.SAARTHITheme
import com.saarthi.assistant.voice.AndroidSpeechEngine
import com.saarthi.assistant.voice.TextToSpeechEngine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            SAARTHITheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SaarthiApp(
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { isDarkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun SaarthiApp(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // Initialize local history
    LaunchedEffect(Unit) {
        HistoryManager.init(context)
    }

    // Navigation Tab state
    var currentTab by rememberSaveable { mutableStateOf(SaarthiTab.HOME) }

    // Intercept Back button: return to Home tab first before exiting
    BackHandler(enabled = currentTab != SaarthiTab.HOME) {
        currentTab = SaarthiTab.HOME
    }

    // Real device connectivity state
    val networkStatus by rememberNetworkStatus()

    // Permissions state
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Conversational state surviving screen rotations
    var typedText by rememberSaveable { mutableStateOf("") }
    var lastUserQuery by rememberSaveable { mutableStateOf("") }
    var lastAssistantResponse by rememberSaveable { mutableStateOf("") }
    var statusText by rememberSaveable { mutableStateOf("Ready") }
    var micState by remember { mutableStateOf(MicState.IDLE) }

    // Speech & Command engines
    val speechEngine = remember { AndroidSpeechEngine(context.applicationContext) }
    val textToSpeechEngine = remember { TextToSpeechEngine(context.applicationContext) }
    val commandEngine = remember { CommandEngine() }

    // Background Assistant service state
    val isBgRunning by AssistantForegroundService.isServiceRunning
    val isBgPaused by AssistantForegroundService.isPaused
    val isVoiceFeedbackEnabled by AssistantForegroundService.isVoiceFeedbackEnabled
    val bgStatusText by AssistantForegroundService.serviceStatusText
    val bgLastRecognized by AssistantForegroundService.serviceLastRecognized
    val bgLastResponse by AssistantForegroundService.serviceLastResponse

    // Device Hardware State (Battery & Flashlight)
    var batteryStatus by remember { mutableStateOf<BatteryStatusResult?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isFlashAvailable by remember { mutableStateOf(true) }

    // Timer State
    val timerSnapshot by TimerController.snapshot

    fun refreshHardwareStatus() {
        batteryStatus = DeviceHardwareController.getRawBatteryStatus(context)
        isFlashlightOn = DeviceHardwareController.isFlashlightOn(context)
        isFlashAvailable = DeviceHardwareController.isFlashAvailable(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshHardwareStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        refreshHardwareStatus()
        TimerController.onTimerFinishedListener = { snapshot ->
            val msg = "Timer for ${TimerController.formatDurationSpoken(snapshot.totalSeconds)} has finished!"
            lastAssistantResponse = msg
            statusText = "Timer finished"
            if (isVoiceFeedbackEnabled) {
                textToSpeechEngine.speak(msg)
            }
        }
    }

    LaunchedEffect(bgLastRecognized) {
        if (bgLastRecognized.isNotBlank()) {
            lastUserQuery = bgLastRecognized
        }
    }

    LaunchedEffect(bgLastResponse) {
        if (bgLastResponse.isNotBlank()) {
            lastAssistantResponse = bgLastResponse
        }
    }

    // TTS Callbacks
    LaunchedEffect(textToSpeechEngine) {
        textToSpeechEngine.onSpeechStart = {
            statusText = "Speaking..."
            micState = MicState.RESPONDING
        }
        textToSpeechEngine.onSpeechDone = {
            if (statusText == "Speaking...") {
                statusText = "Ready"
                micState = MicState.IDLE
            }
        }
        textToSpeechEngine.onSpeechError = { errorMsg ->
            statusText = errorMsg
            micState = MicState.IDLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechEngine.release()
            textToSpeechEngine.release()
        }
    }

    // Permissions Launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioOk = results[Manifest.permission.RECORD_AUDIO] ?: (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS] ?: (
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            true
        }

        hasAudioPermission = audioOk
        hasNotificationPermission = notifOk

        if (!audioOk) {
            micState = MicState.PERMISSION_DENIED
            statusText = "Microphone permission denied"
        } else {
            micState = MicState.IDLE
            statusText = "Ready"
        }
    }

    fun requestPermissions(onGranted: () -> Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            hasAudioPermission = true
            hasNotificationPermission = true
            onGranted()
        } else {
            permissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    // Command Dispatcher
    fun processCommand(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        lastUserQuery = trimmed
        micState = MicState.PROCESSING

        when (val result = commandEngine.process(trimmed)) {
            is CommandResult.Reply -> {
                lastAssistantResponse = result.text
                statusText = "Ready"
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, result.text)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(result.text)
            }

            CommandResult.OpenCalculator -> {
                when (val contract = AppLauncher.execute(context, TargetApp.CALCULATOR)) {
                    is ExecutionContract.Succeeded -> {
                        lastAssistantResponse = contract.spokenFeedback
                        statusText = contract.displayFeedback
                        micState = MicState.IDLE
                        HistoryManager.addInteraction(context, trimmed, contract.spokenFeedback, "Calculator")
                        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(contract.spokenFeedback)
                    }
                    is ExecutionContract.Failed -> {
                        lastAssistantResponse = contract.spokenFeedback
                        statusText = contract.displayFeedback
                        micState = MicState.IDLE
                        HistoryManager.addInteraction(context, trimmed, contract.spokenFeedback)
                        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(contract.spokenFeedback)
                    }
                    else -> {
                        micState = MicState.IDLE
                    }
                }
            }

            is CommandResult.OpenApp -> {
                when (val contract = AppLauncher.execute(context, result.targetApp)) {
                    is ExecutionContract.Succeeded -> {
                        lastAssistantResponse = contract.spokenFeedback
                        statusText = contract.displayFeedback
                        micState = MicState.IDLE
                        HistoryManager.addInteraction(context, trimmed, contract.spokenFeedback, result.targetApp.displayName)
                        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(contract.spokenFeedback)
                    }
                    is ExecutionContract.Failed -> {
                        lastAssistantResponse = contract.spokenFeedback
                        statusText = contract.displayFeedback
                        micState = MicState.IDLE
                        HistoryManager.addInteraction(context, trimmed, contract.spokenFeedback)
                        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(contract.spokenFeedback)
                    }
                    else -> {
                        micState = MicState.IDLE
                    }
                }
            }

            is CommandResult.GetBatteryStatus -> {
                val contract = DeviceHardwareController.getBatteryStatus(context)
                refreshHardwareStatus()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Battery information is unavailable.", "Battery unavailable")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            is CommandResult.SetFlashlight -> {
                val contract = DeviceHardwareController.setFlashlight(context, result.enabled)
                refreshHardwareStatus()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not change flashlight state.", "Flashlight failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            is CommandResult.OpenSystemSettings -> {
                val contract = SystemSettingsController.openSetting(context, result.settingType)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not open settings.", "Settings failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            is CommandResult.SetTimer -> {
                val contract = TimerController.startTimer(context, result.durationSeconds)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not set timer.", "Timer failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            CommandResult.PauseTimer -> {
                val contract = TimerController.pauseTimer()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not pause timer.", "Pause failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            CommandResult.ResumeTimer -> {
                val contract = TimerController.resumeTimer(context)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not resume timer.", "Resume failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            CommandResult.CancelTimer -> {
                val contract = TimerController.cancelTimer()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not cancel timer.", "Cancel failed")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            CommandResult.GetTimerStatus -> {
                val contract = TimerController.getStatus()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Timer status is unavailable.", "Status unavailable")
                }
                lastAssistantResponse = feedback
                statusText = display
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, feedback, display)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
            }

            is CommandResult.Clarification -> {
                lastAssistantResponse = result.question
                statusText = "Clarification needed"
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, result.question)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(result.question)
            }

            is CommandResult.SensitiveConfirmation -> {
                lastAssistantResponse = result.confirmationPrompt
                statusText = "Confirmation needed"
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, result.confirmationPrompt)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(result.confirmationPrompt)
            }

            is CommandResult.WakeWordAcknowledged -> {
                lastAssistantResponse = result.message
                statusText = "Listening for command..."
                micState = MicState.IDLE
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(result.message)
            }

            CommandResult.StopSpeaking -> {
                textToSpeechEngine.stop()
                lastAssistantResponse = "Speaking stopped."
                statusText = "Stopped"
                micState = MicState.IDLE
            }

            CommandResult.StopListening -> {
                speechEngine.stopListening()
                lastAssistantResponse = "Listening stopped."
                statusText = "Stopped listening"
                micState = MicState.IDLE
            }

            CommandResult.NeedsOnlineAI -> {
                val honestMsg = "I cannot do that offline. Offline capabilities include opening apps, checking the time and date, calculating math, and device settings."
                lastAssistantResponse = honestMsg
                statusText = "Online AI required"
                micState = MicState.IDLE
                HistoryManager.addInteraction(context, trimmed, honestMsg)
                if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(honestMsg)
            }

            CommandResult.Ignore -> {
                statusText = "Ready"
                micState = MicState.IDLE
            }
        }
    }

    // Mic action trigger
    fun handleMicAction() {
        if (!hasAudioPermission) {
            requestPermissions {
                handleMicAction()
            }
            return
        }

        if (micState == MicState.LISTENING) {
            speechEngine.stopListening()
            micState = MicState.IDLE
            statusText = "Stopped listening"
        } else if (micState == MicState.RESPONDING) {
            textToSpeechEngine.stop()
            micState = MicState.IDLE
            statusText = "Ready"
        } else {
            micState = MicState.LISTENING
            statusText = "Listening..."
            speechEngine.startListening(
                onResult = { recognized ->
                    processCommand(recognized)
                },
                onError = { error ->
                    statusText = error
                    micState = MicState.IDLE
                    if (isVoiceFeedbackEnabled) {
                        textToSpeechEngine.speak("Sorry, I could not understand that.")
                    }
                }
            )
        }
    }

    // App launch helpers
    fun launchAppDirectly(app: TargetApp) {
        val (feedback, display) = when (val contract = AppLauncher.execute(context, app)) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Attempting to open ${app.displayName}...", app.displayName)
        }
        lastUserQuery = "Open ${app.displayName}"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, "Open ${app.displayName}", feedback, app.displayName)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun checkBatteryDirectly() {
        val contract = DeviceHardwareController.getBatteryStatus(context)
        refreshHardwareStatus()
        val (feedback, display) = when (contract) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Battery information is unavailable.", "Battery unavailable")
        }
        lastUserQuery = "Check Battery"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, "Check Battery", feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun toggleFlashlightDirectly() {
        val contract = DeviceHardwareController.toggleFlashlight(context)
        refreshHardwareStatus()
        val (feedback, display) = when (contract) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not toggle flashlight.", "Torch error")
        }
        lastUserQuery = "Toggle Flashlight"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, "Toggle Flashlight", feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun setFlashlightDirectly(enabled: Boolean) {
        val contract = DeviceHardwareController.setFlashlight(context, enabled)
        refreshHardwareStatus()
        val (feedback, display) = when (contract) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not set flashlight.", "Torch error")
        }
        lastUserQuery = if (enabled) "Turn On Flashlight" else "Turn Off Flashlight"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun openSettingDirectly(settingType: SystemSettingType) {
        val (feedback, display) = when (val contract = SystemSettingsController.openSetting(context, settingType)) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not open ${settingType.displayName} settings.", "Settings failed")
        }
        lastUserQuery = "Open ${settingType.displayName} Settings"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun startTimerDirectly(seconds: Long) {
        val (feedback, display) = when (val contract = TimerController.startTimer(context, seconds)) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not set timer.", "Timer failed")
        }
        lastUserQuery = "Set timer for ${TimerController.formatDurationSpoken(seconds)}"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun pauseTimerDirectly() {
        val (feedback, display) = when (val contract = TimerController.pauseTimer()) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not pause timer.", "Pause failed")
        }
        lastUserQuery = "Pause Timer"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun resumeTimerDirectly() {
        val (feedback, display) = when (val contract = TimerController.resumeTimer(context)) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not resume timer.", "Resume failed")
        }
        lastUserQuery = "Resume Timer"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    fun cancelTimerDirectly() {
        val (feedback, display) = when (val contract = TimerController.cancelTimer()) {
            is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
            is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
            else -> Pair("Could not cancel timer.", "Cancel failed")
        }
        lastUserQuery = "Cancel Timer"
        lastAssistantResponse = feedback
        statusText = display
        HistoryManager.addInteraction(context, lastUserQuery, feedback, display)
        if (isVoiceFeedbackEnabled) textToSpeechEngine.speak(feedback)
    }

    Scaffold(
        bottomBar = {
            SaarthiBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier.navigationBarsPadding()
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                SaarthiTab.HOME -> {
                    HomeScreen(
                        networkStatus = networkStatus,
                        micState = micState,
                        typedText = typedText,
                        onTypedTextChange = { typedText = it },
                        onSendText = {
                            processCommand(it)
                            typedText = ""
                        },
                        onMicClick = { handleMicAction() },
                        onCancelListening = {
                            speechEngine.cancelListening()
                            micState = MicState.IDLE
                            statusText = "Ready"
                        },
                        onStopSpeaking = {
                            textToSpeechEngine.stop()
                            micState = MicState.IDLE
                            statusText = "Ready"
                        },
                        lastUserQuery = lastUserQuery,
                        lastAssistantResponse = lastAssistantResponse,
                        statusText = statusText,
                        historyItems = HistoryManager.items,
                        onHistoryItemClick = { item ->
                            processCommand(item.query)
                        },
                        onClearHistory = {
                            HistoryManager.clear(context)
                        },
                        onCameraClick = { launchAppDirectly(TargetApp.CAMERA) },
                        onCalculatorClick = { launchAppDirectly(TargetApp.CALCULATOR) },
                        onClockClick = { launchAppDirectly(TargetApp.CLOCK) },
                        onCalendarClick = { launchAppDirectly(TargetApp.CALENDAR) },
                        onSettingsClick = { currentTab = SaarthiTab.SETTINGS },
                        onTimeClick = { processCommand("what time is it") },
                        batteryStatus = batteryStatus,
                        isFlashlightOn = isFlashlightOn,
                        isFlashAvailable = isFlashAvailable,
                        onCheckBatteryClick = { checkBatteryDirectly() },
                        onToggleFlashlightClick = { toggleFlashlightDirectly() },
                        timerSnapshot = timerSnapshot,
                        onPauseTimer = { pauseTimerDirectly() },
                        onResumeTimer = { resumeTimerDirectly() },
                        onCancelTimer = { cancelTimerDirectly() }
                    )
                }

                SaarthiTab.CHAT -> {
                    ConversationScreen(
                        historyItems = HistoryManager.items,
                        typedText = typedText,
                        onTypedTextChange = { typedText = it },
                        onSendText = {
                            processCommand(it)
                            typedText = ""
                        },
                        micState = micState,
                        onMicClick = { handleMicAction() },
                        onClearHistory = {
                            HistoryManager.clear(context)
                        }
                    )
                }

                SaarthiTab.TOOLS -> {
                    ToolsScreen(
                        onCameraClick = { launchAppDirectly(TargetApp.CAMERA) },
                        onCalculatorClick = { launchAppDirectly(TargetApp.CALCULATOR) },
                        onClockClick = { launchAppDirectly(TargetApp.CLOCK) },
                        onCalendarClick = { launchAppDirectly(TargetApp.CALENDAR) },
                        onSettingsClick = { launchAppDirectly(TargetApp.SETTINGS) },
                        batteryStatus = batteryStatus,
                        isFlashlightOn = isFlashlightOn,
                        isFlashAvailable = isFlashAvailable,
                        onCheckBatteryClick = { checkBatteryDirectly() },
                        onSetFlashlightClick = { enabled -> setFlashlightDirectly(enabled) },
                        isBgRunning = isBgRunning,
                        isBgPaused = isBgPaused,
                        isVoiceFeedbackEnabled = isVoiceFeedbackEnabled,
                        bgStatusText = bgStatusText,
                        onToggleBgService = {
                            if (!isBgRunning) {
                                requestPermissions {
                                    AssistantForegroundService.startService(context)
                                }
                            } else {
                                AssistantForegroundService.stopService(context)
                            }
                        },
                        onPauseResumeBgService = {
                            if (isBgPaused) {
                                AssistantForegroundService.resumeListening(context)
                            } else {
                                AssistantForegroundService.pauseListening(context)
                            }
                        },
                        onToggleVoiceFeedback = {
                            AssistantForegroundService.toggleVoiceFeedback(context)
                        },
                        timerSnapshot = timerSnapshot,
                        onStartTimer = { startTimerDirectly(it) },
                        onPauseTimer = { pauseTimerDirectly() },
                        onResumeTimer = { resumeTimerDirectly() },
                        onCancelTimer = { cancelTimerDirectly() },
                        onOpenSetting = { openSettingDirectly(it) },
                        onHelpExampleClick = { processCommand(it) }
                    )
                }

                SaarthiTab.SETTINGS -> {
                    SettingsScreen(
                        isVoiceFeedbackEnabled = isVoiceFeedbackEnabled,
                        onToggleVoiceFeedback = {
                            AssistantForegroundService.toggleVoiceFeedback(context)
                        },
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        hasAudioPermission = hasAudioPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermissions = {
                            requestPermissions {}
                        },
                        onClearHistory = {
                            HistoryManager.clear(context)
                        }
                    )
                }
            }
        }
    }
}