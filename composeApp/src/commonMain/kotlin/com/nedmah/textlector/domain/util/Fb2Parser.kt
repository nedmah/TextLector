package com.nedmah.textlector.domain.util

import com.fleeksoft.ksoup.Ksoup
import com.nedmah.textlector.common.platform.file.decodeWithCharset
import no.synth.kmpzip.zip.ZipInputStream

object Fb2Parser {

    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    data class Fb2Result(val title: String, val text: String)

    fun parse(bytes: ByteArray): Fb2Result {
        val xmlBytes = if (bytes.isZip()) unzipFb2(bytes) else bytes
        val charset = detectCharset(xmlBytes)
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

    private fun ByteArray.isZip(): Boolean =
        size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { this[it] == ZIP_MAGIC[it] }

    // .fb2.zip is an archive with a single .fb2 inside
    private fun unzipFb2(bytes: ByteArray): ByteArray {
        ZipInputStream(bytes).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".fb2", ignoreCase = true)) {
                    return zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        error("No .fb2 file inside the archive")
    }
}