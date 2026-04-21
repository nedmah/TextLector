package com.nedmah.textlector.common.platform.file

import com.nedmah.textlector.di.IosEngineHolder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.getBytes
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
    actual suspend fun readPdf(uri: String, onProgress: (Int, Int) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val extractor = IosEngineHolder.pdfExtractor
                    ?: error("PdfTextExtractor not initialized")

                val total = extractor.pageCount(uri)
                if (total == 0) error("Cannot read PDF: $uri")

                val sb = StringBuilder()

                for (i in 0 until total) {
                    val pageText = extractor.extractPage(uri, i)

                    val finalText = if (looksGarbled(pageText))
                        extractor.ocrPage(uri, i)
                    else
                        pageText

                    sb.append(finalText)
                    sb.append("\n\n")
                    onProgress(i + 1, total)
                }

                val result = sb.toString()
                if (result.isBlank()) error("Cannot read PDF: $uri")
                result
            }
        }

    actual suspend fun readBytes(uri: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nsUrl = NSURL.fileURLWithPath(uri)
                val data = NSData.dataWithContentsOfURL(nsUrl)
                    ?: error("Cannot read file: $uri")
                data.toByteArray()
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray =
        ByteArray(length.toInt()).also { bytes ->
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
            }
        }

    /**
     * counts single letters percentage
     */
    private fun looksGarbled(text: String): Boolean {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 15) return false  // too few words
        val singleLetterWords = words.count { it.length == 1 && it[0].isLetter() }
        return singleLetterWords.toFloat() / words.size > 0.15f
    }
}