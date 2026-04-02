package com.nedmah.textlector.ui.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nedmah.textlector.domain.usecase.GetPreferencesUseCase
import com.nedmah.textlector.domain.usecase.UpdatePreferencesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(_root_ide_package_.com.nedmah.textlector.ui.presentation.settings.SettingsState())
    val state = _state.asStateFlow()

    init {
        observePreferences()
    }

    fun onIntent(intent: com.nedmah.textlector.ui.presentation.settings.SettingsIntent) {
        when (intent) {
            is com.nedmah.textlector.ui.presentation.settings.SettingsIntent.SetSpeed -> {
                _state.update { it.copy(preferences = it.preferences.copy(speechSpeed = intent.speed)) }
                viewModelScope.launch { updatePreferencesUseCase.setSpeed(intent.speed) }
            }
            is com.nedmah.textlector.ui.presentation.settings.SettingsIntent.SetFontSize -> {
                _state.update { it.copy(preferences = it.preferences.copy(fontSize = intent.size)) }
                viewModelScope.launch { updatePreferencesUseCase.setFontSize(intent.size) }
            }
            is com.nedmah.textlector.ui.presentation.settings.SettingsIntent.SetDarkMode -> {
                _state.update { it.copy(preferences = it.preferences.copy(isDarkMode = intent.enabled)) }
                viewModelScope.launch { updatePreferencesUseCase.setDarkMode(intent.enabled) }
            }
            is com.nedmah.textlector.ui.presentation.settings.SettingsIntent.SetVoice -> {
                _state.update { it.copy(preferences = it.preferences.copy(speechVoice = intent.gender)) }
                viewModelScope.launch { updatePreferencesUseCase.setVoice(intent.gender) }
            }
            is com.nedmah.textlector.ui.presentation.settings.SettingsIntent.SetLanguage -> {
                _state.update { it.copy(preferences = it.preferences.copy(language = intent.language)) }
                viewModelScope.launch { updatePreferencesUseCase.setLanguage(intent.language) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            getPreferencesUseCase().collect { prefs ->
                _state.update { it.copy(preferences = prefs) }
            }
        }
    }


}