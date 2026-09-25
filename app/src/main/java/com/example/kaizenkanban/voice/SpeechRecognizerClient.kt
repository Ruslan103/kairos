package com.example.kaizenkanban.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Thin wrapper around [SpeechRecognizer]. Call [destroy] when done.
 * Listener callbacks are always delivered on the main thread (required for Toast/UI).
 */
class SpeechRecognizerClient(
    context: Context,
    private val onPartial: ((String) -> Unit)? = null,
    private val onResult: (String) -> Unit,
    private val onError: (Int) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var stopped = false

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(locale: Locale) {
        if (!isAvailable) {
            postError(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        stop()
        stopped = false
        val speech = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (stopped) return
                mainHandler.post { onListeningChanged(true) }
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                if (stopped) return
                mainHandler.post { onListeningChanged(false) }
            }

            override fun onError(error: Int) {
                if (stopped) return
                stopped = true
                mainHandler.post {
                    onListeningChanged(false)
                    onError(error)
                }
            }

            override fun onResults(results: Bundle?) {
                if (stopped) return
                stopped = true
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                mainHandler.post {
                    onListeningChanged(false)
                    if (text.isNotBlank()) onResult(text)
                    else onError(SpeechRecognizer.ERROR_NO_MATCH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (stopped) return
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!text.isNullOrBlank()) {
                    mainHandler.post { onPartial?.invoke(text) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        runCatching { speech.startListening(intent) }
            .onFailure { postError(SpeechRecognizer.ERROR_CLIENT) }
    }

    private fun postError(code: Int) {
        mainHandler.post {
            onListeningChanged(false)
            onError(code)
        }
    }

    fun stop() {
        stopped = true
        runCatching {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
        // Do not invoke onListeningChanged here during replace — avoids UI flicker.
    }

    fun destroy() {
        stopped = true
        mainHandler.post { onListeningChanged(false) }
        runCatching {
            recognizer?.destroy()
        }
        recognizer = null
    }
}
