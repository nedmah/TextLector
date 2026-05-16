package com.nedmah.textlector.ui.presentation.library

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.DocumentSortOrder

sealed interface LibraryIntent {
    data class ToggleFavorite(val id: String, val isFavorite: Boolean) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data class SearchDocuments(val query: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data class SelectDocument(val id: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryIntent
    data object OpenImport : com.nedmah.textlector.ui.presentation.library.LibraryIntent

    data class ChangeSortType(val sortOrder: DocumentSortOrder) : LibraryIntent

    data class RequestDelete(val id: String) : LibraryIntent
    data object ConfirmDelete : LibraryIntent
    data object CancelDelete : LibraryIntent

    data class OpenRenameDialog(val document: Document) : LibraryIntent
    data object DismissRenameDialog : LibraryIntent
    data class RenameDocument(val id: String, val newTitle: String) : LibraryIntent
}