package com.nedmah.textlector.ui.presentation.player

import com.nedmah.textlector.domain.model.Document
import com.nedmah.textlector.domain.model.Paragraph

data class PlayerState(
    val document: Document? = null,
    val paragraphs: List<Paragraph> = emptyList(),
    val currentParagraphIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackSpeed: Float = 1f,
) {
    val currentParagraph: Paragraph?
        get() = paragraphs.getOrNull(currentParagraphIndex)

    val progress: Float
        get() = if (paragraphs.isEmpty()) 0f
        else currentParagraphIndex.toFloat() / (paragraphs.size - 1)

    val isLoaded: Boolean
        get() = document != null && paragraphs.isNotEmpty()
}
