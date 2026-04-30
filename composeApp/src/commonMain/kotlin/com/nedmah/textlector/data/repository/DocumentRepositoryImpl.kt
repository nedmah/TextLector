package com.nedmah.textlector.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.nedmah.textlector.common.platform.logging.CrashReporter
import com.nedmah.textlector.data.db.toDbString
import com.nedmah.textlector.data.db.toDomain
import com.nedmah.textlector.db.DocumentQueries
import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder
import com.nedmah.textlector.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.collections.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DocumentRepositoryImpl(
    private val queries: DocumentQueries
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() }}

    override fun getFavoriteDocs(): Flow<List<Document>> =
        queries.selectFavorites()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map{ list -> list.map { it.toDomain() }}

    override fun getRecentDocs(
        sortBy: DocumentSortOrder,
        limit: Int
    ): Flow<List<Document>> {
        val query = when (sortBy) {
            DocumentSortOrder.LAST_OPENED -> queries.selectRecentByLastOpened(limit.toLong())
            DocumentSortOrder.CREATED_AT -> queries.selectRecentByCreatedAt(limit.toLong())
        }
        return query
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDocumentById(id: String): Flow<Document?> =
        queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }

    override suspend fun saveDocument(document: Document): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                queries.insert(
                    id = document.id,
                    title = document.title,
                    source_type = document.sourceType.toDbString(),
                    created_at = document.createdAt,
                    last_opened_at = document.lastOpenedAt,
                    is_favorite = if (document.isFavorite) 1L else 0L,
                    word_count = document.wordCount.toLong(),
                    estimated_reading_minutes = document.estimatedReadingMinutes.toLong(),
                    total_paragraphs = document.totalParagraphs.toLong(),
                    last_paragraph_index = document.lastParagraphIndex.toLong()
                )
            }.onFailure {
                CrashReporter.recordException(it, "saveDocument failed: ${document.id}")
            }
        }

    override suspend fun deleteDocument(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { queries.deleteById(id) }
        }

    override suspend fun toggleFavorite(
        id: String,
        isFavorite: Boolean
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { queries.updateFavorite(
                isFavorite = if(isFavorite) 1L else 0L,
                id = id
            ) }
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateLastOpened(id: String) =
        withContext(Dispatchers.IO) {
            queries.updateLastOpened(lastOpenedAt = Clock.System.now().toEpochMilliseconds(), id = id)
        }

    override suspend fun updateReadingProgress(id: String, paragraphIndex: Int) =
        withContext(Dispatchers.IO) {
            queries.updateReadingProgress(paragraphIndex = paragraphIndex.toLong(), id = id)
        }
}