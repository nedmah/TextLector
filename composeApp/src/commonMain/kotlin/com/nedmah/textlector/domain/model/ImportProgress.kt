package com.nedmah.textlector.domain.model

sealed class ImportProgress {
    data class Success(val processedDocument: ProcessedDocument) : ImportProgress()
    data class Processing(val current: Int, val total: Int) : ImportProgress()
    data object Segmenting : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}