package com.nedmah.textlector.domain.usecase

import com.nedmah.textlector.domain.model.UserPreferences
import com.nedmah.textlector.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetPreferencesUseCase(
    private val repository: PreferencesRepository
) {
    operator fun invoke(): Flow<UserPreferences> =
        repository.getPreferences()
}