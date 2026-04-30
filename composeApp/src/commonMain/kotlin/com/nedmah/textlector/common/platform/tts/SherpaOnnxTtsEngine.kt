package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel

interface SherpaOnnxTtsEngine : TtsEngine {
    override suspend fun loadVoice(model: VoiceModel)
    suspend fun generate(text: String, speed: Float): ByteArray
    suspend fun playAudio(audio: ByteArray)
}