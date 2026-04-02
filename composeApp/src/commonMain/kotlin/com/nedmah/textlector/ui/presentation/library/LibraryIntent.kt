package com.nedmah.textlector.ui.presentation.library

import com.nedmah.textlector.domain.model.DocumentSortOrder

sealed interface LibraryIntent {
    data class DeleteDocument(val id: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data class ToggleFavorite(val id: String, val isFavorite: Boolean) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data class SearchDocuments(val query: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data class SelectDocument(val id: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data object OpenImport : com.nedmah.textlector.ui.presentation.library.LibraryIntent
}