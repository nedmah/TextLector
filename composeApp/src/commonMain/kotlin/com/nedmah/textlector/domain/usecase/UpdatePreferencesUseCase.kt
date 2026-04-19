package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.repository.PreferencesRepository

class UpdatePreferencesUseCase(
    private val repository: PreferencesRepository
) {
    suspend fun setSpeed(speed: Float) = repository.setPlaybackSpeed(speed)
    suspend fun setFontSize(size: Int) = repository.setFontSize(size)
    suspend fun setDarkMode(enabled: Boolean) = repository.setDarkMode(enabled)
    suspend fun setVoice(gender: VoiceGender) = repository.setVoiceProfile(gender)
    suspend fun setLanguage(language: String) = repository.setVoiceLanguage(language)
    suspend fun setUseSherpaEngine(useSherpa : Boolean) = repository.setUseSherpaEngine(useSherpa)

}