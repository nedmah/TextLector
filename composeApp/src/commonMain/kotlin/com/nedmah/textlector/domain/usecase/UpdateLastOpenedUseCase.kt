package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.repository.DocumentRepository

class UpdateLastOpenedUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String) =
        repository.updateLastOpened(documentId)
}