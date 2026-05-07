package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nedmah.textlector.domain.model.ImportProgress
import com.nedmah.textlector.ui.presentation.components.TopBar
import com.nedmah.textlector.ui.presentation.import_from.components.FileTypeCard
import com.nedmah.textlector.ui.presentation.import_from.components.ImportRowItem
import com.nedmah.textlector.ui.presentation.import_from.components.ImportSuccessSheet
import com.nedmah.textlector.ui.presentation.import_from.components.ManualTextInput
import com.nedmah.textlector.ui.presentation.import_from.components.UrlImportSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_camera
import textlector.composeapp.generated.resources.ic_epub
import textlector.composeapp.generated.resources.ic_fb2
import textlector.composeapp.generated.resources.ic_pdf_doc
import textlector.composeapp.generated.resources.ic_text_doc
import textlector.composeapp.generated.resources.ic_url
import textlector.composeapp.generated.resources.import_analyzing
import textlector.composeapp.generated.resources.import_analyzing_text
import textlector.composeapp.generated.resources.import_from_camera
import textlector.composeapp.generated.resources.import_from_url
import textlector.composeapp.generated.resources.import_pages_progress
import textlector.composeapp.generated.resources.import_process_button
import textlector.composeapp.generated.resources.import_reading_pages
import textlector.composeapp.generated.resources.import_section_files
import textlector.composeapp.generated.resources.import_section_manual
import textlector.composeapp.generated.resources.import_subtitle
import textlector.composeapp.generated.resources.import_title
import textlector.composeapp.generated.resources.made_by

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreenRoot(
    onNavigateToReader: (String) -> Unit,
    viewModel: ImportViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val urlSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fileLauncher = rememberFileLauncher { uri, mimeType ->
        if (uri != null) {
            viewModel.onIntent(ImportIntent.FileSelected(uri, mimeType))
        }
    }

    val cameraLauncher = rememberCameraLauncher { uri ->
        viewModel.onIntent(ImportIntent.CameraImageCaptured(uri))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ImportEffect.NavigateToReader -> onNavigateToReader(effect.documentId)
                is ImportEffect.ShowError -> { /* Snackbar */
                }
            }
        }
    }

    LaunchedEffect(state.showUrlSheet) {
        if (!state.showUrlSheet) {
            urlSheetState.hide()
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(state.shouldLaunchCamera) {
        if (state.shouldLaunchCamera) {
            cameraLauncher()
            viewModel.onIntent(ImportIntent.CameraLaunched)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ImportScreen(
            state,
            viewModel::onIntent,
            onPickFile = { mimeType -> fileLauncher(mimeType) }
        )

        val document = state.processedDocument

        if (document != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(ImportIntent.DismissImport) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ImportSuccessSheet(
                    document = document.document,
                    onAddToLibrary = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            viewModel.onIntent(ImportIntent.ConfirmImport)
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            viewModel.onIntent(ImportIntent.DismissImport)
                        }
                    }
                )
            }
        }

        if (state.showUrlSheet) {
            ModalBottomSheet(
                sheetState = urlSheetState,
                onDismissRequest = { viewModel.onIntent(ImportIntent.DismissUrlSheet) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                UrlImportSheet(
                    urlText = state.urlText,
                    isLoading = state.isLoading,
                    urlError = state.urlError,
                    onUrlChange = { viewModel.onIntent(ImportIntent.EnterUrl(it)) },
                    onImport = { viewModel.onIntent(ImportIntent.ImportFromUrl) },
                    onDismiss = { viewModel.onIntent(ImportIntent.DismissUrlSheet) }
                )
            }
        }

    }
}

@Composable
private fun ImportScreen(
    state: ImportState,
    onIntent: (ImportIntent) -> Unit,
    onPickFile: (String) -> Unit,
) {

    val focusManager = LocalFocusManager.current

    val fileTypes = listOf(
        Triple(
            "PDF Document",
            "STANDARD OCR",
            Res.drawable.ic_pdf_doc
        ) to { onPickFile("application/pdf") },
        Triple(
            "Plain Text",
            "TXT / MD FILES",
            Res.drawable.ic_text_doc
        ) to { onPickFile("text/plain") },
        Triple(
            "EPUB Book",
            "EPUB FILES",
            Res.drawable.ic_epub
        ) to { onPickFile("application/epub+zip") },
        Triple(
            "FictionBook",
            "FB2 FILES",
            Res.drawable.ic_fb2
        ) to { onPickFile("application/x-fictionbook+xml") },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        TopBar(onSearchClick = {})

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Text(
                text = stringResource(Res.string.import_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.import_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Manual transcription
            Text(
                text = stringResource(Res.string.import_section_manual),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            ManualTextInput(
                text = state.manualText,
                onTextChange = { onIntent(ImportIntent.EnterText(it)) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Import Files
            Text(
                text = stringResource(Res.string.import_section_files),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(fileTypes) { (info, onClick) ->
                    FileTypeCard(
                        modifier = Modifier
                            .width(150.dp)
                            .height(160.dp),
                        title = info.first,
                        subtitle = info.second,
                        iconRes = info.third,
                        onClick = onClick
                    )
                }
            }

            when (val progress = state.importProgress) {
                is ImportProgress.Processing -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.import_reading_pages, progress.current, progress.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.current.toFloat() / progress.total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                is ImportProgress.Segmenting -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.import_analyzing_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator( // indeterminate
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Camera - stub
            ImportRowItem(
                title = stringResource(Res.string.import_from_url),
                iconRes = Res.drawable.ic_url,
                onClick = { onIntent(ImportIntent.OpenUrlSheet) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ImportRowItem(
                title = stringResource(Res.string.import_from_camera),
                iconRes = Res.drawable.ic_camera,
                onClick = { onIntent(ImportIntent.OpenCamera) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Process button
            Button(
                onClick = {
                    if (state.manualText.isNotBlank()) {
                        onIntent(ImportIntent.ImportManually)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.manualText.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                when (val progress = state.importProgress) {
                    is ImportProgress.Processing -> {
                        Text(
                            text = stringResource(Res.string.import_pages_progress,progress.current, progress.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    is ImportProgress.Segmenting -> {
                        Text(
                            text = stringResource(Res.string.import_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    else -> {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.import_process_button),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.made_by),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))


        }
    }

}
