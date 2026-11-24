package com.saarthi.assistant.ai

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, on-device memory for successful AI conversations.
 *
 * Device commands and their results are deliberately excluded. The router only
 * sends these messages when the user separately enables cloud conversation
 * context, and caps each request to the most recent three turns.
 */
class ConversationMemory(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun recentMessages(maxMessages: Int = MAX_CONTEXT_MESSAGES): List<AiChatMessage> {
        if (maxMessages <= 0) return emptyList()
        return readTurns()
            .takeLast((maxMessages + 1) / 2)
            .flatMap { turn ->
                listOf(
                    AiChatMessage(role = "user", content = turn.prompt),
                    AiChatMessage(role = "assistant", content = turn.response)
                )
            }
            .takeLast(maxMessages)
    }

    @Synchronized
    fun appendTurn(prompt: String, response: String) {
        val cleanPrompt = prompt.trim().take(MAX_MESSAGE_LENGTH)
        val cleanResponse = response.trim().take(MAX_MESSAGE_LENGTH)
        if (cleanPrompt.isBlank() || cleanResponse.isBlank()) return

        val turns = (readTurns() + StoredTurn(cleanPrompt, cleanResponse)).takeLast(MAX_STORED_TURNS)
        val serialized = JSONArray().apply {
            turns.forEach { turn ->
                put(JSONObject().apply {
                    put(KEY_PROMPT, turn.prompt)
                    put(KEY_RESPONSE, turn.response)
                })
            }
        }
        preferences.edit().putString(KEY_TURNS, serialized.toString()).apply()
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_TURNS).apply()
    }

    private fun readTurns(): List<StoredTurn> {
        val serialized = preferences.getString(KEY_TURNS, null) ?: return emptyList()
        return try {
            val entries = JSONArray(serialized)
            (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val prompt = entry.optString(KEY_PROMPT).trim()
                val response = entry.optString(KEY_RESPONSE).trim()
                if (prompt.isBlank() || response.isBlank()) null else StoredTurn(prompt, response)
            }.takeLast(MAX_STORED_TURNS)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private data class StoredTurn(val prompt: String, val response: String)

    companion object {
        private const val PREFS_NAME = "saarthi_ai_memory"
        private const val KEY_TURNS = "ai_turns"
        private const val KEY_PROMPT = "prompt"
        private const val KEY_RESPONSE = "response"
        private const val MAX_STORED_TURNS = 12
        private const val MAX_MESSAGE_LENGTH = 2000
        const val MAX_CONTEXT_MESSAGES = 6
    }
}
