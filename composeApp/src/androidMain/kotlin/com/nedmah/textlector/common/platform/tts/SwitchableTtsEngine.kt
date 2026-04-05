package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel
import com.nedmah.textlector.domain.model.VoiceRegistry
import com.nedmah.textlector.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SwitchableTtsEngine(
    private val nativeEngine: AndroidTtsEngine,
    private val sherpaEngine: AndroidSherpaOnnxTtsEngine,
    private val preferencesRepository: PreferencesRepository
) : TtsEngine {

    private var active: TtsEngine = nativeEngine
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        scope.launch {
            preferencesRepository.getPreferences().collect { prefs ->
                if (prefs.useSherpaEngine && active !== sherpaEngine) {
                    active.stop()
                    active = sherpaEngine
                    val model = VoiceRegistry.getById(prefs.resolveVoiceId())
                    sherpaEngine.loadVoice(model)
                } else if (!prefs.useSherpaEngine && active !== nativeEngine) {
                    active.stop()
                    active = nativeEngine
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
        nativeEngine.shutdown()
        sherpaEngine.shutdown()
    }
}