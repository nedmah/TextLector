package com.nedmah.textlector.common.platform.logging

import com.nedmah.textlector.common.platform.file.AppDirs
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

actual object CrashReporter {
    private const val MAX_LOG_BYTES = 1_000_000L
    private val logFile = File(AppDirs.dataDir, "textlector.log")
    private val previousLogFile = File(AppDirs.dataDir, "textlector.log.1")
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val keys = ConcurrentHashMap<String, String>()

    actual fun log(message: String, tag: String) = write("I/$tag: $message")

    actual fun recordException(e: Throwable, message: String) {
        val context = if (keys.isEmpty()) "" else " $keys"
        write("E: $message$context\n${e.stackTraceToString()}")
    }

    actual fun setKey(key: String, value: String) {
        keys[key] = value
    }

    @Synchronized
    private fun write(entry: String) {
        val line = "${LocalDateTime.now().format(timeFormat)} $entry"
        System.err.println(line)
        runCatching {
            if (logFile.length() > MAX_LOG_BYTES) {
                previousLogFile.delete()
                logFile.renameTo(previousLogFile)
            }
            logFile.appendText(line + "\n")
        }
    }
}