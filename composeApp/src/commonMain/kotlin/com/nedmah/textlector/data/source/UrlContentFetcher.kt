package com.nedmah.textlector.data.source

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest

class UrlContentFetcher {

    private companion object {
        val BLOCK_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "blockquote", "pre")
        val BLOCK_SELECTOR = BLOCK_TAGS.joinToString(", ")
    }

    suspend fun fetchText(url : String) : Result<Pair<String, String>> = runCatching {
        val doc = Ksoup.parseGetRequest(url)

        val title = doc.title().ifBlank {
            doc.selectFirst("h1")?.text() ?: url.toShortTitle()
        }

        doc.select(
            "script, style, nav, header, footer, " +
                    "iframe, noscript, aside, .ad, .ads, " +
                    ".advertisement, .cookie, .popup, .banner, .sidebar"
        ).remove()

        val contentElement =
            doc.selectFirst("article") ?:
            doc.selectFirst("main") ?:
            doc.selectFirst("[role=main]") ?:
            doc.selectFirst(".content, .post, .entry, .article-body") ?:
            doc.selectFirst("body")

        val blocks = contentElement
            ?.select(BLOCK_SELECTOR)
            ?.filter { el -> el.parents().none { it.normalName() in BLOCK_TAGS } }
            ?.map { it.text().trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val blockText = blocks.joinToString("\n\n")
        val wholeText = (contentElement?.wholeText() ?: doc.body().wholeText()).trim()

        val text = if (blockText.length >= wholeText.length / 2) blockText else wholeText

        if (text.isBlank()) error("No readable content found at $url")

        title to text
    }

    private fun String.toShortTitle(): String =
        removePrefix("https://").removePrefix("http://")
            .substringBefore("/")
            .take(50)
}