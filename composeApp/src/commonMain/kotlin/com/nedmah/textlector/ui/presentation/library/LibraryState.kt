package com.nedmah.textlector.ui.presentation.library

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder

data class LibraryState(
    val favoriteDocs : List<Document> = emptyList(),
    val recentDocs : List<Document> = emptyList(),
    val pendingDeleteDocumentId: String? = null,
    val sortOrder: DocumentSortOrder = DocumentSortOrder.LAST_OPENED,
    val isLoading: Boolean = false,
    val error: String? = null
)
