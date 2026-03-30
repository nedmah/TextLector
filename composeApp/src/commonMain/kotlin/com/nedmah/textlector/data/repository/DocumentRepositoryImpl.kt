package com.nedmah.textlector.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.nedmah.textlector.data.db.toDomain
import com.nedmah.textlector.db.DocumentQueries
import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder
import com.nedmah.textlector.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepositoryImpl(
    private val queries: DocumentQueries
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() }}

    override fun getFavoriteDocs(): Flow<List<Document>> {
        TODO("Not yet implemented")
    }

    override fun getRecentDocs(
        sortBy: DocumentSortOrder,
        limit: Int
    ): Flow<List<Document>> {
        TODO("Not yet implemented")
    }

    override fun getDocumentById(id: String): Flow<Document?> {
        TODO("Not yet implemented")
    }

    override suspend fun saveDocument(document: Document): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteDocument(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun toggleFavorite(
        id: String,
        isFavorite: Boolean
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateLastOpened(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateReadingProgress(id: String, paragraphIndex: Int) {
        TODO("Not yet implemented")
    }
}