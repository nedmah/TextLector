package com.nedmah.textlector


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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

    LectorTheme(isDarkMode = settingsState.preferences.isDarkMode) {
        TextLectorNavGraph()
    }
}