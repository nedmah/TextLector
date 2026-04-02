package com.nedmah.textlector.common.platform.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.PDFKit.PDFDocument

actual class FileReader {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readText(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nsUrl = NSURL.Companion.fileURLWithPath(uri)
                NSString.Companion.stringWithContentsOfURL(
                    url = nsUrl,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) ?: error("Cannot read file at: $uri")
            }
        }

    actual suspend fun readPdf(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nsUrl = NSURL.Companion.fileURLWithPath(uri)
                val pdfDoc = PDFDocument(uRL = nsUrl)

                (0 until pdfDoc.pageCount.toInt()).mapNotNull { index ->
                    pdfDoc.pageAtIndex(index.toULong())?.string
                }.joinToString("\n\n")
            }
        }
}