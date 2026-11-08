package com.auris.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pearl greys from the eardrum, scrub teal for actions, otoscope-light amber for caution.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7EAE5),
    onPrimaryContainer = Color(0xFF06302B),
    secondary = Color(0xFF4F605C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E9E5),
    onSecondaryContainer = Color(0xFF182624),
    tertiary = Color(0xFF8A560A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E7C8),
    onTertiaryContainer = Color(0xFF4A2E05),
    error = Color(0xFFA3302A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF8E0DC),
    onErrorContainer = Color(0xFF5C1712),
    background = Color(0xFFEEF1EE),
    onBackground = Color(0xFF182624),
    surface = Color(0xFFEEF1EE),
    onSurface = Color(0xFF182624),
    surfaceVariant = Color(0xFFE3E9E5),
    onSurfaceVariant = Color(0xFF4F605C),
    outline = Color(0xFFB8C4BE),
    outlineVariant = Color(0xFFCCD6D1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F8F6),
    surfaceContainer = Color(0xFFFBFCFA),
    surfaceContainerHigh = Color(0xFFF1F4F2),
    surfaceContainerHighest = Color(0xFFE8EDEA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF45B9A7),
    onPrimary = Color(0xFF06211D),
    primaryContainer = Color(0xFF183833),
    onPrimaryContainer = Color(0xFFBDEBE2),
    secondary = Color(0xFF9CB0AA),
    onSecondary = Color(0xFF0F1716),
    secondaryContainer = Color(0xFF22302C),
    onSecondaryContainer = Color(0xFFE5ECE9),
    tertiary = Color(0xFFE4AA4E),
    onTertiary = Color(0xFF2B1B02),
    tertiaryContainer = Color(0xFF382C15),
    onTertiaryContainer = Color(0xFFF6E1B8),
    error = Color(0xFFF08B81),
    onError = Color(0xFF3C0905),
    errorContainer = Color(0xFF3C201D),
    onErrorContainer = Color(0xFFF8DCD8),
    background = Color(0xFF0F1716),
    onBackground = Color(0xFFE5ECE9),
    surface = Color(0xFF0F1716),
    onSurface = Color(0xFFE5ECE9),
    surfaceVariant = Color(0xFF22302C),
    onSurfaceVariant = Color(0xFF9CB0AA),
    outline = Color(0xFF3A4C47),
    outlineVariant = Color(0xFF2D3D39),
    surfaceContainerLowest = Color(0xFF0A1110),
    surfaceContainerLow = Color(0xFF131C1A),
    surfaceContainer = Color(0xFF172220),
    surfaceContainerHigh = Color(0xFF1C2826),
    surfaceContainerHighest = Color(0xFF22302C)
)

@Composable
fun AurisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
