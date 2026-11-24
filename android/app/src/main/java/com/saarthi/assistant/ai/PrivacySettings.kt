package com.saarthi.assistant.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * Immutable snapshot of the user's active privacy configuration.
 */
data class PrivacySettings(
    val isCloudAiEnabled: Boolean = false,
    val isCloudConsentGranted: Boolean = false,
    val requireConfirmationBeforeSending: Boolean = false,
    val allowConversationContextInCloud: Boolean = false,
    val customEndpointUrl: String = "",
    val activeProviderId: String = "GROQ_DEFAULT"
)

/**
 * Interface defining privacy operations for decoupling and testability.
 */
interface PrivacyManager {
    fun getSettings(): PrivacySettings
    fun setCloudAiEnabled(enabled: Boolean)
    fun setCloudConsentGranted(granted: Boolean)
    fun setRequireConfirmation(required: Boolean)
    fun setAllowConversationContext(allowed: Boolean)
    fun setCustomEndpointUrl(url: String)
    fun setActiveProviderId(providerId: String)
    fun clearConsent()
}

/**
 * Android SharedPreferences-backed implementation of PrivacyManager.
 */
class AndroidPrivacyController(
    context: Context
) : PrivacyManager {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getSettings(): PrivacySettings {
        return PrivacySettings(
            isCloudAiEnabled = prefs.getBoolean(KEY_CLOUD_AI_ENABLED, false),
            isCloudConsentGranted = prefs.getBoolean(KEY_CLOUD_CONSENT_GRANTED, false),
            requireConfirmationBeforeSending = prefs.getBoolean(KEY_REQUIRE_CONFIRMATION, false),
            allowConversationContextInCloud = prefs.getBoolean(KEY_ALLOW_CONTEXT, false),
            customEndpointUrl = prefs.getString(KEY_CUSTOM_ENDPOINT, "") ?: "",
            activeProviderId = prefs.getString(KEY_ACTIVE_PROVIDER, "GROQ_DEFAULT") ?: "GROQ_DEFAULT"
        )
    }

    override fun setCloudAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_AI_ENABLED, enabled).apply()
    }

    override fun setCloudConsentGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_CONSENT_GRANTED, granted).apply()
    }

    override fun setRequireConfirmation(required: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_CONFIRMATION, required).apply()
    }

    override fun setAllowConversationContext(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_CONTEXT, allowed).apply()
    }

    override fun setCustomEndpointUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_ENDPOINT, url.trim()).apply()
    }

    override fun setActiveProviderId(providerId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, providerId).apply()
    }

    override fun clearConsent() {
        prefs.edit()
            .putBoolean(KEY_CLOUD_AI_ENABLED, false)
            .putBoolean(KEY_CLOUD_CONSENT_GRANTED, false)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "saarthi_privacy_prefs"
        const val KEY_CLOUD_AI_ENABLED = "cloud_ai_enabled"
        const val KEY_CLOUD_CONSENT_GRANTED = "cloud_consent_granted"
        const val KEY_REQUIRE_CONFIRMATION = "require_cloud_confirmation"
        const val KEY_ALLOW_CONTEXT = "allow_cloud_context"
        const val KEY_CUSTOM_ENDPOINT = "custom_endpoint_url"
        const val KEY_ACTIVE_PROVIDER = "active_provider_id"

        const val FIRST_USE_DISCLOSURE =
            "SAARTHI can optionally connect to Cloud AI to answer general knowledge questions beyond its offline capabilities.\n\n" +
            "Privacy Rules:\n" +
            "• The question is sent as text over HTTPS. Recent AI-only chat context is included only if you separately enable it in Settings.\n" +
            "• Zero microphone audio, contacts, SMS, files, or personal phone data are sent.\n" +
            "• Known local actions (apps, timer, torch, settings, math) always run 100% offline.\n" +
            "• You can disable Cloud AI at any time in Settings."
    }
}

/**
 * In-memory PrivacyManager for fast, deterministic unit testing.
 */
class InMemoryPrivacyManager(
    initialSettings: PrivacySettings = PrivacySettings()
) : PrivacyManager {

    private var current = initialSettings

    override fun getSettings(): PrivacySettings = current

    override fun setCloudAiEnabled(enabled: Boolean) {
        current = current.copy(isCloudAiEnabled = enabled)
    }

    override fun setCloudConsentGranted(granted: Boolean) {
        current = current.copy(isCloudConsentGranted = granted)
    }

    override fun setRequireConfirmation(required: Boolean) {
        current = current.copy(requireConfirmationBeforeSending = required)
    }

    override fun setAllowConversationContext(allowed: Boolean) {
        current = current.copy(allowConversationContextInCloud = allowed)
    }

    override fun setCustomEndpointUrl(url: String) {
        current = current.copy(customEndpointUrl = url)
    }

    override fun setActiveProviderId(providerId: String) {
        current = current.copy(activeProviderId = providerId)
    }

    override fun clearConsent() {
        current = current.copy(isCloudAiEnabled = false, isCloudConsentGranted = false)
    }
}
