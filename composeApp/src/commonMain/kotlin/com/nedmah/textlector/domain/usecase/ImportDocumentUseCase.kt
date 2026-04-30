package com.nedmah.textlector.domain.usecase

import com.benasher44.uuid.uuid4
import com.nedmah.textlector.common.platform.file.FileReader
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.ImportProgress
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.model.ProcessedDocument
import com.nedmah.textlector.domain.model.SourceType
import com.nedmah.textlector.domain.util.EpubParser
import com.nedmah.textlector.domain.util.Fb2Parser
import com.nedmah.textlector.domain.util.TextSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ImportDocumentUseCase(
    private val fileReader: FileReader,
) {

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        uri: String,
        title: String,
        type: SourceType
    ): Flow<ImportProgress> = callbackFlow {
        try {
            val text: String
            val resolvedTitle: String

            when (type) {
                is SourceType.Pdf -> {
                    resolvedTitle = title
                    text = fileReader.readPdf(uri) { current, total ->
                        trySend(ImportProgress.Processing(current, total))
                    }.getOrThrow()
                }

                is SourceType.Txt -> {
                    text = fileReader.readText(uri).getOrThrow()
                    resolvedTitle = title
                }

                is SourceType.Fb2 -> {
                    val bytes = fileReader.readBytes(uri).getOrThrow()
                    trySend(ImportProgress.Segmenting)
                    val result = Fb2Parser.parse(bytes)
                    resolvedTitle = result.title
                    text = result.text
                }

                is SourceType.Epub -> {
                    val bytes = fileReader.readBytes(uri).getOrThrow()
                    trySend(ImportProgress.Segmenting)
                    val result = EpubParser.parse(bytes)
                    resolvedTitle = result.title
                    text = result.text
                }

                else -> error("Unsupported type: $type")
            }

            val segments = TextSegmenter.segment(text)
            val documentId = uuid4().toString()

            val document = Document(
                id = documentId,
                title = resolvedTitle,
                sourceType = type,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                lastOpenedAt = Clock.System.now().toEpochMilliseconds(),
                isFavorite = false,
                wordCount = text.split(Regex("\\s+")).size,
                estimatedReadingMinutes = text.split(Regex("\\s+")).size / 200,
                totalParagraphs = segments.size,
                lastParagraphIndex = 0
            )

            val paragraphs = segments.mapIndexed { index, paragraphText ->
                Paragraph(
                    id = uuid4().toString(),
                    documentId = documentId,
                    index = index,
                    text = paragraphText
                )
            }

            send(ImportProgress.Success(ProcessedDocument(document, paragraphs)))
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Import failed: type=$type, uri=$uri")
            send(ImportProgress.Error(e.message ?: "Import failed"))
        }
        close()
    }.flowOn(Dispatchers.Default)
}