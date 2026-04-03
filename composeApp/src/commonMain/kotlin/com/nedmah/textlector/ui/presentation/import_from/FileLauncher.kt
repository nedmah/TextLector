package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFileLauncher(onResult: (uri: String?, mimeType: String) -> Unit): (mimeType: String) -> Unit