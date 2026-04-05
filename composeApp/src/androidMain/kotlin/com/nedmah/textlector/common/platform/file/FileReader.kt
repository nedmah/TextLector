package com.nedmah.textlector.common.platform.file

import android.content.Context
import androidx.core.net.toUri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    actual suspend fun readPdf(uri: String, onProgress: (Int, Int) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                PDFBoxResourceLoader.init(context)
                val inputStream = context.contentResolver
                    .openInputStream(uri.toUri())
                    ?: error("Cannot open stream for uri : $uri")

                val document = PDDocument.load(inputStream)
                val totalPages = document.numberOfPages
                val stripper = PDFTextStripper()
                val sb = StringBuilder()

                for (page in 1..totalPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    sb.append(stripper.getText(document))
                    onProgress(page, totalPages)
                }

                document.close()
                sb.toString()
            }
        }
}