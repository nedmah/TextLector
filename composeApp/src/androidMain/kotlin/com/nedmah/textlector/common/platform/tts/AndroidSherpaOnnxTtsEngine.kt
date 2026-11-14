package com.nedmah.textlector.common.platform.tts

import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.PiperModelPath
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidSherpaOnnxTtsEngine(
    private val modelRepository: VoiceModelRepository
) : SherpaOnnxTtsEngine {

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var currentVoiceId: VoiceId? = null
    private var loadingJob: Job? = null  // we need to read model and build a session before playing
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var paragraphs: List<Paragraph> = emptyList()

    override suspend fun loadVoice(model: VoiceModel) {
        if (model.id == currentVoiceId) return
        loadingJob = engineScope.launch {
            val path = modelRepository.getModelPath(model.id) ?: return@launch
            tts?.release()
            tts = buildOfflineTts(path)
            currentVoiceId = model.id
            CrashReporter.log("loadVoice: ${model.id}", tag = "SherpaEngine")
        }
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        this.paragraphs = paragraphs
    }

    override suspend fun speak(index: Int, speed: Float) {
        loadingJob?.join()
        val engine = tts ?: return
        val text = paragraphs.getOrNull(index)?.text ?: return

        withContext(Dispatchers.IO) {
            val audio = engine.generate(text = text, sid = 0, speed = speed)
            if (coroutineContext.isActive) {
                AndroidTrackPlayer.play(audio.samples, audio.sampleRate) { track ->
                    audioTrack?.release()
                    audioTrack = track
                }
            }
        }
    }

    override suspend fun generate(text: String, speed: Float): ByteArray {
        loadingJob?.join()
        val engine = tts ?: return ByteArray(0)
        return withContext(Dispatchers.IO) {
            try {
                val audio = engine.generate(text = text, sid = 0, speed = speed)
                AndroidTrackPlayer.samplesToWav(audio.samples, audio.sampleRate)
            } catch (e: Exception) {
                CrashReporter.recordException(e, "generate failed")
                ByteArray(0)
            }
        }
    }

    override suspend fun playAudio(audio: ByteArray) {
        withContext(Dispatchers.IO) {
            AndroidTrackPlayer.play(audio) { track ->
                audioTrack?.release()
                audioTrack = track
            }
        }
    }

    override fun stop() {
        audioTrack?.apply {
            pause()
            flush()
        }
    }

    override fun shutdown() {
        engineScope.coroutineContext[Job]?.cancel()
        audioTrack?.release()
        audioTrack = null
        tts?.release()
        tts = null
        currentVoiceId = null
    }

    private fun buildOfflineTts(path: PiperModelPath): OfflineTts {
        val vitsConfig = OfflineTtsVitsModelConfig(
            model = path.onnxPath,
            lexicon = "",
            tokens = path.tokensPath,
            dataDir = path.espeakDataPath,
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f
        )
        val modelConfig = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 4,
            debug = false,
            provider = "cpu"
        )
        val config = OfflineTtsConfig(
            model = modelConfig,
            maxNumSentences = 1
        )
        CrashReporter.log("buildOfflineTts: onnx=${path.onnxPath}", tag = "SherpaEngine")
        return try {
            OfflineTts(config = config)
        } catch (e: Exception) {
            CrashReporter.recordException(e, "buildOfflineTts failed")
            throw e
        }
    }

}