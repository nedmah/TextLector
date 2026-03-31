package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.repository.ParagraphRepository
import kotlinx.coroutines.flow.Flow

class GetParagraphsUseCase(
    private val repository: ParagraphRepository
) {
    operator fun invoke(documentId: String): Flow<List<Paragraph>> =
        repository.getParagraphsByDocumentId(documentId)
}