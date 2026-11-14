package com.nedmah.textlector.di

import com.nedmah.textlector.common.platform.file.FileDownloader
import com.nedmah.textlector.common.platform.file.PdfPageExtractor
import com.nedmah.textlector.common.platform.file.TarExtractor
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.common.platform.tts.SherpaOnnxTtsEngine
import com.nedmah.textlector.domain.repository.SupertonicRepository

object IosEngineHolder {
    var sherpaEngine: SherpaOnnxTtsEngine? = null
    var supertonicEngine: SherpaOnnxTtsEngine? = null
    var supertonicRepository: SupertonicRepository? = null
    var tarExtractor: TarExtractor? = null
    var fileDownloader : FileDownloader? = null
    var pdfExtractor: PdfPageExtractor? = null
    var ocrEngine: OcrEngine? = null
}