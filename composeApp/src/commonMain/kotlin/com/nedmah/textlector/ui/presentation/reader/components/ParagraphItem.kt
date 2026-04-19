package com.nedmah.textlector.ui.presentation.reader.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nedmah.textlector.domain.model.Paragraph
import com.nedmah.textlector.ui.theme.LectorTheme
import com.nedmah.textlector.ui.theme.LocalHighlightColor

@Composable
fun ParagraphItem(
    paragraph: Paragraph,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    fontSize : Int,
    modifier: Modifier = Modifier
) {

    val highlightColor = LocalHighlightColor.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted)
            highlightColor
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "paragraph_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    color = if (isHighlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = paragraph.text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = fontSize.sp,
            color = if (isHighlighted)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onBackground,
            lineHeight = 28.sp
        )
    }
}

@Preview
@Composable
fun ParagraphItemPreview() {
    LectorTheme {
        ParagraphItem(
            paragraph = Paragraph(
                id = "1",
                documentId = "doc1",
                index = 0,
                text = "This is a sample paragraph text to demonstrate how the ParagraphItem looks in the reader view."
            ),
            isHighlighted = false,
            onClick = {},
            fontSize = 20
        )
    }
}

@Preview
@Composable
fun ParagraphItemHighlightedPreview() {
    LectorTheme {
        ParagraphItem(
            paragraph = Paragraph(
                id = "2",
                documentId = "doc1",
                index = 1,
                text = "This is a highlighted paragraph. It usually represents the paragraph currently being read aloud."
            ),
            isHighlighted = true,
            onClick = {},
            fontSize = 14
        )
    }
}
