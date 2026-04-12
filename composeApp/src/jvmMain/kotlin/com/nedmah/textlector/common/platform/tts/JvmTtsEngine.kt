package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel

// soon..
class JvmTtsEngine : TtsEngine{

    override suspend fun speak(text: String, speed: Float) {
        // stub
    }

    override suspend fun loadVoice(model: VoiceModel) = Unit

    override fun stop() {
    }

    override fun shutdown() {
    }

}