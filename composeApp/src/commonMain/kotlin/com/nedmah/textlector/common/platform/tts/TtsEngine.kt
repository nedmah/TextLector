package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel

interface TtsEngine {
    suspend fun speak(text : String, speed: Float)
    suspend fun loadVoice(model: VoiceModel)
    fun stop()
    fun shutdown()
    fun piperEngine(): PiperTtsEngine? = null
}