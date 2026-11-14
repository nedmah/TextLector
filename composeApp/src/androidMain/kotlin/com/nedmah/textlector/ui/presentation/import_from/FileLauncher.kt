package com.nedmah.textlector.ui.presentation.import_from

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val FB2_MIME = "application/x-fictionbook+xml"

@Composable
actual fun rememberFileLauncher(
    onResult: (uri: String?, mimeType: String, fileName: String?) -> Unit
): (mimeType: String) -> Unit {
    val context = LocalContext.current

    // result may arrive in a new composition or even a recreated Activity,
    // so the type is resolved from the picked file itself, not remembered from the request
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onResult(null, "", null)
        } else {
            val name = context.displayName(uri)
            onResult(uri.toString(), context.resolveMimeType(uri, name), name)
        }
    }

    return { mimeType ->
        launcher.launch(pickerFilter(mimeType))
    }
}

// most file providers don't know .fb2 and report it as application/octet-stream or xml
// so a strict filter hides FB2 files
private fun pickerFilter(mimeType: String): Array<String> =
    if (mimeType == FB2_MIME) arrayOf("*/*") else arrayOf(mimeType)

private fun Context.resolveMimeType(uri: Uri, displayName: String?): String {
    val name = displayName?.lowercase().orEmpty()
    return when {
        name.endsWith(".fb2") || name.endsWith(".fb2.zip") -> FB2_MIME
        name.endsWith(".epub") -> "application/epub+zip"
        name.endsWith(".pdf") -> "application/pdf"
        name.endsWith(".txt") -> "text/plain"
        else -> contentResolver.getType(uri).orEmpty()
    }
}

private fun Context.displayName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }