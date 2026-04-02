package com.nedmah.textlector.ui.presentation.reader

sealed interface ReaderIntent {
    data class TapParagraph(val index: Int) :
        com.nedmah.textlector.ui.presentation.reader.ReaderIntent
    data object NavigateBack : com.nedmah.textlector.ui.presentation.reader.ReaderIntent
}