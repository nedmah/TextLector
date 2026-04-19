package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import org.koin.compose.viewmodel.koinViewModel
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.ic_camera
import textlector.composeapp.generated.resources.ic_pdf_doc
import textlector.composeapp.generated.resources.ic_text_doc
import textlector.composeapp.generated.resources.ic_url

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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ImportEffect.NavigateToReader -> onNavigateToReader(effect.documentId)
                is ImportEffect.ShowError -> { /* Snackbar */ }
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
                text = "Import",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bring your physical books and digital documents into your personal library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Manual transcription
            Text(
                text = "MANUAL TRANSCRIPTION",
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
                text = "IMPORT FILES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                FileTypeCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "PDF Document",
                    subtitle = "STANDARD OCR",
                    iconRes = Res.drawable.ic_pdf_doc,
                    onClick = {
                        onPickFile("application/pdf")
                    }
                )
                FileTypeCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "Plain Text",
                    subtitle = "TXT / MD FILES",
                    iconRes = Res.drawable.ic_text_doc,
                    onClick = {
                        onPickFile("text/plain")
                    }
                )
            }

            when (val progress = state.importProgress) {
                is ImportProgress.Processing -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reading ${progress.current} of ${progress.total} pages...",
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
                            text = "Analyzing text...",
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

            // URL и Camera — stubs
            ImportRowItem(
                title = "Import from URL",
                iconRes = Res.drawable.ic_url,
                onClick = { onIntent(ImportIntent.OpenUrlSheet) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ImportRowItem(
                title = "Scan via Camera",
                iconRes = Res.drawable.ic_camera,
                onClick = { /* v2 */ }
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
                            text = "${progress.current}/${progress.total} pages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    is ImportProgress.Segmenting -> {
                        Text(
                            text = "Analyzing...",
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
                                text = "Process Document",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PROCESSED VIA TEXTLECTOR ENGINE 4.2",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))


        }
    }

}
