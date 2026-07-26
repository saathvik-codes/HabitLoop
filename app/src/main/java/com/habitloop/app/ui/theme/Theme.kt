package com.habitloop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light is the primary, designed theme — a warm "paper" neutral instead of
// Material's default cool-white/lavender-tinted scheme, which reads as
// generic and clashes with the warm orange brand mark used across every
// icon and illustration in the app. Dark exists for system-preference users
// but isn't the design's home base the way it was during the MVP pass.
private val LightColors = lightColorScheme(
    primary = Color(0xFF84A98C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4EEE6),
    onPrimaryContainer = Color(0xFF36523D),
    secondary = Color(0xFFF6B89E),
    background = Color(0xFFFAF8F4),
    onBackground = Color(0xFF2F2F2F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2F2F2F),
    surfaceVariant = Color(0xFFF3EFE7),
    onSurfaceVariant = Color(0xFF767676),
    outline = Color(0xFFEAE4D8)
)

@Composable
fun HabitLoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = HabitLoopTypography,
        shapes = HabitLoopShapes,
        content = content
    )
}
