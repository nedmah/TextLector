package com.nedmah.textlector.ui.presentation.import_from

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFileLauncher(
    onResult: (uri: String?, mimeType: String) -> Unit
): (mimeType: String) -> Unit {
    var currentMimeType = remember { "application/pdf" }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        onResult(uri?.toString(), currentMimeType)
    }

    return { mimeType ->
        currentMimeType = mimeType
        launcher.launch(arrayOf(mimeType))
    }
}