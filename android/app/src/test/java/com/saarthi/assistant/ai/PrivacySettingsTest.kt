package com.saarthi.assistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrivacySettingsTest {

    private lateinit var privacyManager: InMemoryPrivacyManager

    @Before
    fun setUp() {
        privacyManager = InMemoryPrivacyManager()
    }

    @Test
    fun defaultPrivacySettings_cloudAiIsDisabledByDefault() {
        val settings = privacyManager.getSettings()
        assertFalse("Cloud AI must be disabled by default", settings.isCloudAiEnabled)
        assertFalse("Cloud consent must not be granted by default", settings.isCloudConsentGranted)
        assertFalse("Confirmation requirement is false by default", settings.requireConfirmationBeforeSending)
        assertFalse("Context transmission is false by default", settings.allowConversationContextInCloud)
        assertEquals("", settings.customEndpointUrl)
    }

    @Test
    fun togglingSettings_persistsCorrectly() {
        privacyManager.setCloudAiEnabled(true)
        assertTrue(privacyManager.getSettings().isCloudAiEnabled)

        privacyManager.setCloudConsentGranted(true)
        assertTrue(privacyManager.getSettings().isCloudConsentGranted)

        privacyManager.setRequireConfirmation(true)
        assertTrue(privacyManager.getSettings().requireConfirmationBeforeSending)

        privacyManager.setAllowConversationContext(true)
        assertTrue(privacyManager.getSettings().allowConversationContextInCloud)

        privacyManager.setCustomEndpointUrl("https://my-proxy.workers.dev/api/chat")
        assertEquals("https://my-proxy.workers.dev/api/chat", privacyManager.getSettings().customEndpointUrl)
    }

    @Test
    fun clearConsent_disablesCloudAiAndRevokesConsentImmediately() {
        privacyManager.setCloudAiEnabled(true)
        privacyManager.setCloudConsentGranted(true)
        assertTrue(privacyManager.getSettings().isCloudAiEnabled)

        privacyManager.clearConsent()
        assertFalse(privacyManager.getSettings().isCloudAiEnabled)
        assertFalse(privacyManager.getSettings().isCloudConsentGranted)
    }
}
