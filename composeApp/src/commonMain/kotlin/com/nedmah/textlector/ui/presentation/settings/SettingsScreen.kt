package com.nedmah.textlector.ui.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreenRoot(
    viewModel: com.nedmah.textlector.ui.presentation.settings.SettingsViewModel = koinViewModel()
){

    val state by viewModel.state.collectAsStateWithLifecycle()
    _root_ide_package_.com.nedmah.textlector.ui.presentation.settings.SettingsScreen()

}

@Composable
private fun SettingsScreen(){

}