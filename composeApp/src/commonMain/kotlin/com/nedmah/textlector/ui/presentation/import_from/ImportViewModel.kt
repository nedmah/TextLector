package com.nedmah.textlector.ui.presentation.import_from

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.data.source.UrlContentFetcher
import com.nedmah.textlector.domain.model.ImportProgress
import com.nedmah.textlector.domain.model.SourceType
import com.nedmah.textlector.domain.usecase.ImportDocumentUseCase
import com.nedmah.textlector.domain.usecase.InputTextManuallyUseCase
import com.nedmah.textlector.domain.usecase.SaveDocumentUseCase
import com.nedmah.textlector.ui.presentation.import_from.ImportEffect.ShowError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportViewModel(
    private val inputTextManuallyUseCase: InputTextManuallyUseCase,
    private val importDocumentUseCase: ImportDocumentUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val urlContentFetcher: UrlContentFetcher
) : ViewModel() {

    private val _state =
        MutableStateFlow(ImportState())
    val state = _state.asStateFlow()

    private val _effect =
        Channel<ImportEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ImportIntent) {
        when (intent) {
            is ImportIntent.EnterText -> _state.update {
                it.copy(
                    manualText = intent.text
                )
            }

            is ImportIntent.FileSelected -> importFile(intent.uri, intent.mimeType)

            ImportIntent.ImportManually -> importManually()
            ImportIntent.DismissError -> _state.update {
                it.copy(
                    error = null
                )
            }

            ImportIntent.ConfirmImport -> confirmImport()
            ImportIntent.DismissImport -> dismissImport()

            ImportIntent.OpenUrlSheet -> _state.update { it.copy(showUrlSheet = true) }
            ImportIntent.DismissUrlSheet -> _state.update { it.copy(showUrlSheet = false, urlText = "") }

            is ImportIntent.EnterUrl -> _state.update { it.copy(urlText = intent.url) }
            ImportIntent.ImportFromUrl -> importFromUrl()
        }
    }

    private fun importManually() {
        val text = _state.value.manualText

        val lines = text.lines()
        val title: String
        val bodyText: String

        if (lines.size == 1) {
            title = text.split(" ").take(5).joinToString(" ")
            bodyText = text
        } else {
            title = lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Untitled"
            bodyText = lines.drop(1).joinToString("\n").trim()
            if (bodyText.isBlank()) {
                _state.update { it.copy(error = "Text cannot be empty") }
                return
            }
        }

        if (bodyText.isBlank()) {
            _state.update { it.copy(error = "Text cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            inputTextManuallyUseCase(title, bodyText)
                .onSuccess { document ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            processedDocument = document,
                            manualText = ""
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(
                        ImportEffect.ShowError(
                            error.message ?: "Import failed"
                        )
                    )
                }
        }
    }

    private fun importFile(uri: String, mimeType: String) {
        val sourceType = when (mimeType) {
            "application/pdf" -> SourceType.Pdf
            "text/plain" -> SourceType.Txt
            else -> {
                viewModelScope.launch {
                    _effect.send(ImportEffect.ShowError("Unsupported file type"))
                }
                return
            }
        }

        val title = uri.substringAfterLast("/").substringBeforeLast(".")

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, importProgress = null) }
            importDocumentUseCase(uri, title, sourceType)
                .collect { progress ->
                    when(progress){
                        is ImportProgress.Processing -> {
                            _state.update { it.copy(importProgress = progress) }
                        }
                        is ImportProgress.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    importProgress = null,
                                    processedDocument = progress.processedDocument
                                )
                            }
                        }
                        is ImportProgress.Error -> {
                            _state.update { it.copy(isLoading = false, importProgress = null) }
                            _effect.send(ShowError(progress.message))
                        }

                        ImportProgress.Segmenting -> _state.update { it.copy(importProgress = progress) }
                    }
                }
        }
    }

    private fun importFromUrl() {
        val url = _state.value.urlText.trim()
        if (url.isBlank()) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _state.update { it.copy(error = "Invalid URL — must start with http:// or https://") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            urlContentFetcher.fetchText(url)
                .onSuccess { (title, text) ->
                    inputTextManuallyUseCase(title, text)
                        .onSuccess { document ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showUrlSheet = false,
                                    processedDocument = document,
                                    urlText = ""
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(ShowError(error.message ?: "Processing failed"))
                        }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(ShowError(error.message ?: "Failed to fetch URL"))
                }
        }
    }

    private fun confirmImport() {
        val processed = _state.value.processedDocument ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            saveDocumentUseCase(processed.document, processed.paragraphs)
                .onSuccess {
                    _state.update { it.copy(processedDocument = null, isLoading = false) }
                    _effect.send(ImportEffect.NavigateToReader(processed.document.id))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(ImportEffect.ShowError(error.message ?: "Save failed"))
                }
        }
    }

    private fun dismissImport() {
        _state.update { it.copy(processedDocument = null) }
    }

}