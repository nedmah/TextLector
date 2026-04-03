package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.repository.DocumentRepository
import com.nedmah.textlector.domain.repository.ParagraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SaveDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val paragraphRepository: ParagraphRepository
) {
    suspend operator fun invoke(
        document: Document,
        paragraphs: List<Paragraph>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            documentRepository.saveDocument(document).getOrThrow()
            paragraphRepository.saveParagraphs(paragraphs).getOrThrow()
        }
    }
}