package com.nedmah.textlector.domain.repository

import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.model.VoiceGender
import com.nedmah.textlector.domain.model.VoiceId
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    suspend fun setVoiceProfile(type: VoiceGender)

    suspend fun setPlaybackSpeed(speed : Float)

    suspend fun setFontSize(size: Int)

    suspend fun setDarkMode(mode : Boolean)

    suspend fun setVoiceLanguage(language : String)

    suspend fun setUseSherpaEngine(value: Boolean)


    fun getPreferences(): Flow<UserPreferences>

}