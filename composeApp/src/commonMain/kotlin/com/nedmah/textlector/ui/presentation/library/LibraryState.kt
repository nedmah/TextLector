package com.nedmah.textlector.ui.presentation.library

import com.nedmah.textlector.domain.model.Document

data class LibraryState(
    val favoriteDocs : List<Document> = emptyList(),
    val recentDocs : List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
