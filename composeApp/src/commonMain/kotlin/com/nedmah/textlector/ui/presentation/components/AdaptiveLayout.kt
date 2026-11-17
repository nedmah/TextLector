package com.nedmah.textlector.ui.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// material "expanded" window width
private val WideLayoutMinWidth = 840.dp
private val ReadableMaxWidth = 720.dp

// layout depends on the window width, not on the os
val LocalWideLayout = compositionLocalOf { false }

@Composable
fun ProvideWideLayout(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(LocalWideLayout provides (maxWidth >= WideLayoutMinWidth)) {
            content()
        }
    }
}

@Composable
fun ReadableWidthBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ReadableMaxWidth).fillMaxHeight()) {
            content()
        }
    }
}