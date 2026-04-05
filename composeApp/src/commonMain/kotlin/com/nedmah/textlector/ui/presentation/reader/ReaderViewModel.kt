package com.nedmah.textlector.ui.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.usecase.GetDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetParagraphsUseCase
import com.nedmah.textlector.domain.usecase.ToggleFavoriteUseCase
import com.nedmah.textlector.ui.presentation.player.PlayerIntent
import com.nedmah.textlector.ui.presentation.player.PlayerViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val getDocumentUseCase: GetDocumentUseCase,
    private val getParagraphsUseCase: GetParagraphsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ReaderEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            ReaderIntent.NavigateBack -> {
                viewModelScope.launch {
                    _effect.send(ReaderEffect.NavigateBack)
                }
            }

            is ReaderIntent.LoadDocument -> loadDocument(intent.docId)
            is ReaderIntent.ToggleFavorite -> toggleFavorite(intent.docId, intent.isFavorite)
        }
    }

    private fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            launch {
                getDocumentUseCase(documentId).collect { document ->
                    _state.update {
                        it.copy(
                            document = document
                        )
                    }
                }
            }

            launch {
                getParagraphsUseCase(documentId).collect { paragraphs ->
                    _state.update { it.copy(paragraphs = paragraphs, isLoading = false) }
                }
            }
        }
    }

    private fun toggleFavorite(id: String, isFavorite: Boolean) = viewModelScope.launch {
        toggleFavoriteUseCase.invoke(id, isFavorite)
            .onFailure { sendEffect(ReaderEffect.ShowError(it.message ?: "Error")) }
    }

    private fun sendEffect(effect: ReaderEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}