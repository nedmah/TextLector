package com.nedmah.textlector.common.platform.tts

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsCallback
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.common.platfrom.tts.WavCodec
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.SupertonicModel
import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.SupertonicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DesktopSupertonicTtsEngine(
    private val modelRepository: SupertonicRepository,
) : SherpaOnnxTtsEngine {

    private var tts: OfflineTts? = null
    private var loadedPaths: SupertonicModelPaths? = null
    private val ttsMutex = Mutex() // native object must not be released while generating otherwise crash
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val player = DesktopAudioPlayer()

    @Volatile private var lang = "en"
    @Volatile private var speakerId = SupertonicModel.speakerId(VoiceGender.MALE)

    private var paragraphs: List<Paragraph> = emptyList()

    // VoiceModel describes only piper voice, supertonic needs its language and gender
    override suspend fun loadVoice(model: VoiceModel) {
        lang = model.language
        speakerId = SupertonicModel.speakerId(model.gender)
        engineScope.launch { ttsMutex.withLock { ensureTts() } } // warm up, model load takes a while
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        this.paragraphs = paragraphs
    }

    override suspend fun speak(index: Int, speed: Float) {
        val text = paragraphs.getOrNull(index)?.text ?: return
        val audio = generate(text, speed)
        if (audio.isNotEmpty()) playAudio(audio)
    }

    override suspend fun generate(text: String, speed: Float): ByteArray = withContext(Dispatchers.IO) {
        try {
            val audio = ttsMutex.withLock {
                val engine = ensureTts() ?: return@withContext ByteArray(0)
                val config = GenerationConfig().apply {
                    sid = speakerId
                    setSpeed(speed)
                    numSteps = SupertonicModel.NUM_STEPS + 2 // pc has more resources, right???
                    extra = mapOf("lang" to lang)
                }
                // supertonic ignores the callback's return value, generation can't be aborted
                engine.generateWithConfigAndCallback(text, config, OfflineTtsCallback { 1 })
            }
            if (audio.samples.isEmpty()) ByteArray(0)
            else WavCodec.fromSamples(audio.samples, audio.sampleRate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Supertonic generate failed")
            ByteArray(0)
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
        loadedPaths = null
    }

    private fun ensureTts(): OfflineTts? {
        val paths = modelRepository.getModelPaths()
        if (paths != loadedPaths || tts == null) {
            tts?.release()
            tts = null
            loadedPaths = null
            if (paths == null) return null
            tts = buildOfflineTts(paths)
            loadedPaths = paths
        }
        return tts
    }

    private fun buildOfflineTts(paths: SupertonicModelPaths): OfflineTts {
        SherpaNativeLoader.ensureLoaded()
        val supertonic = OfflineTtsSupertonicModelConfig.builder()
            .setDurationPredictor(paths.durationPredictor)
            .setTextEncoder(paths.textEncoder)
            .setVectorEstimator(paths.vectorEstimator)
            .setVocoder(paths.vocoder)
            .setTtsJson(paths.ttsJson)
            .setUnicodeIndexer(paths.unicodeIndexer)
            .setVoiceStyle(paths.voiceStyle)
            .build()
        val model = OfflineTtsModelConfig.builder()
            .setSupertonic(supertonic)
            .setNumThreads(4)
            .setDebug(false)
            .setProvider("cpu")
            .build()
        CrashReporter.log("buildOfflineTts: supertonic", tag = "DesktopSupertonic")
        return OfflineTts(OfflineTtsConfig.builder().setModel(model).build())
    }
}