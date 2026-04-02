package com.nedmah.textlector.ui.presentation.import_from

import com.nedmah.textlector.domain.model.Document

data class ImportState(
    val manualText: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val importedDocument: Document? = null
)
