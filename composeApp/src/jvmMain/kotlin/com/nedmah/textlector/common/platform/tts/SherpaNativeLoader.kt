package com.nedmah.textlector.common.platform.tts

import com.k2fsa.sherpa.onnx.LibraryUtils

object SherpaNativeLoader {
    // jvm api doesnt load its native libraries by itself: extracts them
    // from the native-lib jar to a temp dir and loads
    // important: Must run before any OfflineTts
    private val loaded: Unit by lazy { LibraryUtils.load() }

    fun ensureLoaded() = loaded
}