package com.nedmah.textlector.domain.usecase

import com.benasher44.uuid.uuid4
import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.ProcessedDocument
import com.nedmah.textlector.domain.model.SourceType
import com.nedmah.textlector.domain.repository.DocumentRepository
import com.nedmah.textlector.domain.repository.ParagraphRepository
import com.nedmah.textlector.domain.util.TextSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InputTextManuallyUseCase(
) {

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(title: String, text: String): Result<ProcessedDocument> =
        withContext(Dispatchers.IO){
            runCatching {
                val segments = TextSegmenter.segment(text)
                val paragraphs = segments
                    .mapIndexed { index, paragraphText ->
                        Paragraph(
                            id = uuid4().toString(),
                            documentId = "",
                            index = index,
                            text = paragraphText.trim()
                        )
                    }

                val document = Document(
                    id = uuid4().toString(),
                    title = title,
                    sourceType = SourceType.Manual,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastOpenedAt = Clock.System.now().toEpochMilliseconds(),
                    isFavorite = false,
                    wordCount = text.split(Regex("\\s+")).size,
                    estimatedReadingMinutes = text.split(Regex("\\s+")).size / 200,
                    totalParagraphs = paragraphs.size,
                    lastParagraphIndex = 0
                )
                ProcessedDocument(document, paragraphs)
            }
        }
}