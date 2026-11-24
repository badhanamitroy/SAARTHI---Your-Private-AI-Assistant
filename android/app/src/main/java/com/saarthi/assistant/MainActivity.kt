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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saarthi.assistant.ai.AndroidNetworkChecker
import com.saarthi.assistant.ai.AndroidPrivacyController
import com.saarthi.assistant.ai.ConversationMemory
import com.saarthi.assistant.ai.HybridRequestRouter
import com.saarthi.assistant.ai.HybridRoutingResult
import com.saarthi.assistant.ai.SecureHttpAiProvider
import android.content.Context
import com.saarthi.assistant.core.AppLauncher
import com.saarthi.assistant.core.BatteryStatusResult
import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.ConversationState
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
import com.saarthi.assistant.telephony.AndroidAudioRouteDetector
import com.saarthi.assistant.telephony.AndroidCallController
import com.saarthi.assistant.telephony.AndroidCallPreferencesManager
import com.saarthi.assistant.telephony.AndroidContactResolver
import com.saarthi.assistant.telephony.CallCoordinator
import com.saarthi.assistant.telephony.CallPreferences
import com.saarthi.assistant.telephony.SmartCallerAnnouncer
import com.saarthi.assistant.voice.VoiceSessionCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    val requestScope = rememberCoroutineScope()
    val privacyController = remember { AndroidPrivacyController(context.applicationContext) }
    var privacySettings by remember { mutableStateOf(privacyController.getSettings()) }
    val networkChecker = remember { AndroidNetworkChecker(context.applicationContext) }
    val conversationMemory = remember { ConversationMemory(context.applicationContext) }

    fun clearConversationHistory() {
        HistoryManager.clear(context)
        conversationMemory.clear()
    }

    fun routeOnlineQuery(
        queryText: String,
        onComplete: (spoken: String, display: String) -> Unit
    ) {
        requestScope.launch {
            val currentPrivacy = privacyController.getSettings()
            val router = HybridRequestRouter(
                commandEngine = commandEngine,
                privacyManager = privacyController,
                networkChecker = networkChecker,
                aiProvider = SecureHttpAiProvider(endpointUrl = currentPrivacy.customEndpointUrl)
            )
            val result = try {
                router.routeSuspend(
                    rawInput = queryText,
                    recentHistory = conversationMemory.recentMessages()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                HybridRoutingResult.Error("The AI request failed unexpectedly. Please try again.")
            }

            val (reply, display) = when (result) {
                is HybridRoutingResult.LocalAction -> {
                    val localReply = when (val local = result.commandResult) {
                        is CommandResult.Reply -> local.text
                        is CommandResult.Clarification -> local.question
                        else -> "That request is handled by SAARTHI's offline controls."
                    }
                    Pair(localReply, "Ready")
                }
                is HybridRoutingResult.OnlineAiResponse -> {
                    conversationMemory.appendTurn(queryText, result.replyText)
                    Pair(result.replyText, result.providerName)
                }
                is HybridRoutingResult.ConsentRequired -> Pair(
                    "Review and enable Cloud AI in Settings before sending general questions. Your local device commands will keep working offline.",
                    "Cloud AI consent required"
                )
                is HybridRoutingResult.PrivacyRefusal -> Pair(result.reason, "Blocked for privacy")
                is HybridRoutingResult.OfflineExplanation -> Pair(result.explanation, "Offline")
                is HybridRoutingResult.Error -> Pair(result.errorText, "AI request failed")
            }

            HistoryManager.addInteraction(context, queryText, reply)
            onComplete(reply, display)
        }
    }

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

    // Telephony & Hands-Free Calling Controllers
    val contactResolver = remember { AndroidContactResolver(context.applicationContext) }
    val audioRouteDetector = remember { AndroidAudioRouteDetector(context.applicationContext) }
    val callPreferencesManager = remember { AndroidCallPreferencesManager(context.applicationContext) }
    val callController = remember { AndroidCallController(context.applicationContext) }
    var callPreferences by remember { mutableStateOf(callPreferencesManager.getPreferences()) }

    fun checkPhonePermissions(): Boolean {
        val phoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return phoneState && contacts
    }

    var hasPhonePermissions by remember { mutableStateOf(checkPhonePermissions()) }

    // Hardware status updater
    fun refreshHardwareStatus() {
        batteryStatus = DeviceHardwareController.getRawBatteryStatus(context)
        isFlashlightOn = DeviceHardwareController.isFlashlightOn(context)
        isFlashAvailable = DeviceHardwareController.isFlashAvailable(context)
        hasPhonePermissions = checkPhonePermissions()
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

    fun executeCommandAction(
        context: Context,
        result: CommandResult,
        queryText: String,
        refreshHardwareStatus: () -> Unit,
        onComplete: (spoken: String, display: String) -> Unit
    ) {
        when (result) {
            is CommandResult.Reply -> {
                HistoryManager.addInteraction(context, queryText, result.text)
                onComplete(result.text, "Ready")
            }
            CommandResult.OpenCalculator -> {
                when (val contract = AppLauncher.execute(context, TargetApp.CALCULATOR)) {
                    is ExecutionContract.Succeeded -> {
                        HistoryManager.addInteraction(context, queryText, contract.spokenFeedback, "Calculator")
                        onComplete(contract.spokenFeedback, contract.displayFeedback)
                    }
                    is ExecutionContract.Failed -> {
                        HistoryManager.addInteraction(context, queryText, contract.spokenFeedback)
                        onComplete(contract.spokenFeedback, contract.displayFeedback)
                    }
                    else -> onComplete("Opening Calculator...", "Calculator")
                }
            }
            is CommandResult.OpenApp -> {
                when (val contract = AppLauncher.execute(context, result.targetApp)) {
                    is ExecutionContract.Succeeded -> {
                        HistoryManager.addInteraction(context, queryText, contract.spokenFeedback, result.targetApp.displayName)
                        onComplete(contract.spokenFeedback, contract.displayFeedback)
                    }
                    is ExecutionContract.Failed -> {
                        HistoryManager.addInteraction(context, queryText, contract.spokenFeedback)
                        onComplete(contract.spokenFeedback, contract.displayFeedback)
                    }
                    else -> onComplete("Opening ${result.targetApp.displayName}...", result.targetApp.displayName)
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
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            is CommandResult.SetFlashlight -> {
                val contract = DeviceHardwareController.setFlashlight(context, result.enabled)
                refreshHardwareStatus()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not change flashlight state.", "Flashlight failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            is CommandResult.OpenSystemSettings -> {
                val contract = SystemSettingsController.openSetting(context, result.settingType, result.isDirectToggleAttempt)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not open settings.", "Settings failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            is CommandResult.SetTimer -> {
                val contract = TimerController.startTimer(context, result.durationSeconds)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not set timer.", "Timer failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.PauseTimer -> {
                val contract = TimerController.pauseTimer()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not pause timer.", "Pause failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.ResumeTimer -> {
                val contract = TimerController.resumeTimer(context)
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not resume timer.", "Resume failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.CancelTimer -> {
                val contract = TimerController.cancelTimer()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Could not cancel timer.", "Cancel failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.GetTimerStatus -> {
                val contract = TimerController.getStatus()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Timer status is unavailable.", "Status unavailable")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.AnswerCall -> {
                val coordinator = CallCoordinator.instance
                val contract = coordinator?.handleVoiceCallAnswer() ?: AndroidCallController(context).answerCall()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Unable to answer call.", "Answer failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            CommandResult.RejectCall -> {
                val coordinator = CallCoordinator.instance
                val contract = coordinator?.handleVoiceCallReject() ?: AndroidCallController(context).rejectCall()
                val (feedback, display) = when (contract) {
                    is ExecutionContract.Succeeded -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    is ExecutionContract.Failed -> Pair(contract.spokenFeedback, contract.displayFeedback)
                    else -> Pair("Unable to decline call.", "Decline failed")
                }
                HistoryManager.addInteraction(context, queryText, feedback, display)
                onComplete(feedback, display)
            }
            is CommandResult.Clarification -> {
                HistoryManager.addInteraction(context, queryText, result.question)
                onComplete(result.question, "Clarification needed")
            }
            is CommandResult.SensitiveConfirmation -> {
                HistoryManager.addInteraction(context, queryText, result.confirmationPrompt)
                onComplete(result.confirmationPrompt, "Confirmation needed")
            }
            is CommandResult.WakeWordAcknowledged -> {
                onComplete(result.message, "Listening for command...")
            }
            CommandResult.StopSpeaking -> {
                textToSpeechEngine.stop()
                onComplete("Speaking stopped.", "Stopped")
            }
            CommandResult.StopListening -> {
                speechEngine.stopListening()
                onComplete("Listening stopped.", "Stopped")
            }
            CommandResult.NeedsOnlineAI -> {
                routeOnlineQuery(queryText, onComplete)
            }
            CommandResult.Ignore -> {
                onComplete("", "Ready")
            }
        }
    }

    // Voice Session Coordinator
    val voiceSessionCoordinator = remember {
        VoiceSessionCoordinator(
            voiceEngine = speechEngine,
            speechSynthesizer = textToSpeechEngine,
            commandEngine = commandEngine,
            actionExecutor = { commandResult, onComplete ->
                executeCommandAction(context, commandResult, lastUserQuery, ::refreshHardwareStatus, onComplete)
            }
        ).apply {
            this.isVoiceFeedbackEnabled = isVoiceFeedbackEnabled
        }
    }

    LaunchedEffect(isVoiceFeedbackEnabled) {
        voiceSessionCoordinator.isVoiceFeedbackEnabled = isVoiceFeedbackEnabled
    }

    val smartCallerAnnouncer = remember {
        SmartCallerAnnouncer(
            speechSynthesizer = textToSpeechEngine,
            audioRouteDetector = audioRouteDetector,
            preferencesManager = callPreferencesManager
        )
    }

    val callCoordinator = remember(voiceSessionCoordinator) {
        CallCoordinator(
            contactResolver = contactResolver,
            audioRouteDetector = audioRouteDetector,
            callPreferencesManager = callPreferencesManager,
            callController = callController,
            smartCallerAnnouncer = smartCallerAnnouncer,
            voiceSessionCoordinator = voiceSessionCoordinator
        )
    }

    LaunchedEffect(voiceSessionCoordinator) {
        voiceSessionCoordinator.onStateChanged = { convState ->
            micState = when (convState) {
                ConversationState.IDLE -> MicState.IDLE
                ConversationState.LISTENING -> MicState.LISTENING
                ConversationState.AWAITING_FOLLOW_UP -> MicState.LISTENING
                ConversationState.PROCESSING, ConversationState.EXECUTING -> MicState.PROCESSING
                ConversationState.SPEAKING -> MicState.RESPONDING
                ConversationState.ERROR -> if (voiceSessionCoordinator.statusText.contains("permission", ignoreCase = true)) MicState.PERMISSION_DENIED else MicState.IDLE
            }
        }
        voiceSessionCoordinator.onStatusChanged = { newStatus ->
            statusText = newStatus
        }
        voiceSessionCoordinator.onTranscriptReceived = { text ->
            lastUserQuery = text
        }
        voiceSessionCoordinator.onResponseReceived = { response ->
            lastAssistantResponse = response
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceSessionCoordinator.stopSession()
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

    val phonePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPhonePermissions = checkPhonePermissions()
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
        voiceSessionCoordinator.handleSpeechRecognized(trimmed)
    }

    // Mic action trigger
    fun handleMicAction() {
        if (!hasAudioPermission) {
            requestPermissions {
                handleMicAction()
            }
            return
        }

        if (voiceSessionCoordinator.isSessionActive) {
            if (voiceSessionCoordinator.state == ConversationState.SPEAKING) {
                textToSpeechEngine.stop()
                voiceSessionCoordinator.stopSession("Ready")
            } else {
                voiceSessionCoordinator.stopSession("Stopped listening")
            }
        } else {
            voiceSessionCoordinator.startSession()
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
                            voiceSessionCoordinator.cancelCurrentOperation()
                        },
                        onStopSpeaking = {
                            voiceSessionCoordinator.stopSession("Ready")
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
                            clearConversationHistory()
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
                            clearConversationHistory()
                        },
                        privacySettings = privacySettings,
                        onToggleCloudAi = { enabled ->
                            if (enabled) {
                                privacyController.setCloudAiEnabled(true)
                            } else {
                                privacyController.clearConsent()
                                privacyController.setAllowConversationContext(false)
                            }
                            privacySettings = privacyController.getSettings()
                        },
                        onGrantCloudAiConsent = {
                            privacyController.setCloudConsentGranted(true)
                            privacyController.setCloudAiEnabled(true)
                            privacySettings = privacyController.getSettings()
                        },
                        onToggleCloudContext = { enabled ->
                            privacyController.setAllowConversationContext(enabled)
                            privacySettings = privacyController.getSettings()
                        },
                        onSaveCloudEndpoint = { endpoint ->
                            privacyController.setCustomEndpointUrl(endpoint)
                            privacySettings = privacyController.getSettings()
                        },
                        callPreferences = callPreferences,
                        onToggleCallerAnnouncement = {
                            callPreferencesManager.setCallerAnnouncementEnabled(it)
                            callPreferences = callPreferencesManager.getPreferences()
                        },
                        onToggleSpeakerAnnouncement = {
                            callPreferencesManager.setSpeakerAnnouncementEnabled(it)
                            callPreferences = callPreferencesManager.getPreferences()
                        },
                        onToggleEarbudsAnnouncement = {
                            callPreferencesManager.setEarbudsAnnouncementEnabled(it)
                            callPreferences = callPreferencesManager.getPreferences()
                        },
                        onToggleVoiceCallControl = {
                            callPreferencesManager.setVoiceCallControlEnabled(it)
                            callPreferences = callPreferencesManager.getPreferences()
                        },
                        onToggleAnnounceUnknown = {
                            callPreferencesManager.setAnnounceUnknownNumbers(it)
                            callPreferences = callPreferencesManager.getPreferences()
                        },
                        hasPhonePermissions = hasPhonePermissions,
                        onRequestPhonePermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_CONTACTS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                perms.add(Manifest.permission.ANSWER_PHONE_CALLS)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                perms.add(Manifest.permission.READ_CALL_LOG)
                            }
                            phonePermissionsLauncher.launch(perms.toTypedArray())
                        }
                    )
                }
            }
        }
    }
}
