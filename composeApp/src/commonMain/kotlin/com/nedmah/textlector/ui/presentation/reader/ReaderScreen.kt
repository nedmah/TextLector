package com.nedmah.textlector.ui.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReaderScreenRoot(
    documentId : String,
    onNavigateBack : () -> Unit,
    viewModel: com.nedmah.textlector.ui.presentation.reader.ReaderViewModel = koinViewModel()
){

    val state by viewModel.state.collectAsStateWithLifecycle()

    _root_ide_package_.com.nedmah.textlector.ui.presentation.reader.ReaderScreen()

}

@Composable
fun ReaderScreen(){

}