package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable

@Composable
expect fun rememberCameraLauncher(
    onImageCaptured: (uri: String) -> Unit
) : () -> Unit