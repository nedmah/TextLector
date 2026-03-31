package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(
    private val repository: DocumentRepository
) {
    operator fun invoke() : Flow<List<Document>> =
        repository.getFavoriteDocs()
}