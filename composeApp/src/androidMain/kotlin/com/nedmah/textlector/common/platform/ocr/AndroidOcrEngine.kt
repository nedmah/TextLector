package com.nedmah.textlector.common.platform.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.googlecode.tesseract.android.TessBaseAPI
import com.nedmah.textlector.domain.repository.OcrDataRepository

class AndroidOcrEngine(
    private val context : Context,
    private val ocrDataRepository: OcrDataRepository
) : OcrEngine {

    private var tessApi: TessBaseAPI? = null
    private val tessDataPath: String
        get() = context.filesDir.absolutePath // tessdata/ in there

    override suspend fun recognize(imageUri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!ocrDataRepository.isReady()) error("OCR data not downloaded")

                val api = getOrCreateApi()
                val bitmap = loadBitmap(imageUri)

                api.setImage(bitmap)
                val result = api.utF8Text?.trim() ?: ""
                api.clear()

                if (result.isBlank()) error("No text detected in image")
                result
            }
        }

    private fun getOrCreateApi(): TessBaseAPI {
        return tessApi ?: TessBaseAPI().also { api ->
            val success = api.init(tessDataPath, "rus+eng")
            if (!success) error("Tesseract init failed")
            tessApi = api
        }
    }

    private fun loadBitmap(imageUri: String): Bitmap {
        val uri = imageUri.toUri()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    fun release() {
        tessApi?.recycle()
        tessApi = null
    }
}