package com.saarthi.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechEngine(
    context: Context
) : TextToSpeech.OnInitListener, SpeechSynthesizer {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var textToSpeech: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (_: Throwable) {
        null
    }

    private var initialized = false
    private var watchdogRunnable: Runnable? = null

    override var onSpeechStart: (() -> Unit)? = null
    override var onSpeechDone: (() -> Unit)? = null
    override var onSpeechError: ((String) -> Unit)? = null

    private fun cancelWatchdog() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

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
                            mainHandler.post {
                                onSpeechStart?.invoke()
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                cancelWatchdog()
                                onSpeechDone?.invoke()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                cancelWatchdog()
                                onSpeechError?.invoke("Audio playback error.")
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post {
                                cancelWatchdog()
                                onSpeechError?.invoke("Audio playback error ($errorCode).")
                            }
                        }

                        override fun onStop(utteranceId: String?, interrupted: Boolean) {
                            mainHandler.post {
                                cancelWatchdog()
                                onSpeechDone?.invoke()
                            }
                        }
                    }
                )
            } catch (_: Throwable) {
                initialized = false
                mainHandler.post {
                    cancelWatchdog()
                    onSpeechError?.invoke("Text-to-speech initialization failed.")
                }
            }
        } else {
            initialized = false
            mainHandler.post {
                cancelWatchdog()
                onSpeechError?.invoke("Text-to-speech initialization failed.")
            }
        }
    }

    override fun isInitialized(): Boolean = initialized

    override fun speak(text: String) {
        if (text.isBlank()) {
            mainHandler.post {
                cancelWatchdog()
                onSpeechDone?.invoke()
            }
            return
        }

        val tts = textToSpeech
        if (!initialized || tts == null) {
            mainHandler.post {
                cancelWatchdog()
                onSpeechError?.invoke("Text-to-speech engine is not initialized.")
            }
            return
        }

        try {
            val speechText = SpeechPronunciationFormatter.formatForSpeech(text)

            cancelWatchdog()
            val timeoutMs = (speechText.length * 120L).coerceIn(4000L, 25000L)
            val watchdog = Runnable {
                watchdogRunnable = null
                onSpeechDone?.invoke()
            }
            watchdogRunnable = watchdog
            mainHandler.postDelayed(watchdog, timeoutMs)

            val status = tts.speak(
                speechText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SAARTHI_RESPONSE"
            )

            if (status != TextToSpeech.SUCCESS) {
                cancelWatchdog()
                mainHandler.post {
                    onSpeechError?.invoke("TTS failed to queue utterance.")
                }
            }
        } catch (e: Throwable) {
            cancelWatchdog()
            mainHandler.post {
                onSpeechError?.invoke("Audio playback error: ${e.message}")
            }
        }
    }

    override fun stop() {
        cancelWatchdog()
        try {
            textToSpeech?.stop()
        } catch (_: Throwable) {}
        mainHandler.post {
            onSpeechDone?.invoke()
        }
    }

    override fun release() {
        cancelWatchdog()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Throwable) {}
        textToSpeech = null
        initialized = false
    }
}