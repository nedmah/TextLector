package com.nedmah.textlector.ui.presentation.reader


sealed interface ReaderIntent {
    data object NavigateBack : ReaderIntent

    data class LoadDocument(val docId : String) : ReaderIntent
    data class ToggleFavorite(val docId : String, val isFavorite: Boolean) : ReaderIntent
}