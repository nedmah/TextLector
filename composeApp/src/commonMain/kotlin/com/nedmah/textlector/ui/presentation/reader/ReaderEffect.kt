package com.nedmah.textlector.ui.presentation.reader

interface ReaderEffect {
    data object NavigateBack : com.nedmah.textlector.ui.presentation.reader.ReaderEffect
    data class ShowError(val message: String) :
        com.nedmah.textlector.ui.presentation.reader.ReaderEffect
}