package com.nedmah.textlector.ui.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.model.VoiceId
import com.nedmah.textlector.domain.model.VoiceRegistry
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

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private val _effect = Channel<PlayerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var saveProgressJob: Job? = null
    private var playbackJob: Job? = null

    private var currentUtteranceId: Int = 0

    init {
        observePreferences()
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.LoadDocument -> loadDocument(intent.documentId)
            PlayerIntent.Play -> play()
            PlayerIntent.Pause -> pause()
            PlayerIntent.NextParagraph -> navigateParagraph(+1)
            PlayerIntent.PreviousParagraph -> navigateParagraph(-1)
            is PlayerIntent.SeekToParagraph -> seekTo(intent.index)
            is PlayerIntent.ChangeSpeed -> changeSpeed(intent.speed)
            PlayerIntent.Stop -> stop()
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
        if (_state.value.document?.id == documentId) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            updateLastOpenedUseCase(documentId)

            launch {
                getDocumentUseCase(documentId).collect { document ->
                    if (document == null) {
                        _effect.send(PlayerEffect.ShowError("Document not found"))
                        return@collect
                    }
                    _state.update {
                        it.copy(
                            document = document,
                            currentParagraphIndex = document.lastParagraphIndex
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

    private fun play() {
        if (!_state.value.isLoaded) return
        if (_state.value.isLoading) return
        val paragraph = _state.value.currentParagraph ?: return

        val utteranceId = ++currentUtteranceId
        playbackJob?.cancel()
        ttsEngine.stop()

        _state.update { it.copy(isPlaying = true) }

        playbackJob = viewModelScope.launch {
            ttsEngine.speak(
                text = paragraph.text,
                speed = _state.value.playbackSpeed,
            )
            if (utteranceId == currentUtteranceId){
                navigateParagraph(+1)
            }
        }
    }

    private fun pause() {
        currentUtteranceId++
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
        if (current.paragraphs.isEmpty()) return
        val newIndex = current.currentParagraphIndex + delta

        if (newIndex > current.paragraphs.lastIndex) {
            pause()
            viewModelScope.launch {
                _effect.send(PlayerEffect.PlaybackFinished)
            }
            return
        }

        val safeIndex = newIndex.coerceIn(0, current.paragraphs.lastIndex)
        _state.update { it.copy(currentParagraphIndex = safeIndex) }
        scheduleSaveProgress(safeIndex)

        if (_state.value.isPlaying) {
            play()
        }
    }

    private fun seekTo(index: Int) {
        if (_state.value.paragraphs.isEmpty()) return
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