package com.nedmah.textlector.common.platform.tts

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.common.platfrom.tts.WavCodec
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.PiperModelPath
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.VoiceModelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DesktopPiperTtsEngine(
    private val modelRepository: VoiceModelRepository,
) : SherpaOnnxTtsEngine {

    private var tts: OfflineTts? = null
    private val ttsMutex = Mutex() // native object must not be released while generating, otherwise crash
    private var currentVoiceId: VoiceId? = null
    private var loadingJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val player = DesktopAudioPlayer()
    private var paragraphs: List<Paragraph> = emptyList()

    override suspend fun loadVoice(model: VoiceModel) {
        loadingJob = engineScope.launch {
            ttsMutex.withLock {
                if (model.id == currentVoiceId) return@withLock
                val path = modelRepository.getModelPath(model.id) ?: return@withLock
                tts?.release()
                tts = null
                tts = buildOfflineTts(path)
                currentVoiceId = model.id
                CrashReporter.log("loadVoice: ${model.id}", tag = "DesktopPiper")
            }
        }
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        this.paragraphs = paragraphs
    }

    override suspend fun speak(index: Int, speed: Float) {
        val text = paragraphs.getOrNull(index)?.text ?: return
        val audio = generate(text, speed)
        if (audio.isNotEmpty()) playAudio(audio)
    }

    override suspend fun generate(text: String, speed: Float): ByteArray {
        loadingJob?.join()
        return withContext(Dispatchers.IO) {
            try {
                val audio = ttsMutex.withLock { tts?.generate(text, 0, speed) }
                    ?: return@withContext ByteArray(0)
                WavCodec.fromSamples(audio.samples, audio.sampleRate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CrashReporter.recordException(e, "Piper generate failed")
                ByteArray(0)
            }
        }
    }

    override suspend fun playAudio(audio: ByteArray) {
        withContext(Dispatchers.IO) { player.play(audio) }
    }

    override fun stop() = player.stop()

    override fun shutdown() {
        engineScope.cancel()
        player.stop()
        tts?.release()
        tts = null
        currentVoiceId = null
    }

    private fun buildOfflineTts(path: PiperModelPath): OfflineTts {
        SherpaNativeLoader.ensureLoaded()
        val vits = OfflineTtsVitsModelConfig.builder()
            .setModel(path.onnxPath)
            .setTokens(path.tokensPath)
            .setDataDir(path.espeakDataPath)
            .setNoiseScale(0.667f)
            .setNoiseScaleW(0.8f)
            .setLengthScale(1.0f)
            .build()
        val model = OfflineTtsModelConfig.builder()
            .setVits(vits)
            .setNumThreads(4)
            .setDebug(false)
            .setProvider("cpu")
            .build()
        val config = OfflineTtsConfig.builder()
            .setModel(model)
            .setMaxNumSentences(1)
            .build()
        return OfflineTts(config)
    }
}