package com.nedmah.textlector.common.platform.tts

actual class TtsEngineFactory {
    actual fun create(): TtsEngine = IosTtsEngine()
}