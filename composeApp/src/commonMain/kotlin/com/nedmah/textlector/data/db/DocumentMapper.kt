package com.nedmah.textlector.data.db

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.SourceType

fun com.nedmah.textlector.db.Document.toDomain(): Document =
    Document(
        id = id,
        title = title,
        sourceType = source_type.toSourceType(),
        createdAt = created_at,
        lastOpenedAt = last_opened_at,
        isFavorite = is_favorite == 1L,
        wordCount = word_count.toInt(),
        estimatedReadingMinutes = estimated_reading_minutes.toInt(),
        totalParagraphs = total_paragraphs.toInt(),
        lastParagraphIndex = last_paragraph_index.toInt()
    )

fun SourceType.toDbString(): String = when (this) {
    is SourceType.Manual -> "MANUAL"
    is SourceType.Txt -> "TXT"
    is SourceType.Pdf -> "PDF"
    is SourceType.Url -> "URL:${this.url}"
    is SourceType.Docx -> "DOCX"
    is SourceType.Camera -> "CAMERA"
    SourceType.Epub -> "EPUB"
    SourceType.Fb2 -> "FB2"
}

fun String.toSourceType(): SourceType = when {
    this == "MANUAL" -> SourceType.Manual
    this == "TXT" -> SourceType.Txt
    this == "PDF" -> SourceType.Pdf
    this.startsWith("URL:") -> SourceType.Url(this.removePrefix("URL:"))
    this == "DOCX" -> SourceType.Docx
    this == "CAMERA" -> SourceType.Camera
    this == "EPUB" -> SourceType.Epub
    this == "FB2" -> SourceType.Fb2
    else -> throw IllegalArgumentException("Unknown source type: $this")
}