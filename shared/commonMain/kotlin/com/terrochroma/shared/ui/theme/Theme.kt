package com.terrochroma.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Cream,
    primaryContainer = MidGreen,
    onPrimaryContainer = Cream,
    secondary = Olive,
    onSecondary = Cream,
    secondaryContainer = MidSoil,
    onSecondaryContainer = Cream,
    tertiary = SoilBrown,
    onTertiary = Cream,
    tertiaryContainer = DeepSoil,
    onTertiaryContainer = Cream,
    error = Color(0xFFB71C1C),
    onError = Cream,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    background = Cream,
    onBackground = DeepSoil,
    surface = WarmBeige,
    onSurface = DeepSoil,
    surfaceVariant = MidSoil,
    onSurfaceVariant = Cream,
    outline = MidSoil,
    outlineVariant = Olive,
    scrim = DeepSoil,
    inverseSurface = DeepSoil,
    inverseOnSurface = Cream,
    inversePrimary = MidGreen
)

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreen,
    onPrimary = DarkCream,
    primaryContainer = MidGreen,
    onPrimaryContainer = DarkCream,
    secondary = Olive,
    onSecondary = DarkCream,
    secondaryContainer = DarkMidSoil,
    onSecondaryContainer = DarkCream,
    tertiary = DarkSoilBrown,
    onTertiary = DarkCream,
    tertiaryContainer = DarkDeepSoil,
    onTertiaryContainer = DarkCream,
    error = Color(0xFFCF6679),
    onError = DarkCream,
    errorContainer = Color(0xFF5C0002),
    onErrorContainer = Color(0xFFFFB4AB),
    background = DarkCream,
    onBackground = Cream,
    surface = DarkWarmBeige,
    onSurface = Cream,
    surfaceVariant = DarkMidSoil,
    onSurfaceVariant = Cream,
    outline = DarkMidSoil,
    outlineVariant = Olive,
    scrim = Color(0xFF000000),
    inverseSurface = Cream,
    inverseOnSurface = DeepSoil,
    inversePrimary = MidGreen
)

@Composable
fun TerroChromaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
