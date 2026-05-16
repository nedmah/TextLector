package com.nedmah.textlector.ui.presentation.import_from

interface ImportIntent {
    data class EnterText(val text: String) :
        ImportIntent

    data class FileSelected(val uri: String, val mimeType: String) : ImportIntent
    data object ImportManually : ImportIntent
    data object DismissError : ImportIntent
    data object ConfirmImport :
        ImportIntent // bottom sheet

    data object DismissImport :
        ImportIntent // bottom sheet

    data object OpenUrlSheet : ImportIntent
    data object DismissUrlSheet : ImportIntent
    data class EnterUrl(val url: String) : ImportIntent
    data object ImportFromUrl : ImportIntent

    data object OpenCamera : ImportIntent
    data object CameraLaunched : ImportIntent
    data class CameraImageCaptured(val uri: String) : ImportIntent
    data object DownloadOcrData : ImportIntent
    data object DismissOcrDialog : ImportIntent
}