package com.saarthi.assistant.core

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val id: String = System.currentTimeMillis().toString() + "_" + (100..999).random(),
    val query: String,
    val response: String,
    val formattedTime: String,
    val appLabel: String? = null
)

object HistoryManager {
    private const val PREFS_NAME = "saarthi_history"
    private const val KEY_ENTRIES = "history_entries"
    private const val MAX_ENTRIES = 40

    val items = mutableStateListOf<HistoryItem>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_ENTRIES, null) ?: return

            val lines = raw.split("\u001F")
            items.clear()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("\u001E")
                if (parts.size >= 4) {
                    items.add(
                        HistoryItem(
                            id = parts[0],
                            query = parts[1],
                            response = parts[2],
                            formattedTime = parts[3],
                            appLabel = parts.getOrNull(4)?.ifEmpty { null }
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    fun addInteraction(context: Context, query: String, response: String, appLabel: String? = null) {
        try {
            init(context)
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val item = HistoryItem(
                query = query,
                response = response,
                formattedTime = timeStr,
                appLabel = appLabel
            )
            // Add to front of history
            items.add(0, item)
            if (items.size > MAX_ENTRIES) {
                items.removeRange(MAX_ENTRIES, items.size)
            }
            persist(context)
        } catch (_: Throwable) {}
    }

    fun clear(context: Context) {
        try {
            items.clear()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_ENTRIES).apply()
        } catch (_: Throwable) {}
    }

    private fun persist(context: Context) {
        try {
            val serialized = items.take(MAX_ENTRIES).joinToString("\u001F") { item ->
                "${item.id}\u001E${item.query}\u001E${item.response}\u001E${item.formattedTime}\u001E${item.appLabel ?: ""}"
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ENTRIES, serialized).apply()
        } catch (_: Throwable) {}
    }
}
