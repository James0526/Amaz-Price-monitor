package com.example.amazpricetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6F00),
    secondary = Color(0xFF1976D2),
    tertiary = Color(0xFF388E3C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    secondary = Color(0xFF90CAF9),
    tertiary = Color(0xFFA5D6A7)
)

@Composable
fun AmazPriceMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
