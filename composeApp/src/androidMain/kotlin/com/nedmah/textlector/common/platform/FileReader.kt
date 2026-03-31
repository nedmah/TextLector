package com.nedmah.textlector.common.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

actual class FileReader(private val context: Context) {

    actual suspend fun readText(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver
                    .openInputStream(uri.toUri())
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("Cannot open stream for uri: $uri")
            }
        }

    actual suspend fun readPdf(uri: String): Result<String> =
        withContext(Dispatchers.IO){
            runCatching {
                PDFBoxResourceLoader.init(context)
                val inputStream = context.contentResolver
                    .openInputStream(uri.toUri())
                    ?: error("Cannot open stream for uri : $uri")

                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        }
}