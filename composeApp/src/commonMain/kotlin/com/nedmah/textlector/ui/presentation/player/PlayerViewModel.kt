package com.nedmah.textlector.ui.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.common.platform.tts.TtsEngine
import com.nedmah.textlector.common.platform.tts.TtsQueue
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

private const val PLAYER_LOGS = true

private fun playerLog(message: String) {
    if (PLAYER_LOGS) println("[PlayerVM] $message")
}

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

    private var ttsQueue: TtsQueue? = null

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

                val piper = ttsEngine.piperEngine()
                when {
                    piper != null && ttsQueue == null -> ttsQueue = TtsQueue(piper)
                    piper == null && ttsQueue != null -> {
                        ttsQueue?.clear()
                        ttsQueue = null
                    }
                }
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
        val currentIndex = _state.value.currentParagraphIndex
        val paragraphs = _state.value.paragraphs
        val speed = _state.value.playbackSpeed

        if (ttsQueue == null) {
            ttsEngine.piperEngine()?.let {
                ttsQueue = TtsQueue(it)
                playerLog("play: TtsQueue создан (lazy init)")
            }  // if observePreferences didn't create it
        }

        val utteranceId = ++currentUtteranceId
        playbackJob?.cancel()
        ttsEngine.stop()
        _state.update { it.copy(isPlaying = true) }

        playerLog("play(index=$currentIndex, utterance=$utteranceId, speed=$speed)")

        playbackJob = viewModelScope.launch {
            val queue = ttsQueue

            if (queue != null) {
                val cached = queue.getCachedAudio(currentIndex)
                playerLog("queue=${if (cached != null) "CACHE HIT" else "CACHE MISS"} для index=$currentIndex")

                if (cached == null) {
                    _state.update { it.copy(isBuffering = true) }
                }

                val audio = try {
                    queue.getAudio(currentIndex, paragraph.text, speed)
                } finally {
                    _state.update { it.copy(isBuffering = false) }
                }  // getAudio: instant if prefetch already worked, otherwise generate

                if (utteranceId != currentUtteranceId) {
                    playerLog("utteranceId устарел после getAudio ($utteranceId != $currentUtteranceId), выхожу")
                    return@launch
                }

                queue.prefetchAhead(currentIndex, paragraphs, speed)
                playerLog("playAudio($currentIndex) начинаю, размер=${audio.size}b")
                ttsEngine.piperEngine()?.playAudio(audio)
                playerLog("playAudio($currentIndex) завершён")

            } else {
                playerLog("Native TTS path: speak($currentIndex)")
                ttsEngine.speak(paragraph.text, speed)
                playerLog("speak($currentIndex) завершён")
            }
            if (utteranceId == currentUtteranceId) {
                playerLog("navigateParagraph(+1) от index=$currentIndex")
                navigateParagraph(+1)
            }else {
                playerLog("utteranceId устарел после playback ($utteranceId != $currentUtteranceId), навигацию пропускаю")
            }
        }
    }

    private fun pause() {
        playerLog("pause() utterance=$currentUtteranceId")
        currentUtteranceId++
        playbackJob?.cancel()
        ttsQueue?.clear()
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

        if (delta < 0) {
            playerLog("navigateParagraph(-1): clear queue")
            ttsQueue?.clear()
        }
        val safeIndex = newIndex.coerceIn(0, current.paragraphs.lastIndex)
        playerLog("navigateParagraph($delta): ${current.currentParagraphIndex} → $safeIndex")
        _state.update { it.copy(currentParagraphIndex = safeIndex) }
        scheduleSaveProgress(safeIndex)

        if (_state.value.isPlaying) {
            play()
        }
    }

    private fun seekTo(index: Int) {
        if (_state.value.paragraphs.isEmpty()) return
        val safeIndex = index.coerceIn(0, _state.value.paragraphs.lastIndex)
        playerLog("seekTo($safeIndex): clear queue")
        ttsQueue?.clear()
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
        ttsQueue?.shutdown()
    }
}