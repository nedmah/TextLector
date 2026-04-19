package com.nedmah.textlector.ui.presentation.import_from

import com.nedmah.textlector.domain.model.ImportProgress
import com.nedmah.textlector.domain.model.ProcessedDocument

data class ImportState(
    val manualText: String = "",
    val urlText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showUrlSheet: Boolean = false,
    val importProgress: ImportProgress? = null,
    val processedDocument: ProcessedDocument? = null
)
