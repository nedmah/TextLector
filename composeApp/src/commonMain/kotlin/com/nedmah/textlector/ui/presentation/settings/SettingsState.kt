package com.nedmah.textlector.ui.presentation.settings

import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.model.VoiceGender

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(
        speechSpeed = 1f,
        speechVoice = VoiceGender.MALE,
        fontSize = 16,
        isDarkMode = false,
        language = "en"
    )
)