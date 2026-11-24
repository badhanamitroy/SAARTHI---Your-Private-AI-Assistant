package com.saarthi.assistant.ai

import com.saarthi.assistant.core.CommandEngine
import com.saarthi.assistant.core.CommandResult
import com.saarthi.assistant.core.SystemSettingType
import com.saarthi.assistant.core.TargetApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HybridRequestRouterTest {

    private lateinit var commandEngine: CommandEngine
    private lateinit var privacyManager: InMemoryPrivacyManager
    private lateinit var networkChecker: FakeNetworkChecker
    private lateinit var mockAiProvider: MockAiProvider
    private lateinit var router: HybridRequestRouter

    @Before
    fun setUp() {
        commandEngine = CommandEngine()
        privacyManager = InMemoryPrivacyManager()
        networkChecker = FakeNetworkChecker(connected = true)
        mockAiProvider = MockAiProvider()

        router = HybridRequestRouter(
            commandEngine = commandEngine,
            privacyManager = privacyManager,
            networkChecker = networkChecker,
            aiProvider = mockAiProvider
        )
    }

    // -------------------------------------------------------------
    // Test Category 1: Local-First Priority (Zero Cloud Calls for Known Actions)
    // -------------------------------------------------------------

    @Test
    fun localActions_openCalculator_neverCallsCloudAi() {
        // Even if Cloud AI is enabled and consented, local actions ALWAYS execute locally!
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("open calculator")
        assertTrue(result is HybridRoutingResult.LocalAction)
        assertEquals(CommandResult.OpenCalculator, (result as HybridRoutingResult.LocalAction).commandResult)
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun localActions_batteryStatus_neverCallsCloudAi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("battery koto ache?")
        assertTrue(result is HybridRoutingResult.LocalAction)
        assertEquals(CommandResult.GetBatteryStatus, (result as HybridRoutingResult.LocalAction).commandResult)
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun localActions_flashlight_neverCallsCloudAi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("Torch on koro")
        assertTrue(result is HybridRoutingResult.LocalAction)
        val cmd = (result as HybridRoutingResult.LocalAction).commandResult
        assertTrue(cmd is CommandResult.SetFlashlight && cmd.enabled)
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun localActions_setTimer_neverCallsCloudAi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("set a timer for 5 minutes")
        assertTrue(result is HybridRoutingResult.LocalAction)
        val cmd = (result as HybridRoutingResult.LocalAction).commandResult
        assertTrue(cmd is CommandResult.SetTimer)
        assertEquals(300L, (cmd as CommandResult.SetTimer).durationSeconds)
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun localActions_safeMath_neverCallsCloudAi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("what is 27 times 48")
        assertTrue(result is HybridRoutingResult.LocalAction)
        val cmd = (result as HybridRoutingResult.LocalAction).commandResult
        assertTrue(cmd is CommandResult.Reply)
        assertTrue((cmd as CommandResult.Reply).text.contains("1296"))
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun localActions_wifiSettings_neverCallsCloudAi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("Turn off Wi-Fi")
        assertTrue(result is HybridRoutingResult.LocalAction)
        val cmd = (result as HybridRoutingResult.LocalAction).commandResult
        assertTrue(cmd is CommandResult.OpenSystemSettings)
        assertEquals(SystemSettingType.WIFI, (cmd as CommandResult.OpenSystemSettings).settingType)
        assertEquals(0, mockAiProvider.callCount)
    }

    // -------------------------------------------------------------
    // Test Category 2: Privacy Policy & Consent Gates for Unknown Queries
    // -------------------------------------------------------------

    @Test
    fun unknownQuery_whenCloudAiDisabled_returnsOfflineExplanationWithoutCallingCloud() {
        privacyManager.setCloudAiEnabled(false)

        val result = router.route("Explain why the sky is blue")
        assertTrue(result is HybridRoutingResult.OfflineExplanation)
        val explanation = (result as HybridRoutingResult.OfflineExplanation).explanation
        assertTrue(explanation.contains("Cloud AI is currently disabled", ignoreCase = true))
        assertTrue(explanation.contains("offline", ignoreCase = true))
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun unknownQuery_whenConsentNotGranted_returnsConsentRequiredPrompt() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(false)

        val result = router.route("Explain quantum computing")
        assertTrue(result is HybridRoutingResult.ConsentRequired)
        val prompt = (result as HybridRoutingResult.ConsentRequired).prompt
        assertTrue(prompt.contains("consent", ignoreCase = true))
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun unknownQuery_whenQueryContainsSensitiveData_blocksLocally() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        // Query contains an API key
        val result = router.route("Check if this key works: sk-proj-1234567890abcdef1234567890abcdef")
        assertTrue(result is HybridRoutingResult.PrivacyRefusal)
        val refusal = (result as HybridRoutingResult.PrivacyRefusal).reason
        assertTrue(refusal.contains("blocked locally", ignoreCase = true))
        assertTrue(refusal.contains("credentials", ignoreCase = true))
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun unknownQuery_whenDeviceIsOffline_returnsOfflineNetworkMessage() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        networkChecker.connected = false

        val result = router.route("What is machine learning?")
        assertTrue(result is HybridRoutingResult.OfflineExplanation)
        val msg = (result as HybridRoutingResult.OfflineExplanation).explanation
        assertTrue(msg.contains("offline", ignoreCase = true))
        assertTrue(msg.contains("internet connection", ignoreCase = true))
        assertEquals(0, mockAiProvider.callCount)
    }

    // -------------------------------------------------------------
    // Test Category 3: Successful Online AI Requests & Data Minimization
    // -------------------------------------------------------------

    @Test
    fun unknownQuery_whenPermittedAndOnline_callsProviderAndReturnsResponse() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        networkChecker.connected = true
        mockAiProvider.simulatedResult = AiProviderResult.Success(
            AiResponse(
                text = "Photosynthesis is the process by which green plants use sunlight to synthesize nutrients.",
                providerId = "MOCK_PROVIDER",
                tokensUsed = 35
            )
        )

        val result = router.route("How does photosynthesis work?")
        assertTrue(result is HybridRoutingResult.OnlineAiResponse)
        val onlineResponse = result as HybridRoutingResult.OnlineAiResponse
        assertTrue(onlineResponse.replyText.contains("Photosynthesis"))
        assertEquals("Mock AI Provider", onlineResponse.providerName)
        assertEquals(1, mockAiProvider.callCount)
        assertEquals("How does photosynthesis work?", mockAiProvider.lastRequest?.prompt)
    }

    @Test
    fun conversationContext_isExcludedWhenNotPermittedInSettings() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        privacyManager.setAllowConversationContext(false) // Opted out of context transmission

        val history = listOf(
            AiChatMessage("user", "Hello"),
            AiChatMessage("assistant", "Hi there")
        )

        router.route("What is gravity?", recentHistory = history)
        assertEquals(1, mockAiProvider.callCount)
        assertTrue(mockAiProvider.lastRequest?.conversationHistory?.isEmpty() == true)
    }

    @Test
    fun conversationContext_isBoundedToRecentTurnsWhenPermitted() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        privacyManager.setAllowConversationContext(true)

        // 10 historical turns (20 messages)
        val longHistory = (1..20).map { i ->
            AiChatMessage(if (i % 2 == 1) "user" else "assistant", "Turn $i")
        }

        router.route("Tell me more", recentHistory = longHistory)
        assertEquals(1, mockAiProvider.callCount)
        // Must be capped at 6 messages (3 recent turns) for strict data minimization
        assertEquals(6, mockAiProvider.lastRequest?.conversationHistory?.size)
    }

    // -------------------------------------------------------------
    // Test Category 4: Error Handling & Quota Protection
    // -------------------------------------------------------------

    @Test
    fun quotaExceeded_returnsCleanUserError() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        mockAiProvider.simulatedResult = AiProviderResult.QuotaExceeded()

        val result = router.route("What is deep learning?")
        assertTrue(result is HybridRoutingResult.Error)
        val err = (result as HybridRoutingResult.Error).errorText
        assertTrue(err.contains("free AI usage quota", ignoreCase = true))
    }

    @Test
    fun routeToCommandResult_producesCleanCommandResultForExistingUi() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        mockAiProvider.simulatedResult = AiProviderResult.Success(
            AiResponse("The moon orbits the Earth at 384,400 km.", "MOCK_PROVIDER")
        )

        val cmdResult = router.routeToCommandResult("How far is the moon?")
        assertTrue(cmdResult is CommandResult.Reply)
        assertTrue((cmdResult as CommandResult.Reply).text.contains("384,400 km"))
    }

    // -------------------------------------------------------------
    // Test Category 5: Request Classification Architecture
    // -------------------------------------------------------------

    @Test
    fun classification_localDeviceCommands_classifiedCorrectly() {
        val deviceCommands = listOf(
            "open calculator",
            "turn on flashlight",
            "battery status",
            "open settings",
            "set timer for 10 minutes",
            "call dhoro",
            "phone kato",
            "stop listening"
        )

        for (cmd in deviceCommands) {
            val classification = router.classify(cmd)
            assertEquals("Expected LOCAL_DEVICE_COMMAND for '$cmd'", RequestClassification.LOCAL_DEVICE_COMMAND, classification)
        }
    }

    @Test
    fun classification_localOfflineQueries_classifiedCorrectly() {
        val offlineQueries = listOf(
            "what time is it",
            "what is today's date",
            "calculate 25 * 4",
            "hello",
            "who are you",
            "help"
        )

        for (q in offlineQueries) {
            val classification = router.classify(q)
            assertEquals("Expected LOCAL_OFFLINE_QUERY for '$q'", RequestClassification.LOCAL_OFFLINE_QUERY, classification)
        }
    }

    @Test
    fun classification_generalKnowledgeQuestions_classifiedCorrectly() {
        val generalQuestions = listOf(
            "Explain quantum computing in simple terms",
            "Why is the sky blue?",
            "How do airplanes stay in the air?",
            "Who wrote Hamlet?"
        )

        for (q in generalQuestions) {
            val classification = router.classify(q)
            assertEquals("Expected GENERAL_KNOWLEDGE_AI for '$q'", RequestClassification.GENERAL_KNOWLEDGE_AI, classification)
        }
    }

    @Test
    fun classification_sensitiveRequests_classifiedAsSensitiveRejected() {
        val sensitiveQueries = listOf(
            "Check this token sk-1234567890abcdef1234567890abcdef",
            "My password is SuperSecretPassword123",
            "Card number 4532015112830366",
            "Who called me from +8801712345678?",
            "Look up phone number 01711223344 online",
            "Show recent call log"
        )

        for (q in sensitiveQueries) {
            val classification = router.classify(q)
            assertEquals("Expected SENSITIVE_REJECTED for '$q'", RequestClassification.SENSITIVE_REJECTED, classification)
        }
    }

    @Test
    fun unknownQuery_whenQueryContainsPhoneOrContactData_blocksLocally() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)

        val result = router.route("Who called from +8801712345678?")
        assertTrue(result is HybridRoutingResult.PrivacyRefusal)
        val refusal = (result as HybridRoutingResult.PrivacyRefusal).reason
        assertTrue(refusal.contains("blocked locally", ignoreCase = true))
        assertTrue(refusal.contains("never transmits contact, telephony, or private personal data", ignoreCase = true))
        assertEquals(0, mockAiProvider.callCount)
    }

    @Test
    fun routeSuspend_executesSuccessfullyWithCoroutineScope() = runBlocking {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        networkChecker.connected = true
        mockAiProvider.simulatedResult = AiProviderResult.Success(
            AiResponse("Saturn has 146 confirmed moons.", "MOCK_PROVIDER")
        )

        val result = router.routeSuspend("How many moons does Saturn have?")
        assertTrue(result is HybridRoutingResult.OnlineAiResponse)
        val response = result as HybridRoutingResult.OnlineAiResponse
        assertTrue(response.replyText.contains("146 confirmed moons"))
        assertEquals(1, mockAiProvider.callCount)
    }
}
