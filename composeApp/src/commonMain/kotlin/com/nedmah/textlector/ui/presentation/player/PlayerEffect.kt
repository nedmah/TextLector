package com.nedmah.textlector.ui.presentation.player

interface PlayerEffect {
    data class ShowError(val message: String) :
        com.nedmah.textlector.ui.presentation.player.PlayerEffect
    data object PlaybackFinished : com.nedmah.textlector.ui.presentation.player.PlayerEffect
}