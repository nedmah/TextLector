package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.repository.DocumentRepository

class RenameDocumentUseCase(
    private val repository: DocumentRepository
) {

    suspend operator fun invoke(id: String, title: String) : Result<Unit>{
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        return repository.updateTitle(id, title.trim())
    }
}