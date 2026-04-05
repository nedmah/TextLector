package com.nedmah.textlector.ui.presentation.reader

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.Paragraph

data class ReaderState(
    val document: Document? = null,
    val paragraphs: List<Paragraph> = emptyList(),
    val fontSize: Int = 16,
    val isLoading: Boolean = false,
    val error: String? = null
)
