package com.nedmah.textlector.ui.presentation.library

interface LibraryEffect {
    data class NavigateToReader(val documentId: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryEffect
    data object NavigateToImport : com.nedmah.textlector.ui.presentation.library.LibraryEffect
    data class ShowError(val message: String) :
        com.nedmah.textlector.ui.presentation.library.LibraryEffect
    data object DocumentDeleted : com.nedmah.textlector.ui.presentation.library.LibraryEffect
}