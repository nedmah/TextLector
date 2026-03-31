package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetDocumentUseCase(
    private val repository: DocumentRepository
) {
    operator fun invoke(id: String): Flow<Document?> =
        repository.getDocumentById(id)
}