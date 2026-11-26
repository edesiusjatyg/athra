package id.ac.ub.athra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AthraColorScheme = darkColorScheme(
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    primary = Accent,
    onPrimary = Background,
    primaryContainer = Forest,
    onPrimaryContainer = Accent,
    secondary = Secondary,
    onSecondary = Background,
    secondaryContainer = Forest,
    onSecondaryContainer = Ink,
    tertiary = Accent,
    onTertiary = Background,
    tertiaryContainer = SoftGreen,
    onTertiaryContainer = Ink,
    outline = Border,
    outlineVariant = Border,
    surfaceVariant = Muted,
    onSurfaceVariant = Secondary,
    surfaceTint = Accent,
    surfaceDim = Background,
    surfaceBright = SoftGreen,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Muted,
    surfaceContainerHighest = SoftGreen,
    error = androidx.compose.ui.graphics.Color(0xFFF2B8AD),
    onError = Background,
)

@Composable
fun AthraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AthraColorScheme, typography = AthraTypography, content = content)
}
