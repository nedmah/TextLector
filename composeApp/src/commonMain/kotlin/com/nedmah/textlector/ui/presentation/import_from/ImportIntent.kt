package com.nedmah.textlector.ui.presentation.import_from

interface ImportIntent {
    data class EnterText(val text: String) :
        com.nedmah.textlector.ui.presentation.import_from.ImportIntent
    data class EnterTitle(val title: String) :
        com.nedmah.textlector.ui.presentation.import_from.ImportIntent
    data class ImportFile(val uri: String, val mimeType: String) :
        com.nedmah.textlector.ui.presentation.import_from.ImportIntent
    data object ImportManually : com.nedmah.textlector.ui.presentation.import_from.ImportIntent
    data object DismissError : com.nedmah.textlector.ui.presentation.import_from.ImportIntent
    data object ConfirmImport :
        com.nedmah.textlector.ui.presentation.import_from.ImportIntent // bottom sheet
    data object DismissImport :
        com.nedmah.textlector.ui.presentation.import_from.ImportIntent // bottom sheet
}