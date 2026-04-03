package com.nedmah.textlector.common.platform.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorReadingOptions
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
                val nsUrl = NSURL.fileURLWithPath(uri)
                var result = ""

                NSFileCoordinator().coordinateReadingItemAtURL(
                    url = nsUrl,
                    options = 0u,
                    error = null
                ) { url ->
                    url?.let {
                        result = NSString.stringWithContentsOfURL(
                            url = it,
                            encoding = NSUTF8StringEncoding,
                            error = null
                        ) ?: ""
                    }
                }

                if (result.isBlank()) error("Cannot read file: $uri")
                result
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readPdf(uri: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nsUrl = NSURL.fileURLWithPath(uri)
                var result = ""


                NSFileCoordinator().coordinateReadingItemAtURL(
                    url = nsUrl,
                    options = 0u,
                    error = null
                ) { url ->
                    url?.let {
                        val pdfDoc = PDFDocument(uRL = it) ?: return@let
                        result = (0 until pdfDoc.pageCount.toInt())
                            .mapNotNull { i -> pdfDoc.pageAtIndex(i.toULong())?.string }
                            .joinToString("\n\n")
                    }
                }


                if (result.isBlank()) error("Cannot read PDF: $uri")
                result
            }
        }
}