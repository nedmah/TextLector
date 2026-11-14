package com.nedmah.textlector.data.download

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HttpFileDownloader {

    private const val BUFFER_SIZE = 64 * 1024
    private const val TIMEOUT_MS = 30_000

    // writes to a temp file first, so dest only ever holds a complete download.
    suspend fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        val tempFile = File(dest.parentFile, "${dest.name}.tmp")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.connect()

            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }

            if (!tempFile.renameTo(dest)) throw IOException("Failed to move ${tempFile.name} to ${dest.name}")
        } catch (e: Throwable) {
            tempFile.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }
}