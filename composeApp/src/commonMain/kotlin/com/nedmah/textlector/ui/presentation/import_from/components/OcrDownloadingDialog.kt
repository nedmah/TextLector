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
                    is ModelState.Error -> "Download failed"
                    else -> "Camera scanning"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when (state) {
                        is ModelState.NotDownloaded ->
                            "To scan text with your camera, recognition data needs to be downloaded (~20 MB)."
                        is ModelState.Downloading ->
                            "Downloading recognition data..."
                        is ModelState.Error ->
                            "Failed to download: ${state.message}"
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
                        text = if (state is ModelState.Error) "Retry" else "Download (~20 MB)"
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