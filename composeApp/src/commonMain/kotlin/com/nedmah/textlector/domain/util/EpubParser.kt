package com.nedmah.textlector.domain.util

import com.fleeksoft.ksoup.Ksoup
import no.synth.kmpzip.zip.ZipInputStream

object EpubParser {

    data class EpubResult(
        val title: String,
        val text: String
    )

    fun parse(bytes: ByteArray): EpubResult {
        val files = readZip(bytes)

        val opfPath = findOpfPath(files)
        val opfContent = files[opfPath]
            ?: error("OPF file not found at: $opfPath")

        val title = parseTitle(opfContent)
        val chapterPaths = parseSpine(opfContent, opfPath)

        val text = chapterPaths
            .mapNotNull { files[it] }
            .joinToString("\n\n") { chapterBytes ->
                extractTextFromXhtml(chapterBytes)
            }
            .trim()

        if (text.isBlank()) error("EPUB has no readable content")

        return EpubResult(title = title, text = text)
    }


    /**
     * Read all files from ZIP into Map<path, bytes>.
     */
    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    result[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        return result
    }

    /**
     * META-INF/container.xml contains path to OPF:
     * <rootfile full-path="OEBPS/content.opf" .../>
     */
    private fun findOpfPath(files: Map<String, ByteArray>): String {
        val containerBytes = files["META-INF/container.xml"]
            ?: error("META-INF/container.xml not found — not a valid EPUB")

        val doc = Ksoup.parse(containerBytes.decodeToString())
        return doc.selectFirst("rootfile[full-path]")
            ?.attr("full-path")
            ?: error("Cannot find OPF path in container.xml")
    }

    /**
     * From OPF take:
     * - dc:title for title
     * - <spine> → order idref → <manifest> → href of the paragraphs files
     */
    private fun parseTitle(opfBytes: ByteArray): String =
        Ksoup.parse(opfBytes.decodeToString())
            .selectFirst("metadata title, dc|title")
            ?.text()
            ?.trim()
            ?: "Untitled"

    private fun parseSpine(opfBytes: ByteArray, opfPath: String): List<String> {
        val doc = Ksoup.parse(opfBytes.decodeToString())

        // manifest: id -> href
        val manifest = doc.select("manifest item")
            .associate { it.attr("id") to it.attr("href") }

        // spine creates order through idref
        val orderedHrefs = doc.select("spine itemref")
            .mapNotNull { manifest[it.attr("idref")] }

        // OPF can be lay in subfolder (OEBPS/), href related to it
        val opfDir = opfPath.substringBeforeLast("/", "")

        return orderedHrefs.map { href ->
            if (opfDir.isEmpty()) href else "$opfDir/$href"
        }
    }

    /**
     * Take text from all the <p> tags in XHTML title.
     * Ksoup strip tags, decode HTML entities automatically.
     */
    private fun extractTextFromXhtml(bytes: ByteArray): String =
        Ksoup.parse(bytes.decodeToString())
            .select("p")
            .joinToString("\n\n") { it.text().trim() }
            .trim()
}