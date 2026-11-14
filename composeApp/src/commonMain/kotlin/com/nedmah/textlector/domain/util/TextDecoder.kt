package com.nedmah.textlector.domain.util

import com.nedmah.textlector.common.platform.file.decodeWithCharset

object TextDecoder {
    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    // valid UTF-8 is never a misread windows-1251 file, so strict UTF-8 goes first
    // old Russian txt files are usually windows-1251.
    fun decode(bytes: ByteArray): String {
        val content = if (bytes.startsWith(UTF8_BOM)) bytes.copyOfRange(UTF8_BOM.size, bytes.size) else bytes
        return try {
            content.decodeToString(throwOnInvalidSequence = true)
        } catch (e: CharacterCodingException) {
            content.decodeWithCharset("windows-1251")
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

}