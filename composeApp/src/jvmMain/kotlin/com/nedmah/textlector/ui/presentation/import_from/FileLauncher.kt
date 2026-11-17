package com.nedmah.textlector.ui.presentation.import_from

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private const val FB2_MIME = "application/x-fictionbook+xml"

@Composable
actual fun rememberFileLauncher(
    onResult: (uri: String?, mimeType: String, fileName: String?) -> Unit
): (mimeType: String) -> Unit = { requestedMimeType ->
    val extensions = extensionsFor(requestedMimeType)
    val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> extensions.any { name.lowercase().endsWith(it) } } // mac
        if (isWindows) file = extensions.joinToString(";") { "*$it" } // windows ignores mac filter
        isVisible = true // blocks until pick file or cancel
    }

    val name = dialog.file
    val dir = dialog.directory
    if (name == null || dir == null) {
        onResult(null, requestedMimeType, null)
    } else {
        val file = File(dir, name)
        onResult(file.absolutePath, resolveMimeType(file.name), file.name)
    }
}

private val isWindows = System.getProperty("os.name").lowercase().contains("win")

private fun extensionsFor(mimeType: String): List<String> = when (mimeType) {
    "application/pdf" -> listOf(".pdf")
    "text/plain" -> listOf(".txt")
    "application/epub+zip" -> listOf(".epub")
    FB2_MIME -> listOf(".fb2", ".fb2.zip")
    else -> emptyList()
}

private fun resolveMimeType(fileName: String): String {
    val name = fileName.lowercase()
    return when {
        name.endsWith(".fb2") || name.endsWith(".fb2.zip") -> FB2_MIME
        name.endsWith(".epub") -> "application/epub+zip"
        name.endsWith(".pdf") -> "application/pdf"
        name.endsWith(".txt") -> "text/plain"
        else -> ""
    }
}