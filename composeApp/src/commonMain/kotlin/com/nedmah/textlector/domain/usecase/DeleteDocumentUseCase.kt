package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.repository.DocumentRepository

class DeleteDocumentUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteDocument(id)
}