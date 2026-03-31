package com.nedmah.textlector.data.repository

import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.repository.PreferencesRepository
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class PreferencesRepositoryImpl(
    private val settings: ObservableSettings
) : PreferencesRepository {

    companion object{
        const val KEY_VOICE_GENDER = "voice_gender"
        const val KEY_PLAYBACK_SPEED = "playback_speed"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LANGUAGE = "language"

    }

    override suspend fun setVoiceProfile(type: VoiceGender) =
        withContext(Dispatchers.IO) {
            settings.putString(KEY_VOICE_GENDER, type.name)
        }

    override suspend fun setPlaybackSpeed(speed: Float) =
        withContext(Dispatchers.IO){
            settings.putFloat(KEY_PLAYBACK_SPEED,speed)
        }

    override suspend fun setFontSize(size: Int) =
        withContext(Dispatchers.IO) {
            settings.putInt(KEY_FONT_SIZE, size)
        }

    override suspend fun setDarkMode(mode: Boolean) =
        withContext(Dispatchers.IO) {
            settings.putBoolean(KEY_DARK_MODE, mode)
        }

    override suspend fun setVoiceLanguage(language: String) =
        withContext(Dispatchers.IO) {
            settings.putString(KEY_LANGUAGE, language)
        }

    @OptIn(ExperimentalSettingsApi::class)
    override fun getPreferences(): Flow<UserPreferences> =
        combine(
            settings.getStringFlow(KEY_VOICE_GENDER, VoiceGender.MALE.name),
            settings.getFloatFlow(KEY_PLAYBACK_SPEED, 1f),
            settings.getIntFlow(KEY_FONT_SIZE, 16),
            settings.getBooleanFlow(KEY_DARK_MODE, false),
            settings.getStringFlow(KEY_LANGUAGE, "en")
        ) { gender, speed, fontSize, darkMode, language ->
            UserPreferences(
                speechVoice = VoiceGender.valueOf(gender),
                speechSpeed = speed,
                fontSize = fontSize,
                isDarkMode = darkMode,
                language = language
            )

        }
}