package com.nedmah.textlector.ui.presentation.settings

import com.nedmah.textlector.domain.model.VoiceGender

interface SettingsIntent {
    data class SetSpeed(val speed: Float) :
        com.nedmah.textlector.ui.presentation.settings.SettingsIntent
    data class SetFontSize(val size: Int) :
        com.nedmah.textlector.ui.presentation.settings.SettingsIntent
    data class SetDarkMode(val enabled: Boolean) :
        com.nedmah.textlector.ui.presentation.settings.SettingsIntent
    data class SetVoice(val gender: VoiceGender) :
        com.nedmah.textlector.ui.presentation.settings.SettingsIntent
    data class SetLanguage(val language: String) :
        com.nedmah.textlector.ui.presentation.settings.SettingsIntent
}