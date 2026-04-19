package com.nedmah.textlector.data.source

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest

class UrlContentFetcher {

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

        val text = contentElement?.wholeText() ?: doc.body().wholeText()

        if (text.isBlank()) error("No readable content found at $url")

        title to text
    }

    private fun String.toShortTitle(): String =
        removePrefix("https://").removePrefix("http://")
            .substringBefore("/")
            .take(50)
}