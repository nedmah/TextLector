package com.nedmah.textlector


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.nedmah.textlector.common.navigation.TextLectorNavGraph
import com.nedmah.textlector.ui.presentation.settings.SettingsViewModel
import com.nedmah.textlector.ui.theme.LectorTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val settingsState by settingsViewModel.state.collectAsState()

    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkMode = settingsState.preferences.isDarkMode ?: systemDarkTheme

    LectorTheme(isDarkMode = isDarkMode) {
        TextLectorNavGraph()
    }
}