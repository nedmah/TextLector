package com.nedmah.textlector.common.platform.tts

import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.SupertonicModel
import com.nedmah.textlector.domain.model.SupertonicModelPaths
import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.repository.SupertonicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class AndroidSupertonicTtsEngine(
    private val modelRepository: SupertonicRepository
) : SherpaOnnxTtsEngine {

    private var tts: OfflineTts? = null
    private var loadedPaths: SupertonicModelPaths? = null
    private val ttsMutex = Mutex() // guards tts: native object must not be released while generating
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var lang = "en"
    @Volatile
    private var speakerId = SupertonicModel.speakerId(VoiceGender.MALE)

    private var audioTrack: AudioTrack? = null
    private var paragraphs: List<Paragraph> = emptyList()

    // VoiceModel describes a Piper voice; Supertonic only needs its language and gender
    override suspend fun loadVoice(model: VoiceModel) {
        lang = model.language
        speakerId = SupertonicModel.speakerId(model.gender)
        engineScope.launch { ttsMutex.withLock { ensureTts() } } // warm up, model load takes a while
    }

    override suspend fun generate(text: String, speed: Float): ByteArray = withContext(Dispatchers.IO) {
        try {
            val audio = ttsMutex.withLock {
                val engine = ensureTts() ?: return@withContext ByteArray(0)
                val config = GenerationConfig(
                    sid = speakerId,
                    speed = speed,
                    numSteps = SupertonicModel.NUM_STEPS,
                    extra = mapOf("lang" to lang),
                )
                engine.generateWithConfig(text, config)
            }
            if (audio.samples.isEmpty()) ByteArray(0)
            else AndroidTrackPlayer.samplesToWav(audio.samples, audio.sampleRate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Supertonic generate failed")
            ByteArray(0)
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

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        this.paragraphs = paragraphs
    }

    override suspend fun speak(index: Int, speed: Float) {
        val text = paragraphs.getOrNull(index)?.text ?: return
        val audio = generate(text, speed)
        if (audio.isNotEmpty()) playAudio(audio)
    }

    override fun stop() {
        AndroidTrackPlayer.stop(audioTrack)
    }

    override fun shutdown() {
        engineScope.coroutineContext[Job]?.cancel()
        audioTrack?.release()
        audioTrack = null
        tts?.release()
        tts = null
        loadedPaths = null
    }

    // must be called under ttsMutex. Builds the model lazily and drops it if files were deleted.
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
        val supertonicConfig = OfflineTtsSupertonicModelConfig(
            durationPredictor = paths.durationPredictor,
            textEncoder = paths.textEncoder,
            vectorEstimator = paths.vectorEstimator,
            vocoder = paths.vocoder,
            ttsJson = paths.ttsJson,
            unicodeIndexer = paths.unicodeIndexer,
            voiceStyle = paths.voiceStyle,
        )
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                supertonic = supertonicConfig,
                numThreads = 4,
                debug = false,
                provider = "cpu",
            ),
        )
        CrashReporter.log("buildOfflineTts: supertonic", tag = "SupertonicEngine")
        return try {
            OfflineTts(config = config)
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Supertonic buildOfflineTts failed")
            throw e
        }
    }

}