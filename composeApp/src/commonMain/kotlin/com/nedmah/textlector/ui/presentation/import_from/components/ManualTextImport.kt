package com.nedmah.textlector.ui.presentation.import_from.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ManualTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text))
    }

    LaunchedEffect(text) {
        if (text != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = text)
        }
    }

    val annotatedText = buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            if (index == 0) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                ) { append(line) }
            } else {
                append(line)
            }
            if (index < lines.lastIndex) append("\n")
        }
    }

    TextField(
        value = textFieldValue.copy(annotatedString = annotatedText),
        onValueChange = { newValue ->
            textFieldValue = newValue
            onTextChange(newValue.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        placeholder = {
            Text(
                text = "Title\nPaste or type text here...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}