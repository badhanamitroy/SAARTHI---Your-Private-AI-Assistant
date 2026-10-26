package com.saarthi.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidSpeechEngine(
    private val context: Context
) : VoiceEngine {

    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        if (listening) {
            return
        }

        // Clean up previous recognizer instance to prevent binder leaks
        speechRecognizer?.destroy()
        speechRecognizer = null

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
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
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                    onError(errorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    listening = false

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val text = matches?.firstOrNull()

                    if (!text.isNullOrBlank()) {
                        onResult(text)
                    } else {
                        onError("I could not understand that.")
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

        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
        listening = false
    }

    override fun cancelListening() {
        speechRecognizer?.cancel()
        listening = false
    }

    override fun isListening(): Boolean {
        return listening
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        listening = false
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