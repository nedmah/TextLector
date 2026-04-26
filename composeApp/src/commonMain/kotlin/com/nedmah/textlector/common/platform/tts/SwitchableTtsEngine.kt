package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class SwitchableTtsEngine(
    private val nativeEngine: TtsEngine,
    private val sherpaEngine: TtsEngine,
    private val preferencesRepository: PreferencesRepository
) : TtsEngine {

    private val _engineChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val engineChanged: Flow<Unit> = _engineChanged

    @Volatile
    private var active: TtsEngine = nativeEngine
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        scope.launch(Dispatchers.IO) {
            preferencesRepository.getPreferences().collect { prefs ->
                if (prefs.useSherpaEngine && active !== sherpaEngine) {
                    active.stop()
                    active = sherpaEngine
                    val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                    sherpaEngine.loadVoice(model)
                    CrashReporter.log("Engine switched to Piper", tag = "SwitchableTtsEngine")
                    _engineChanged.tryEmit(Unit)
                } else if (!prefs.useSherpaEngine && active !== nativeEngine) {
                    active.stop()
                    active = nativeEngine
                    CrashReporter.log("Engine switched to Native", tag = "SwitchableTtsEngine")
                    _engineChanged.tryEmit(Unit)
                } else if (prefs.useSherpaEngine && active === sherpaEngine) {
                    val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                    sherpaEngine.loadVoice(model)
                }
            }
        }
    }

    override suspend fun speak(text: String, speed: Float) =
        active.speak(text, speed)

    override suspend fun loadVoice(model: VoiceModel) =
        active.loadVoice(model)

    override fun stop() = active.stop()

    override fun shutdown() {
        active.stop()
        scope.coroutineContext[Job]?.cancel()
        nativeEngine.shutdown()
        sherpaEngine.shutdown()
    }

    override fun piperEngine(): PiperTtsEngine? = active as? PiperTtsEngine

}