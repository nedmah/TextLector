package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder
import com.nedmah.textlector.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetRecentDocumentsUseCase(
    private val repository: DocumentRepository
) {

    operator fun invoke(sortOrder: DocumentSortOrder, limit: Int = 5): Flow<List<Document>> =
        repository.getRecentDocs(sortOrder, limit)
}