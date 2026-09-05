package com.raton.kavi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KaviMint = Color(0xFF46D7A7)
val KaviBackground = Color(0xFF0B0D0C)
val KaviSurface = Color(0xFF141715)
val KaviSurfaceRaised = Color(0xFF1C201E)

private val KaviDarkColors: ColorScheme = darkColorScheme(
    primary = KaviMint,
    onPrimary = Color(0xFF002117),
    primaryContainer = Color(0xFF123B2F),
    onPrimaryContainer = Color(0xFFC5F5E4),
    background = KaviBackground,
    onBackground = Color(0xFFF1F4F2),
    surface = KaviSurface,
    onSurface = Color(0xFFF1F4F2),
    surfaceVariant = KaviSurfaceRaised,
    onSurfaceVariant = Color(0xFFB6BDB9),
    error = Color(0xFFFF6B6B)
)

@Composable
fun KaviTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaviDarkColors,
        content = content
    )
}
