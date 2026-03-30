package com.nedmah.textlector.data.repository

import com.nedmah.textlector.db.ParagraphQueries
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.domain.repository.ParagraphRepository
import kotlinx.coroutines.flow.Flow

class ParagraphRepositoryImpl(
    private val queries: ParagraphQueries
) : ParagraphRepository{

    override fun getParagraphsByDocumentId(documentId: String): Flow<List<Paragraph>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveParagraphs(paragraphs: List<Paragraph>): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteParagraphsByDocumentId(documentId: String): Result<Unit> {
        TODO("Not yet implemented")
    }
}