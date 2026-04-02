package com.nedmah.textlector.ui.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.usecase.GetDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetParagraphsUseCase
import com.nedmah.textlector.domain.usecase.GetPreferencesUseCase
import com.nedmah.textlector.domain.usecase.SaveProgressUseCase
import com.nedmah.textlector.domain.usecase.UpdateLastOpenedUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val getDocumentUseCase: GetDocumentUseCase,
    private val getParagraphsUseCase: GetParagraphsUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val updateLastOpenedUseCase: UpdateLastOpenedUseCase,
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _state = MutableStateFlow(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerState())
    val state = _state.asStateFlow()

    private val _effect = Channel<com.nedmah.textlector.ui.presentation.player.PlayerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var saveProgressJob: Job? = null
    private var playbackJob: Job? = null

    init {
        observePreferences()
    }

    fun onIntent(intent: com.nedmah.textlector.ui.presentation.player.PlayerIntent) {
        when (intent) {
            is com.nedmah.textlector.ui.presentation.player.PlayerIntent.LoadDocument -> loadDocument(intent.documentId)
            _root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.Play -> play()
            _root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.Pause -> pause()
            _root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.NextParagraph -> navigateParagraph(+1)
            _root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.PreviousParagraph -> navigateParagraph(-1)
            is com.nedmah.textlector.ui.presentation.player.PlayerIntent.SeekToParagraph -> seekTo(intent.index)
            is com.nedmah.textlector.ui.presentation.player.PlayerIntent.ChangeSpeed -> changeSpeed(intent.speed)
            _root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerIntent.Stop -> stop()
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            getPreferencesUseCase().collect { prefs ->
                _state.update { it.copy(playbackSpeed = prefs.speechSpeed) }
            }
        }
    }

    private fun loadDocument(documentId: String) {
        if (_state.value.document?.id == documentId) {
            return
        }

        viewModelScope.launch {
            updateLastOpenedUseCase(documentId)

            launch {
                getDocumentUseCase(documentId).collect { document ->
                    if (document == null) {
                        _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerEffect.ShowError("Document not found"))
                        return@collect
                    }
                    _state.update { it.copy(
                        document = document,
                        currentParagraphIndex = document.lastParagraphIndex
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

    private fun play() {
        val paragraph = _state.value.currentParagraph ?: return
        ttsEngine.stop()

        _state.update { it.copy(isPlaying = true) }

        playbackJob = viewModelScope.launch {
            ttsEngine.speak(
                text = paragraph.text,
                speed = _state.value.playbackSpeed,
                onDone = {
                    viewModelScope.launch { navigateParagraph(+1) }
                }
            )
        }
    }

    private fun pause() {
        playbackJob?.cancel()
        ttsEngine.stop()
        _state.update { it.copy(isPlaying = false) }
    }

    private fun stop() {
        pause()
        scheduleSaveProgress(_state.value.currentParagraphIndex)
    }

    private fun navigateParagraph(delta: Int) {
        val current = _state.value
        val newIndex = current.currentParagraphIndex + delta

        if (newIndex > current.paragraphs.lastIndex) {
            pause()
            viewModelScope.launch {
                _effect.send(_root_ide_package_.com.nedmah.textlector.ui.presentation.player.PlayerEffect.PlaybackFinished)
            }
            return
        }

        val safeIndex = newIndex.coerceIn(0, current.paragraphs.lastIndex)
        _state.update { it.copy(currentParagraphIndex = safeIndex) }
        scheduleSaveProgress(safeIndex)

        if (current.isPlaying) {
            play()
        }
    }

    private fun seekTo(index: Int) {
        val safeIndex = index.coerceIn(0, _state.value.paragraphs.lastIndex)
        _state.update { it.copy(currentParagraphIndex = safeIndex) }
        scheduleSaveProgress(safeIndex)

        if (_state.value.isPlaying) {
            play()
        }
    }

    private fun changeSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        if (_state.value.isPlaying) {
            pause()
            play()
        }
    }

    private fun scheduleSaveProgress(index: Int) {
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(3_000L)
            val documentId = _state.value.document?.id ?: return@launch
            saveProgressUseCase(documentId, index)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }
}