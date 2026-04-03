package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberFileLauncher(
    onResult: (uri: String?, mimeType: String) -> Unit
): (mimeType: String) -> Unit {
    return { mimeType ->
        val chooser = JFileChooser()
        val extension = when (mimeType) {
            "application/pdf" -> "pdf"
            "text/plain" -> "txt"
            else -> error("extension is not supported")
        }
        chooser.fileFilter = FileNameExtensionFilter(extension.uppercase(), extension)
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(chooser.selectedFile.absolutePath, mimeType)
        } else {
            onResult(null, mimeType)
        }
    }
}