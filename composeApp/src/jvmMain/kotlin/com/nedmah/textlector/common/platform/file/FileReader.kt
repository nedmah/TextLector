package com.nedmah.textlector.common.platform.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

actual class FileReader {

    actual suspend fun readPdf(
        uri: String,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<String> {
        TODO("Not yet implemented")
    }

    actual suspend fun readBytes(uri: String): Result<ByteArray> {
        TODO("Not yet implemented")
    }
}