package com.nedmah.textlector.domain.util

import com.fleeksoft.ksoup.Ksoup
import com.nedmah.textlector.common.platform.file.decodeWithCharset

object Fb2Parser {


    data class Fb2Result(val title: String, val text: String)

    fun parse(bytes: ByteArray): Fb2Result {
        val charset = detectCharset(bytes)
        val xmlString = bytes.decodeWithCharset(charset)

        val doc = Ksoup.parse(xmlString)

        val title = doc.selectFirst("description title-info book-title")
            ?.text()
            ?.trim()
            ?: "Untitled"

        val text = doc.select("body p")
            .joinToString("\n\n") { it.text().trim() }
            .trim()

        if (text.isBlank()) error("FB2 has no readable content")

        return Fb2Result(title = title, text = text)
    }

    private fun detectCharset(bytes: ByteArray): String {
        val header = bytes.take(200)
            .map { it.toInt().and(0xFF).toChar() }
            .joinToString("")

        val match = Regex("""encoding=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(header)

        return match?.groupValues?.get(1) ?: "UTF-8"
    }
}