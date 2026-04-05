package com.nedmah.textlector.common.platform.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTtsEngine(
    private val context: Context
) : TtsEngine {

    companion object {
        private const val UTTERANCE_ID = "lector_utterance"
    }

    private var tts: TextToSpeech? = null
    private var isReady : Boolean = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isReady = true
            }
        }
    }

    override fun speak(text: String, speed: Float, onDone: () -> Unit) {
        if (!isReady) return

        tts?.setSpeechRate(speed)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                Handler(Looper.getMainLooper()).post { onDone() }  // bc playerVM updates state on main thread
            }
            override fun onError(utteranceId: String?) {}
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

}