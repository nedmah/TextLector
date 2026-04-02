package com.nedmah.textlector.common.platform.tts

import android.content.Context

actual class TtsEngineFactory(private val context: Context) {
    actual fun create(): TtsEngine = AndroidTtsEngine(context)
}