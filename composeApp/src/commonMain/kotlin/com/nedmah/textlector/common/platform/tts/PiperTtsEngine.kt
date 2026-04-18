package com.nedmah.textlector.common.platform.tts

interface PiperTtsEngine : TtsEngine {
    suspend fun generate(text: String, speed: Float): ByteArray
    suspend fun playAudio(audio: ByteArray)
}