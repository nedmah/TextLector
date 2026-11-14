package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.TtsEngineType
import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

private const val ENGINE_LOGS = true
private fun log(message: String) {
    if (ENGINE_LOGS) println("[SwitchableTtsEngine] $message")
}

/**
 * Single entry point for TTS in the app.
 *
 * Switches between three engines depending on [UserPreferences.engineType]:
 * - [TtsEngineType.SYSTEM] — Android system TTS, no buffering
 * - [TtsEngineType.PIPER] — offline VITS via sherpa-onnx, with [TtsQueue]
 * - [TtsEngineType.SUPERTONIC] — offline neural TTS via sherpa-onnx (Supertonic 3), with [TtsQueue]
 *
 * Both ONNX engines implement [SherpaOnnxTtsEngine] and work through the same
 * [TtsQueue] — prefetches the next paragraph while the current one is playing.
 *
 * Switching occurs reactively by subscribing to [PreferencesRepository.getPreferences].
 * When the engine changes, [engineChanged] is emitted — [com.nedmah.textlector.ui.presentation.player.PlayerViewModel] restarts playback.
 */
class SwitchableTtsEngine(
    private val nativeEngine: TtsEngine,
    private val sherpaEngine: SherpaOnnxTtsEngine,
    private val supertonicEngine: SherpaOnnxTtsEngine,
    private val preferencesRepository: PreferencesRepository
) : TtsEngine {

    private val _engineChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val engineChanged: Flow<Unit> = _engineChanged

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: Flow<Boolean> = _isBuffering.asStateFlow()

    @Volatile
    private var active: TtsEngine = nativeEngine
    private var ttsQueue: TtsQueue? = null
    private var paragraphs: List<Paragraph> = emptyList()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {

        scope.launch(Dispatchers.IO) {
            preferencesRepository.getPreferences().collect { prefs ->
                handleEngineSwitch(prefs)
            }
        }
    }

    /**
     * Delegates only to the active engine if it is a [SherpaOnnxTtsEngine].
     * Called from [com.nedmah.textlector.ui.presentation.settings.SettingsViewModel] after the model has loaded.
     */
    override suspend fun loadVoice(model: VoiceModel) {
        log("loadVoice: ${model.id}")
        (active as? SherpaOnnxTtsEngine)?.loadVoice(model)
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        log("setPlaylist: ${paragraphs.size} paragraphs")
        this.paragraphs = paragraphs
        nativeEngine.setPlaylist(paragraphs)
        sherpaEngine.setPlaylist(paragraphs)
        supertonicEngine.setPlaylist(paragraphs)
    }

    /**
     * Plays paragraph [index].
     *
     * If the ONNX engine is active, it takes audio from the [TtsQueue] (cache or generated),
     * then runs a prefetch of the next paragraph in the background.
     * If the SYSTEM engine is active, it delegates directly without buffering.
     */
    override suspend fun speak(index: Int, speed: Float) {
        val queue = ttsQueue
        log("speak: index=$index, speed=$speed, queue=${when {
            queue != null && active === supertonicEngine -> "Supertonic"
            queue != null -> "Piper"
            else -> "Native"
        }}, paragraphs=${paragraphs.size}")

        if (queue != null) {
            if (index >= paragraphs.size) {
                log("speak: index out of bounds! index=$index, size=${paragraphs.size}")
                return
            }

            val cached = queue.getCachedAudio(index)
            log("speak: cache ${if (cached != null) "HIT" else "MISS"} for index=$index")
            if (cached == null) _isBuffering.emit(true)

            val audio = try {
                queue.getAudio(index, paragraphs[index].text, speed)
            } catch (e: Exception) {
                log("speak: getAudio failed — ${e.message}")
                withContext(NonCancellable) { _isBuffering.emit(false) }
                throw e
            } finally {
                withContext(NonCancellable) { _isBuffering.emit(false) }
            }

            log("speak: audio ready, size=${audio.size}b, starting prefetch and playback")
            queue.prefetchAhead(index, paragraphs, speed)
            (active as SherpaOnnxTtsEngine).playAudio(audio)
            log("speak: playAudio finished for index=$index")
        } else {
            log("speak: native path for index=$index")
            nativeEngine.speak(index, speed)
            log("speak: native finished for index=$index")
        }
    }

    override fun stop() {
        log("stop() called, active=${when (active) {
            sherpaEngine -> "Piper"
            supertonicEngine -> "Supertonic"
            else -> "Native"
        }}")
        ttsQueue?.clear()
        active.stop()
        // don't emit buffering false here because it interrupts, pause() in playerVM already cancels loading
    }

    override fun shutdown() {
        log("shutdown()")
        active.stop()
        scope.coroutineContext[Job]?.cancel()
        nativeEngine.shutdown()
        sherpaEngine.shutdown()
        supertonicEngine.shutdown()
    }

    private suspend fun handleEngineSwitch(prefs : UserPreferences) {
        val targetEngine = when(prefs.engineType){
            TtsEngineType.SYSTEM -> nativeEngine
            TtsEngineType.PIPER -> sherpaEngine
            TtsEngineType.SUPERTONIC -> supertonicEngine
        }

        // if voice or language was changed
        if (active === targetEngine) {
            if (targetEngine is SherpaOnnxTtsEngine) {  // supertonic implements SherpaOnnxTtsEngine too
                val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                targetEngine.loadVoice(model)
                if (ttsQueue == null) ttsQueue = TtsQueue(targetEngine)
            }
            return
        }

        log("switching to ${prefs.engineType}...")
        active.stop()
        active = targetEngine

        if (targetEngine is SherpaOnnxTtsEngine) {
            val model = VoiceRegistry.getById(prefs.resolveVoiceId())
            targetEngine.loadVoice(model)
            ttsQueue?.shutdown()
            ttsQueue = TtsQueue(targetEngine)
        } else {
            ttsQueue?.shutdown()
            ttsQueue = null
        }

        CrashReporter.log("Engine switched to ${prefs.engineType}", tag = "SwitchableTtsEngine")
        _engineChanged.tryEmit(Unit)
    }
}