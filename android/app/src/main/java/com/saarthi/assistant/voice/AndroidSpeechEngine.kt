package com.saarthi.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidSpeechEngine(
    private val context: Context
) : VoiceEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var listening = false
    @Volatile
    private var isStarting = false

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        runOnMain {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition service is not available on this device.")
                return@runOnMain
            }

            if (listening || isStarting) {
                return@runOnMain
            }
            isStarting = true

            // Clean up previous recognizer instance to prevent binder leaks
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            } catch (e: Throwable) {
                isStarting = false
                listening = false
                onError("Failed to initialize speech recognizer: ${e.message}")
                return@runOnMain
            }

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        isStarting = false
                        listening = true
                    }

                    override fun onBeginningOfSpeech() {
                        // User started speaking.
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Future: voice level visualization.
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Not needed for now.
                    }

                    override fun onEndOfSpeech() {
                        // Keep listening marked true until onResults or onError completes
                    }

                    override fun onError(error: Int) {
                        isStarting = false
                        listening = false
                        mainHandler.post {
                            onError(errorMessage(error))
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isStarting = false
                        listening = false

                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val text = matches?.firstOrNull()

                        mainHandler.post {
                            if (!text.isNullOrBlank()) {
                                onResult(text)
                            } else {
                                onError("I could not understand that.")
                            }
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {
                        // Future: live transcription.
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {
                        // Not needed for now.
                    }
                }
            )

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )

                // Hint to Android platform to prefer offline / on-device speech recognition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Throwable) {
                isStarting = false
                listening = false
                onError("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        runOnMain {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {}
            isStarting = false
            listening = false
        }
    }

    override fun cancelListening() {
        runOnMain {
            try {
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
            isStarting = false
            listening = false
        }
    }

    override fun isListening(): Boolean {
        return listening || isStarting
    }

    override fun release() {
        runOnMain {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
            isStarting = false
            listening = false
        }
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Microphone audio error."

            SpeechRecognizer.ERROR_CLIENT ->
                "Speech recognition client error."

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required."

            SpeechRecognizer.ERROR_NETWORK ->
                "Network error during speech recognition. (Offline voice may require downloaded language pack)."

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Speech recognition network timeout."

            SpeechRecognizer.ERROR_NO_MATCH ->
                "No speech match recognized."

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech recognizer is busy."

            SpeechRecognizer.ERROR_SERVER ->
                "Speech recognition server error."

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No speech detected."

            else ->
                "Speech recognition error ($error)."
        }
    }
}