package com.nedmah.textlector.ui.presentation.import_from

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.common.platform.ocr.OcrEngine
import com.nedmah.textlector.data.source.UrlContentFetcher
import com.nedmah.textlector.domain.model.ImportProgress
import com.nedmah.textlector.domain.model.ModelState
import com.nedmah.textlector.domain.model.SourceType
import com.nedmah.textlector.domain.repository.OcrDataRepository
import com.nedmah.textlector.domain.usecase.DownloadOcrDataUseCase
import com.nedmah.textlector.domain.usecase.ImportDocumentUseCase
import com.nedmah.textlector.domain.usecase.InputTextManuallyUseCase
import com.nedmah.textlector.domain.usecase.SaveDocumentUseCase
import com.nedmah.textlector.domain.util.UrlValidator
import com.nedmah.textlector.ui.presentation.import_from.ImportEffect.ShowError
import kotlinx.coroutines.Job
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
    private val urlContentFetcher: UrlContentFetcher,
    private val ocrEngine: OcrEngine,
    private val ocrDataRepository: OcrDataRepository,
    private val downloadOcrDataUseCase: DownloadOcrDataUseCase
) : ViewModel() {

    private val _state =
        MutableStateFlow(ImportState())
    val state = _state.asStateFlow()

    private val _effect =
        Channel<ImportEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var downloadJob: Job? = null

    init {
        observeOcrDataState()
    }

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
            ImportIntent.DismissUrlSheet -> _state.update {
                it.copy(
                    showUrlSheet = false,
                    urlText = "",
                    urlError = null
                )
            }

            is ImportIntent.EnterUrl -> {
                val error = UrlValidator.validate(intent.url)
                _state.update {
                    it.copy(
                        urlText = intent.url,
                        urlError = if (error != null) UrlValidator.errorMessage(error) else null
                    )
                }
            }

            ImportIntent.ImportFromUrl -> importFromUrl()

            ImportIntent.OpenCamera -> {
                if (ocrDataRepository.isReady())
                    _state.update { it.copy(shouldLaunchCamera = true) }
                else
                    _state.update { it.copy(showOcrDownloadDialog = true) }
            }

            ImportIntent.CameraLaunched ->
                _state.update { it.copy(shouldLaunchCamera = false) }

            is ImportIntent.CameraImageCaptured -> processOcrImage(intent.uri)

            ImportIntent.DownloadOcrData -> downloadOcrData()

            ImportIntent.DismissOcrDialog -> _state.update { it.copy(showOcrDownloadDialog = false) }
        }
    }

    private fun observeOcrDataState() {
        viewModelScope.launch {
            ocrDataRepository.getState().collect { modelState ->
                _state.update { it.copy(ocrDataState = modelState) }

                if (modelState is ModelState.Ready && _state.value.showOcrDownloadDialog) {
                    _state.update {
                        it.copy(
                            showOcrDownloadDialog = false,
                            shouldLaunchCamera = true
                        )
                    }
                }
            }
        }
    }

    private fun importManually() {
        val text = _state.value.manualText
        if (text.isBlank()) {
            _state.update { it.copy(error = "Text cannot be empty") }
            return
        }

        val lines = text.lines()
        val title: String
        val bodyText: String

        if (lines.size == 1) {
            title = text.split(" ").take(5).joinToString(" ")
            bodyText = text
        } else {
            title = lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Untitled"
            bodyText = lines.drop(1).joinToString("\n").trim()
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
            "application/epub+zip" -> SourceType.Epub
            "application/x-fictionbook+xml", "text/xml", "application/xml" -> SourceType.Fb2
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
                    when (progress) {
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, urlError = null) }
            urlContentFetcher.fetchText(url)
                .onSuccess { (title, text) ->
                    inputTextManuallyUseCase(title, text)
                        .onSuccess { document ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    showUrlSheet = false,
                                    processedDocument = document,
                                    urlText = "",
                                    urlError = null
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

    private fun downloadOcrData() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            downloadOcrDataUseCase().collect { state ->
                _state.update { it.copy(ocrDataState = state) }
            }
        }
    }

    private fun processOcrImage(uri: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            runCatching { ocrEngine.recognize(uri) }
                .onSuccess { text ->
                    val title = text.lines()
                        .firstOrNull { it.isNotBlank() }
                        ?.trim()
                        ?.take(50)
                        ?: "Camera scan"

                    inputTextManuallyUseCase(title, text)
                        .onSuccess { document ->
                            _state.update {
                                it.copy(isLoading = false, processedDocument = document)
                            }
                        }
                        .onFailure { e ->
                            _state.update { it.copy(isLoading = false) }
                            _effect.send(ImportEffect.ShowError(e.message ?: "Processing failed"))
                        }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(ImportEffect.ShowError(e.message ?: "OCR failed"))
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