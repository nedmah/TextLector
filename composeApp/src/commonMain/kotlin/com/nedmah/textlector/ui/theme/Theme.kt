package com.nedmah.textlector.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LectorBlue,
    onPrimary = Color.White,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LectorBlue,
    onPrimary = Color.White,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

val LocalHighlightColor = compositionLocalOf<Color> { HighlightLight }

@Composable
fun LectorTheme(
    isDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
    val highlightColor = if (isDarkMode) HighlightDark else HighlightLight

    val animatedBackground by animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(durationMillis = 400),
        label = "background"
    )
    val animatedSurface by animateColorAsState(
        targetValue = colorScheme.surface,
        animationSpec = tween(durationMillis = 400),
        label = "surface"
    )
    val animatedOnBackground by animateColorAsState(
        targetValue = colorScheme.onBackground,
        animationSpec = tween(durationMillis = 400),
        label = "onBackground"
    )

    val animatedColorScheme = colorScheme.copy(
        background = animatedBackground,
        surface = animatedSurface,
        onBackground = animatedOnBackground
    )

    CompositionLocalProvider(LocalHighlightColor provides highlightColor) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = LectorTypography,
            content = content
        )
    }
}