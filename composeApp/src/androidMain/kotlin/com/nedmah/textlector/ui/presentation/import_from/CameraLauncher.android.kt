package com.nedmah.textlector.ui.presentation.import_from

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (uri: String) -> Unit): () -> Unit {
    val context = LocalContext.current

    val tempUri = remember {
        val file = File(context.cacheDir, "camera_ocr_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) onImageCaptured(tempUri.toString())
    }

    return remember { { launcher.launch(tempUri) } }
}