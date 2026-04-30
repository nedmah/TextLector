package com.nedmah.textlector.common.platform.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.nedmah.textlector.domain.model.Paragraph
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AndroidTtsEngine(
    private val context: Context
) : TtsEngine {

    companion object {
        private const val UTTERANCE_ID = "lector_utterance"
    }

    private var tts: TextToSpeech? = null
    private var isReady : Boolean = false
    private var paragraphs: List<Paragraph> = emptyList()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isReady = true
            }
        }
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        this.paragraphs = paragraphs
    }

    override suspend fun speak(index: Int, speed: Float) {
        if (!isReady) return

        val text = paragraphs.getOrNull(index)?.text ?: return

        suspendCancellableCoroutine { cont ->
            tts?.setSpeechRate(speed)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { cont.resume(Unit) }
                override fun onError(utteranceId: String?) { cont.resume(Unit) }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)

            cont.invokeOnCancellation { tts?.stop() }
        }
    }

    override fun stop() { tts?.stop() }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

}