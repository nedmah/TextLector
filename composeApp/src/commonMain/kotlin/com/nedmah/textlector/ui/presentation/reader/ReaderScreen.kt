package com.nedmah.textlector.ui.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nedmah.textlector.ui.presentation.player.PlayerIntent
import com.nedmah.textlector.ui.presentation.player.PlayerState
import com.nedmah.textlector.ui.presentation.player.PlayerViewModel
import com.nedmah.textlector.ui.presentation.reader.components.PlayerControls
import com.nedmah.textlector.ui.presentation.reader.components.ReaderTopBar
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReaderScreenRoot(
    documentId : String,
    onNavigateBack : () -> Unit,
    readerViewModel: ReaderViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinInject()
){

    val readerState by readerViewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(documentId) {
        readerViewModel.onIntent(ReaderIntent.LoadDocument(documentId))
        playerViewModel.onIntent(PlayerIntent.LoadDocument(documentId))
    }

    LaunchedEffect(Unit) {
        readerViewModel.effect.collect { effect ->
            when (effect) {
                ReaderEffect.NavigateBack -> onNavigateBack()
            }
        }
    }


    ReaderScreen(
        readerState = readerState,
        playerState = playerState,
        onReaderIntent = readerViewModel::onIntent,
        onPlayerIntent = playerViewModel::onIntent
    )

}

@Composable
fun ReaderScreen(
    readerState: ReaderState,
    playerState: PlayerState,
    onReaderIntent: (ReaderIntent) -> Unit,
    onPlayerIntent: (PlayerIntent) -> Unit
){


    Scaffold(
        topBar = {
            ReaderTopBar(
                title = readerState.document?.title ?: "",
                isFavorite = readerState.document?.isFavorite ?: false,
                onNavigateBack = { onReaderIntent(ReaderIntent.NavigateBack) },
                onToggleFavorite = {
                    val doc = readerState.document ?: return@ReaderTopBar
                    onReaderIntent(ReaderIntent.ToggleFavorite(doc.id, !doc.isFavorite))
                }
            )
        },
        bottomBar = {
            PlayerControls(
                isPlaying = playerState.isPlaying,
                playbackSpeed = playerState.playbackSpeed,
                onPlay = { onPlayerIntent(PlayerIntent.Play) },
                onPause = { onPlayerIntent(PlayerIntent.Pause) },
                onNext = { onPlayerIntent(PlayerIntent.NextParagraph) },
                onPrevious = { onPlayerIntent(PlayerIntent.PreviousParagraph) },
                onSpeedChange = { /* позже — bottom sheet со скоростями */ },
                progress = playerState.progress,
                isEnabled = playerState.isLoaded,
                isLoading = playerState.isLoading,
                onSeek = { fraction ->
                    val index = (fraction * playerState.paragraphs.size).toInt()
                        .coerceIn(0, playerState.paragraphs.lastIndex)
                    onPlayerIntent(PlayerIntent.SeekToParagraph(index))
                }
            )
        }
    ) { paddingValues ->
        if (readerState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ReaderContent(
                paragraphs = readerState.paragraphs,
                currentParagraphIndex = playerState.currentParagraphIndex,
                isPlaying = playerState.isPlaying,
                onParagraphClick = { index ->
                    onPlayerIntent(PlayerIntent.SeekToParagraph(index))
                    onPlayerIntent(PlayerIntent.Play)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

}