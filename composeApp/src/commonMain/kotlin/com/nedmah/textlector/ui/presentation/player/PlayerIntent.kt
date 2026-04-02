package com.nedmah.textlector.ui.presentation.player

interface PlayerIntent {
    data class LoadDocument(val documentId: String) :
        com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data object Play : com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data object Pause : com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data object NextParagraph : com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data object PreviousParagraph : com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data class SeekToParagraph(val index: Int) :
        com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data class ChangeSpeed(val speed: Float) :
        com.nedmah.textlector.ui.presentation.player.PlayerIntent
    data object Stop : com.nedmah.textlector.ui.presentation.player.PlayerIntent
}