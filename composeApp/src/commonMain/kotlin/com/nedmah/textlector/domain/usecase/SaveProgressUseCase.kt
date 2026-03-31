package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.repository.DocumentRepository

class SaveProgressUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String, paragraphIndex: Int) =
        repository.updateReadingProgress(documentId, paragraphIndex)
}