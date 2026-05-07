package com.nedmah.textlector.common.platform.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.googlecode.tesseract.android.TessBaseAPI
import com.nedmah.textlector.domain.repository.OcrDataRepository
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.nedmah.textlector.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

class AndroidOcrEngine(
    private val context : Context,
    private val ocrDataRepository: OcrDataRepository,
    private val preferencesRepository: PreferencesRepository
) : OcrEngine {

    private var tessApi: TessBaseAPI? = null
    private var currentLang: String? = null

    private val tessDataPath: String
        get() = context.filesDir.absolutePath // tessdata/ in there

    override suspend fun recognize(imageUri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!ocrDataRepository.isReady()) error("OCR data not downloaded")

                val api = getOrCreateApi()
                val bitmap = loadBitmap(imageUri)
                val processed = preprocessBitmap(bitmap)

                api.setImage(processed)
                val result = api.utF8Text?.trim() ?: ""
                Log.d("OCR", "result: ${result.take(300)}")
                api.clear()

                if (result.isBlank()) error("No text detected in image")
                result
            }
        }

    fun release() {
        tessApi?.recycle()
        tessApi = null
    }

    private suspend fun getOrCreateApi(): TessBaseAPI {
        val prefs = preferencesRepository.getPreferences().first()
        val lang = if (prefs.language == "ru") "rus" else "eng"

        if (lang != currentLang) {
            tessApi?.recycle()
            tessApi = null
        }

        return tessApi ?: TessBaseAPI().also { api ->
            val success = api.init(tessDataPath, lang)
            if (!success) error("Tesseract init failed")
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
            tessApi = api
            currentLang = lang
        }
    }

    private fun loadBitmap(imageUri: String): Bitmap {
        val uri = imageUri.toUri()
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        return if (raw.config == Bitmap.Config.ARGB_8888) raw
        else raw.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // scaling
        val maxWidth = 1500
        val scaled = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            bitmap.scale(maxWidth, (bitmap.height * ratio).toInt())
        } else bitmap

        // grayscale
        val grayscale = createBitmap(scaled.width, scaled.height)
        val canvas = Canvas(grayscale)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        // contrast
        val contrast = createBitmap(grayscale.width, grayscale.height)
        val contrastCanvas = Canvas(contrast)
        val scale = 1.5f
        val translate = (-.5f * scale + .5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        contrastCanvas.drawBitmap(grayscale, 0f, 0f, Paint().apply {
            colorFilter = ColorMatrixColorFilter(contrastMatrix)
        })

        return contrast
    }

}