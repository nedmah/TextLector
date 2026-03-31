package com.nedmah.textlector.common.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import okio.buffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

actual class FileReader {
    actual suspend fun readText(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                okio.FileSystem.SYSTEM
                    .source(uri.toPath())
                    .buffer()
                    .use { it.readUtf8() }
            }
        }

    actual suspend fun readPdf(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val document = PDDocument.load(File(uri))
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        }
}