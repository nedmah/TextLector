package com.nedmah.textlector.ui.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.usecase.GetDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetParagraphsUseCase
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
    private val playerViewModel: com.nedmah.textlector.ui.presentation.player.PlayerViewModel
) : ViewModel() {

    private val _state = MutableStateFlow(_root_ide_package_.com.nedmah.textlector.ui.presentation.reader.ReaderState())
    val state = _state.asStateFlow()

    private val _effect = Channel<com.nedmah.textlector.ui.presentation.reader.ReaderEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            playerViewModel.onIntent(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.LoadDocument(documentId))

            launch {
                getDocumentUseCase(documentId).collect { document ->
                    _state.update { it.copy(
                        document = document,
                        isLoading = false
                    )}
                }
            }

            launch {
                getParagraphsUseCase(documentId).collect { paragraphs ->
                    _state.update { it.copy(paragraphs = paragraphs) }
                }
            }
        }
    }

    fun onIntent(intent: com.nedmah.textlector.ui.presentation.reader.ReaderIntent) {
        when (intent) {
            is com.nedmah.textlector.ui.presentation.reader.ReaderIntent.TapParagraph -> {
                playerViewModel.onIntent(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.SeekToParagraph(intent.index))
                playerViewModel.onIntent(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.Play)
            }
            _root_ide_package_.com.nedmah.textlector.ui.presentation.reader.ReaderIntent.NavigateBack -> {
                viewModelScope.launch {
                    _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.reader.ReaderEffect.NavigateBack)
                }
            }
        }
    }
}