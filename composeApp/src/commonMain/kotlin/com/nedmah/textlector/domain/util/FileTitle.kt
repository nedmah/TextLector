package com.nedmah.textlector.domain.util

object FileTitle {

    fun fromFileName(fileName: String): String =
        fileName
            .removeSuffix(".zip")
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifEmpty { fileName }
}