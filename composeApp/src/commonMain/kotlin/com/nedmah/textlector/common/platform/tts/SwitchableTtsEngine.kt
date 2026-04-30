package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.Paragraph
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

class SwitchableTtsEngine(
    private val nativeEngine: TtsEngine,
    private val sherpaEngine: SherpaOnnxTtsEngine,
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
                log("preferences received: useSherpa=${prefs.useSherpaEngine}, voice=${prefs.resolveVoiceId()}")
                if (prefs.useSherpaEngine && active !== sherpaEngine) {
                    log("switching to Piper...")
                    active.stop()
                    active = sherpaEngine
                    val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                    log("loading voice: ${model.id}")
                    sherpaEngine.loadVoice(model)
                    ttsQueue = TtsQueue(sherpaEngine)
                    log("TtsQueue created")
                    CrashReporter.log("Engine switched to Piper", tag = "SwitchableTtsEngine")
                    _engineChanged.tryEmit(Unit)
                } else if (!prefs.useSherpaEngine && active !== nativeEngine) {
                    log("switching to Native...")
                    active.stop()
                    active = nativeEngine
                    ttsQueue?.shutdown()
                    ttsQueue = null
                    CrashReporter.log("Engine switched to Native", tag = "SwitchableTtsEngine")
                    _engineChanged.tryEmit(Unit)
                } else if (prefs.useSherpaEngine && active === sherpaEngine) {
                    val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                    log("same engine (Piper), reloading voice: ${model.id}")
                    sherpaEngine.loadVoice(model)
                    if (ttsQueue == null) ttsQueue = TtsQueue(sherpaEngine)
                }
            }
        }
    }

    override suspend fun loadVoice(model: VoiceModel) {
        log("loadVoice: ${model.id}")
        sherpaEngine.loadVoice(model)
    }

    override fun setPlaylist(paragraphs: List<Paragraph>) {
        log("setPlaylist: ${paragraphs.size} paragraphs")
        this.paragraphs = paragraphs
        nativeEngine.setPlaylist(paragraphs)
        sherpaEngine.setPlaylist(paragraphs)
    }

    override suspend fun speak(index: Int, speed: Float) {
        val queue = ttsQueue
        log("speak: index=$index, speed=$speed, queue=${if (queue != null) "Piper" else "Native"}, paragraphs=${paragraphs.size}")

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
            sherpaEngine.playAudio(audio)
            log("speak: playAudio finished for index=$index")
        } else {
            log("speak: native path for index=$index")
            nativeEngine.speak(index, speed)
            log("speak: native finished for index=$index")
        }
    }

    override fun stop() {
        log("stop() called, active=${if (active === sherpaEngine) "Piper" else "Native"}")
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
    }

}