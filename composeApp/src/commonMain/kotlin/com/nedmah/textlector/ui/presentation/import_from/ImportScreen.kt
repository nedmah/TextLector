package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportScreenRoot(
    onNavigateToReader: (String) -> Unit,
    viewModel: com.nedmah.textlector.ui.presentation.import_from.ImportViewModel = koinViewModel()
){

    val state by viewModel.state.collectAsStateWithLifecycle()

    _root_ide_package_.com.nedmah.textlector.ui.presentation.import_from.ImportScreen()
}

@Composable
private fun ImportScreen(){

}