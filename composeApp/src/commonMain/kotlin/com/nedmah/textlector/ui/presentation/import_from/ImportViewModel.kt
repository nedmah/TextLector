package com.nedmah.textlector.ui.presentation.import_from

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.model.SourceType
import com.nedmah.textlector.domain.usecase.ImportDocumentUseCase
import com.nedmah.textlector.domain.usecase.InputTextManuallyUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportViewModel(
    private val inputTextManuallyUseCase: InputTextManuallyUseCase,
    private val importDocumentUseCase: ImportDocumentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(_root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportState())
    val state = _state.asStateFlow()

    private val _effect = Channel<com.nedmah.textlector.ui.presentation.import_from.ImportEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: com.nedmah.textlector.ui.presentation.import_from.ImportIntent) {
        when (intent) {
            is com.nedmah.textlector.ui.presentation.import_from.ImportIntent.EnterText -> _state.update { it.copy(manualText = intent.text) }
            is com.nedmah.textlector.ui.presentation.import_from.ImportIntent.EnterTitle -> _state.update { it.copy(title = intent.title) }
            is com.nedmah.textlector.ui.presentation.import_from.ImportIntent.ImportFile -> importFile(intent.uri, intent.mimeType)
            _root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportIntent.ImportManually -> importManually()
            _root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportIntent.DismissError -> _state.update { it.copy(error = null) }
            _root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportIntent.ConfirmImport -> confirmImport()
            _root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportIntent.DismissImport -> _state.update { it.copy(importedDocument = null) }
        }
    }

    private fun importManually() {
        val title = _state.value.title.ifBlank { "Untitled" }
        val text = _state.value.manualText

        if (text.isBlank()) {
            _state.update { it.copy(error = "Text cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            inputTextManuallyUseCase(title, text)
                .onSuccess { document ->
                    _state.update { it.copy(
                        isLoading = false,
                        importedDocument = document,
                        manualText = "",
                        title = ""
                    )}
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportEffect.ShowError(error.message ?: "Import failed"))
                }
        }
    }

    private fun importFile(uri: String, mimeType: String) {
        val sourceType = when {
            mimeType == "application/pdf" -> SourceType.Pdf
            mimeType == "text/plain" -> SourceType.Txt
            else -> {
                viewModelScope.launch {
                    _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportEffect.ShowError("Unsupported file type"))
                }
                return
            }
        }

        val title = _state.value.title.ifBlank { "Imported Document" }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            importDocumentUseCase(uri, title, sourceType)
                .onSuccess { document ->
                    _state.update { it.copy(
                        isLoading = false,
                        importedDocument = document
                    )}
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportEffect.ShowError(error.message ?: "Import failed"))
                }
        }
    }

    private fun confirmImport() {
        val documentId = _state.value.importedDocument?.id ?: return
        _state.update { it.copy(importedDocument = null) }
        viewModelScope.launch {
            _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportEffect.NavigateToReader(documentId))
        }
    }
}