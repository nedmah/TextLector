package com.nedmah.textlector.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.nedmah.textlector.data.db.toDomain
import com.nedmah.textlector.db.ParagraphQueries
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.repository.ParagraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ParagraphRepositoryImpl(
    private val queries: ParagraphQueries
) : ParagraphRepository {

    override fun getParagraphsByDocumentId(documentId: String): Flow<List<Paragraph>> =
        queries.selectByDocumentId(documentId).asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun saveParagraphs(paragraphs: List<Paragraph>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                queries.transaction {
                    paragraphs.forEach { pg ->
                        queries.insertParagraph(
                            id = pg.id,
                            document_id = pg.documentId,
                            index_in_doc = pg.index.toLong(),
                            text = pg.text
                        )
                    }
                }
            }
        }

    override suspend fun deleteParagraphsByDocumentId(documentId: String): Result<Unit> =
        withContext(Dispatchers.IO){
            runCatching {
                queries.deleteByDocumentId(documentId)
            }
        }
}