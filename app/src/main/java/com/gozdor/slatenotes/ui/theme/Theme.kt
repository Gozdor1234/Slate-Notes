package com.gozdor.slatenotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF0E1630),
    primaryContainer = Color(0xFF2A3354),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondaryContainer = Color(0xFF2A3354),
    onSecondaryContainer = Color(0xFFDCE3FF),
    background = Color(0xFF16181D),
    onBackground = Color(0xFFDCDFE6),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFDCDFE6),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFF9AA0AD),
    surfaceContainer = Color(0xFF1C1F25),
    surfaceContainerHigh = Color(0xFF22262E),
    outline = Color(0xFF3A3F4B),
    outlineVariant = Color(0xFF2C3039),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5BD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF0E1A55),
    secondaryContainer = Color(0xFFE2E7FB),
    onSecondaryContainer = Color(0xFF16204A),
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1C1E23),
    surface = Color(0xFFF8F9FB),
    onSurface = Color(0xFF1C1E23),
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF5E6472),
    surfaceContainer = Color(0xFFF0F2F6),
    surfaceContainerHigh = Color(0xFFEAEDF2),
    outline = Color(0xFFC4C8D2),
    outlineVariant = Color(0xFFDDE0E7),
)

@Composable
fun SlateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
