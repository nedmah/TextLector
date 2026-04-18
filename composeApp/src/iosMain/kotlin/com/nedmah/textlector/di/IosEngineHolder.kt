package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileDownloader
import com.nedmah.textlector.common.platform.file.PdfPageExtractor
import com.nedmah.textlector.common.platform.file.TarExtractor
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.domain.repository.VoiceModelRepository

object IosEngineHolder {
    var ttsEngine : TtsEngine? = null
    var tarExtractor: TarExtractor? = null
    var fileDownloader : FileDownloader? = null
    var pdfExtractor: PdfPageExtractor? = null
}