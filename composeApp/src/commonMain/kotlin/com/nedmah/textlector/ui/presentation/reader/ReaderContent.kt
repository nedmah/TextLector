package com.nedmah.textlector.ui.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.ui.presentation.reader.components.ParagraphItem

@Composable
fun ReaderContent(
    paragraphs: List<Paragraph>,
    currentParagraphIndex: Int,
    isPlaying: Boolean,
    fontSize: Int,
    onParagraphClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val listState = rememberLazyListState()

    LaunchedEffect(currentParagraphIndex, isPlaying) {
        if (isPlaying) {
            listState.animateScrollToItem(
                index = currentParagraphIndex,
                scrollOffset = -200
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 160.dp
        )
    ) {
        itemsIndexed(
            items = paragraphs,
            key = { _, paragraph -> paragraph.id }
        ) { index, paragraph ->
            ParagraphItem(
                paragraph = paragraph,
                isHighlighted = index == currentParagraphIndex,
                fontSize = fontSize,
                onClick = { onParagraphClick(index) }
            )
        }
    }

}