package com.nedmah.textlector.ui.presentation.import_from

interface ImportEffect {
    data class NavigateToReader(val documentId: String) :
        com.nedmah.textlector.ui.presentation.import_from.ImportEffect
    data class ShowError(val message: String) :
        com.nedmah.textlector.ui.presentation.import_from.ImportEffect
}