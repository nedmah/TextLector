package com.nedmah.textlector.domain.repository

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun getAllDocuments(): Flow<List<Document>>
    fun getFavoriteDocs() : Flow<List<Document>>
    fun getRecentDocs(sortBy: DocumentSortOrder, limit: Int) : Flow<List<Document>>
    fun getDocumentById(id: String): Flow<Document?>

    suspend fun saveDocument(document: Document): Result<Unit>
    suspend fun deleteDocument(id: String): Result<Unit>
    suspend fun toggleFavorite(id: String, isFavorite: Boolean): Result<Unit>
    suspend fun updateLastOpened(id: String)
    suspend fun updateReadingProgress(id: String, paragraphIndex: Int)
    suspend fun updateTitle(id: String, title: String): Result<Unit>
}