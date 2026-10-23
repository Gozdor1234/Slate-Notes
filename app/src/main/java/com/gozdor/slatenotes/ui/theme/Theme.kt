package com.gozdor.slatenotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.gozdor.slatenotes.data.SlateSettings

/** Accent choices shown in Settings > Appearance. */
val AccentPresets: List<Pair<String, Long>> = listOf(
    "Slate blue" to 0xFF5B6EE1,
    "Violet" to 0xFF8B6CEF,
    "Teal" to 0xFF1A9E95,
    "Green" to 0xFF3E9B4F,
    "Amber" to 0xFFD08A1E,
    "Coral" to 0xFFE0614F,
    "Pink" to 0xFFD9578F,
    "Graphite" to 0xFF6B7280,
)

private val DarkBase = darkColorScheme(
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFDADADA),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFDADADA),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFF9B9B9B),
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF202020),
    surfaceContainer = Color(0xFF262626),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF363636),
)

private val LightBase = lightColorScheme(
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF222222),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF6B6B6B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    outline = Color(0xFFC8C8C8),
    outlineVariant = Color(0xFFE3E3E3),
)

@Composable
fun SlateTheme(settings: SlateSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val base = if (dark) DarkBase else LightBase
    val accent = Color(settings.accent)
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val container = lerp(accent, base.background, if (dark) 0.7f else 0.82f)
    val onContainer = if (dark) lerp(accent, Color.White, 0.75f) else lerp(accent, Color.Black, 0.55f)
    val scheme = base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
