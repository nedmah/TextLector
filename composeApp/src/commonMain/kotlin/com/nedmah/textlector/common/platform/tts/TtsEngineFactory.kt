package com.nedmah.textlector.common.platform.tts

expect class TtsEngineFactory {
    fun create() : TtsEngine
}