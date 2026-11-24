package com.saarthi.assistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureHttpAiProviderTest {

    @Test
    fun blankEndpoint_returnsClientErrorWithoutNetworkCall() {
        val provider = SecureHttpAiProvider(endpointUrl = "")
        val result = provider.generateResponse(AiRequest(prompt = "Hello"))
        assertTrue(result is AiProviderResult.ClientError)
        val msg = (result as AiProviderResult.ClientError).message
        assertTrue(msg.contains("No AI service endpoint is configured"))
    }

    @Test
    fun zeroHardcodedSecrets_inProviderConstants() {
        // Verify default endpoint and constants do not contain secret keys
        assertFalse(SecureHttpAiProvider.DEFAULT_ENDPOINT.contains("key="))
        assertFalse(SecureHttpAiProvider.DEFAULT_ENDPOINT.contains("secret"))
        assertEquals("llama-3.1-8b-instant", SecureHttpAiProvider.DEFAULT_MODEL)
    }

    @Test
    fun unreachableHost_returnsNetworkError() {
        val provider = SecureHttpAiProvider(endpointUrl = "https://invalid-subdomain-that-does-not-exist.test/api")
        val result = provider.generateResponse(AiRequest(prompt = "Hello"))
        assertTrue("Expected NetworkError for unreachable host, got: $result", result is AiProviderResult.NetworkError)
        val netErr = result as AiProviderResult.NetworkError
        assertTrue(netErr.message.isNotBlank())
    }

    @Test
    fun httpEndpoint_isRejectedWithoutNetworkCall() {
        val provider = SecureHttpAiProvider(endpointUrl = "http://example.com/api/chat")
        val result = provider.generateResponse(AiRequest(prompt = "Hello"))

        assertTrue(result is AiProviderResult.ClientError)
        assertTrue((result as AiProviderResult.ClientError).message.contains("HTTPS"))
    }
}
