package com.example.streamfilx_androidtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val StreamFlixColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = StreamFlixOnSurface,
    primaryContainer = NetflixRedDark,
    onPrimaryContainer = StreamFlixOnSurface,
    secondary = StreamFlixOnSurfaceVariant,
    onSecondary = StreamFlixBackground,
    background = StreamFlixBackground,
    onBackground = StreamFlixOnSurface,
    surface = StreamFlixSurface,
    onSurface = StreamFlixOnSurface,
    surfaceVariant = StreamFlixSurfaceVariant,
    onSurfaceVariant = StreamFlixOnSurfaceVariant,
    border = StreamFlixBorder,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamFlixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StreamFlixColorScheme,
        typography = StreamFlixTypography,
        content = content,
    )
}
