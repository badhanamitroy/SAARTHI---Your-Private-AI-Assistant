package com.saarthi.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechEngine(
    context: Context
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (_: Throwable) {
        null
    }

    private var initialized = false

    var onSpeechStart: (() -> Unit)? = null
    var onSpeechDone: (() -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    override fun onInit(status: Int) {
        val tts = textToSpeech
        if (status == TextToSpeech.SUCCESS && tts != null) {
            try {
                val result = tts.setLanguage(Locale.getDefault())

                initialized =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                tts.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {

                        override fun onStart(utteranceId: String?) {
                            onSpeechStart?.invoke()
                        }

                        override fun onDone(utteranceId: String?) {
                            onSpeechDone?.invoke()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            onSpeechError?.invoke("Audio playback error.")
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            onSpeechError?.invoke("Audio playback error ($errorCode).")
                        }

                        override fun onStop(utteranceId: String?, interrupted: Boolean) {
                            onSpeechDone?.invoke()
                        }
                    }
                )
            } catch (_: Throwable) {
                initialized = false
                onSpeechError?.invoke("Text-to-speech initialization failed.")
            }
        } else {
            initialized = false
            onSpeechError?.invoke("Text-to-speech initialization failed.")
        }
    }

    fun isInitialized(): Boolean = initialized

    fun speak(text: String) {
        val tts = textToSpeech
        if (!initialized || tts == null || text.isBlank()) {
            return
        }

        try {
            val speechText = SpeechPronunciationFormatter.formatForSpeech(text)

            tts.speak(
                speechText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SAARTHI_RESPONSE"
            )
        } catch (_: Throwable) {}
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (_: Throwable) {}
        onSpeechDone?.invoke()
    }

    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Throwable) {}
        textToSpeech = null
        initialized = false
    }
}