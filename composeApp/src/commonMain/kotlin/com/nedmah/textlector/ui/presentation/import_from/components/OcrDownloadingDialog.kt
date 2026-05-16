package com.nedmah.textlector.ui.presentation.import_from.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nedmah.textlector.domain.model.ModelState
import org.jetbrains.compose.resources.stringResource
import textlector.composeapp.generated.resources.Res
import textlector.composeapp.generated.resources.action_retry
import textlector.composeapp.generated.resources.import_ocr_download
import textlector.composeapp.generated.resources.import_ocr_download_error
import textlector.composeapp.generated.resources.import_ocr_download_failed
import textlector.composeapp.generated.resources.import_ocr_downloading
import textlector.composeapp.generated.resources.import_ocr_subtitle
import textlector.composeapp.generated.resources.import_ocr_title

@Composable
fun OcrDownloadDialog(
    state: ModelState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (state !is ModelState.Downloading) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = when (state) {
                    is ModelState.Error -> stringResource(Res.string.import_ocr_download_failed)
                    else -> stringResource(Res.string.import_ocr_title)
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when (state) {
                        is ModelState.NotDownloaded ->
                            stringResource(Res.string.import_ocr_subtitle)

                        is ModelState.Downloading ->
                            stringResource(Res.string.import_ocr_downloading)

                        is ModelState.Error ->
                            stringResource(Res.string.import_ocr_download_error, state.message)

                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state is ModelState.Downloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (state !is ModelState.Downloading) {
                Button(onClick = onDownload) {
                    Text(
                        text = if (state is ModelState.Error) stringResource(Res.string.action_retry)
                        else stringResource(Res.string.import_ocr_download)
                    )
                }
            }
        },
        dismissButton = {
            if (state !is ModelState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}