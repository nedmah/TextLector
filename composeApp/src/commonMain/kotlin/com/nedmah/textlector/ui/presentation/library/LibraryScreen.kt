package com.nedmah.textlector.ui.presentation.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreenRoot(
    onNavigateToReader: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: com.nedmah.textlector.ui.presentation.library.LibraryViewModel = koinViewModel()
){
    val state by viewModel.state.collectAsStateWithLifecycle()

    _root_ide_package_.com.nedmah.textlector.ui.presentation.library.LibraryScreen()
}

@Composable
private fun LibraryScreen(){

}