package com.nedmah.textlector.common.platform.tts

interface TtsEngine {
    fun speak(text : String, speed: Float, onDone : () -> Unit)
    fun stop()
    fun shutdown()
}