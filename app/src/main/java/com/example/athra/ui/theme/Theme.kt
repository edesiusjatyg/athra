package com.example.athra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AthraColorScheme = darkColorScheme(
    background       = Background,
    surface          = Surface,
    onBackground     = Ink,
    onSurface        = Ink,
    primary          = Accent,
    onPrimary        = Background,
    secondary        = Secondary,
    onSecondary      = Background,
    outline          = Border,
    surfaceVariant   = SoftGreen,
    onSurfaceVariant = Secondary,
)

@Composable
fun AthraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AthraColorScheme,
        typography  = AthraTypography,
        content     = content,
    )
}
