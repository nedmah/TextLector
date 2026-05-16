package com.nedmah.textlector.ui.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.model.DocumentSortOrder
import com.nedmah.textlector.domain.usecase.DeleteDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetDocumentsUseCase
import com.nedmah.textlector.domain.usecase.GetFavoritesUseCase
import com.nedmah.textlector.domain.usecase.GetRecentDocumentsUseCase
import com.nedmah.textlector.domain.usecase.RenameDocumentUseCase
import com.nedmah.textlector.domain.usecase.ToggleFavoriteUseCase
import com.nedmah.textlector.domain.usecase.UpdateLastOpenedUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getRecentDocumentsUseCase: GetRecentDocumentsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateLastOpenedUseCase: UpdateLastOpenedUseCase,
    private val renameDocumentUseCase: RenameDocumentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(_root_ide_package_.com.nedmah.textlector.ui.presentation.library.LibraryState())
    val state = _state.asStateFlow()

    private val _effect = Channel<com.nedmah.textlector.ui.presentation.library.LibraryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var searchJob : Job? = null
    private var recentDocsJob: Job? = null
    private var favoritesJob: Job? = null

    init {
        loadDocuments()
    }

    fun onEvent(intent: com.nedmah.textlector.ui.presentation.library.LibraryIntent){
        when(intent){
            is LibraryIntent.ToggleFavorite -> toggleFavorite(intent.id, intent.isFavorite)
            is LibraryIntent.SearchDocuments -> searchForDocs(intent.query)
            is LibraryIntent.SelectDocument -> selectDocument(intent.id)
            LibraryIntent.OpenImport -> sendEffect(
                LibraryEffect.NavigateToImport)

            is LibraryIntent.ChangeSortType -> changeSortOrder(intent.sortOrder)
            LibraryIntent.CancelDelete -> _state.update { it.copy(pendingDeleteDocumentId = null) }
            LibraryIntent.ConfirmDelete -> {
                val id = _state.value.pendingDeleteDocumentId ?: return
                _state.update { it.copy(pendingDeleteDocumentId = null) }
                deleteDocument(id)
            }
            is LibraryIntent.RequestDelete -> _state.update { it.copy(pendingDeleteDocumentId = intent.id) }
            LibraryIntent.DismissRenameDialog -> _state.update { it.copy(documentToRename = null) }
            is LibraryIntent.OpenRenameDialog -> _state.update { it.copy(documentToRename = intent.document) }
            is LibraryIntent.RenameDocument -> renameDocument(intent.id, intent.newTitle)
        }
    }

    private fun searchForDocs(query : String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L) // debounce
            if (query.isBlank()) {
                changeSortOrder(_state.value.sortOrder)
                return@launch
            }
            val filtered = _state.value.recentDocs
                .filter { it.title.contains(query, ignoreCase = true) }
            _state.update { it.copy(recentDocs = filtered) }
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            favoritesJob?.cancel()
            favoritesJob = launch {
                getFavoritesUseCase().collect { favorites ->
                    _state.update { it.copy(favoriteDocs = favorites) }
                }
            }

            recentDocsJob?.cancel()
            recentDocsJob = launch {
                getRecentDocumentsUseCase(DocumentSortOrder.LAST_OPENED)
                    .collect { recent ->
                        _state.update { it.copy(
                            recentDocs = recent,
                            isLoading = false
                        )}
                    }
            }
        }
    }

    private fun selectDocument(id: String) {
        viewModelScope.launch {
            updateLastOpenedUseCase(id)
            sendEffect(_root_ide_package_.com.nedmah.textlector.ui.presentation.library.LibraryEffect.NavigateToReader(id))
        }
    }

    private fun toggleFavorite(id : String, isFavorite : Boolean)= viewModelScope.launch {
        toggleFavoriteUseCase(id, isFavorite)
            .onFailure { sendEffect(_root_ide_package_.com.nedmah.textlector.ui.presentation.library.LibraryEffect.ShowError(it.message ?: "Error")) }
    }

    private fun changeSortOrder(sortOrder: DocumentSortOrder) {
        _state.update { it.copy(sortOrder = sortOrder) }
        recentDocsJob?.cancel()
        recentDocsJob = viewModelScope.launch {
            getRecentDocumentsUseCase(sortOrder).collect { recent ->
                _state.update { it.copy(recentDocs = recent) }
            }
        }
    }

    private fun renameDocument(id: String, title: String) = viewModelScope.launch {
        renameDocumentUseCase(id, title)
            .onFailure { sendEffect(LibraryEffect.ShowError(it.message ?: "Rename failed")) }
        _state.update { it.copy(documentToRename = null) }
    }

    private fun deleteDocument(id: String) = viewModelScope.launch {
        deleteDocumentUseCase(id)
            .onSuccess { sendEffect(LibraryEffect.DocumentDeleted) }
            .onFailure { sendEffect(LibraryEffect.ShowError(it.message ?: "Delete failed")) }
    }


    private fun sendEffect(effect: com.nedmah.textlector.ui.presentation.library.LibraryEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

}