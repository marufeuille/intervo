package dev.marufeuille.intervo.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF16A34A),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF475569),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF4ADE80),
    background = Color(0xFF0F172A),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5E1),
)

@Composable
fun CompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
