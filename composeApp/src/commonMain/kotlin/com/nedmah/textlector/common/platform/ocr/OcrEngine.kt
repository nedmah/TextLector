package com.nedmah.textlector.common.platform.ocr

interface OcrEngine {
    suspend fun recognize(imageUri : String) : String
}