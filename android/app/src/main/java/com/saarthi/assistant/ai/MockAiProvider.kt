package com.saarthi.assistant.ai

/**
 * Deterministic mock implementation of AiProvider for automated unit testing.
 * Allows simulating success, quota errors, network timeouts, and authentication failures.
 */
class MockAiProvider(
    override val providerId: String = "MOCK_PROVIDER",
    override val providerName: String = "Mock AI Provider"
) : AiProvider {

    var simulatedResult: AiProviderResult = AiProviderResult.Success(
        AiResponse(
            text = "This is a simulated AI response.",
            providerId = providerId,
            tokensUsed = 42
        )
    )

    var callCount: Int = 0
        private set

    var lastRequest: AiRequest? = null
        private set

    override fun generateResponse(request: AiRequest): AiProviderResult {
        callCount++
        lastRequest = request
        return simulatedResult
    }

    fun reset() {
        callCount = 0
        lastRequest = null
        simulatedResult = AiProviderResult.Success(
            AiResponse(
                text = "This is a simulated AI response.",
                providerId = providerId,
                tokensUsed = 42
            )
        )
    }
}
