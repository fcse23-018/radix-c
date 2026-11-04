package com.soilcarbon.engine.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// TerroChroma-aligned palette: warm cream paper, deep soil brown, living leaf green.
val SoilDeep = Color(0xFF2B211B)
val SoilBrown = Color(0xFF5D4037)
val LeafGreen = Color(0xFF2E7D32)
val Olive = Color(0xFF689F38)
val Cream = Color(0xFFF5F5F0)
val WarmBeige = Color(0xFFEFEBE9)
val Charcoal = Color(0xFF1A1D20)
val Amber = Color(0xFFF9A825)
val Rust = Color(0xFFB23A2E)
val Sky = Color(0xFF2F6F8F)
val Clay = Color(0xFFB07A5A)

private val LightColors = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBCB),
    onPrimaryContainer = Color(0xFF14320F),
    secondary = SoilBrown,
    onSecondary = Color.White,
    secondaryContainer = WarmBeige,
    onSecondaryContainer = SoilDeep,
    tertiary = Sky,
    onTertiary = Color.White,
    background = Cream,
    onBackground = SoilDeep,
    surface = Color(0xFFFDFCF8),
    onSurface = SoilDeep,
    surfaceVariant = WarmBeige,
    onSurfaceVariant = Color(0xFF5B5148),
    error = Rust,
    onError = Color.White,
    outline = Color(0xFFB5A99B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD67F),
    onPrimary = Color(0xFF12300C),
    primaryContainer = Color(0xFF2C4B22),
    onPrimaryContainer = Color(0xFFD7EBCB),
    secondary = Color(0xFFD3B8A8),
    onSecondary = Color(0xFF3A2A22),
    secondaryContainer = Color(0xFF4A3A31),
    onSecondaryContainer = Color(0xFFF0E3D8),
    tertiary = Color(0xFF8FC7DF),
    onTertiary = Color(0xFF0B2B39),
    background = Color(0xFF141210),
    onBackground = Color(0xFFEDE7E0),
    surface = Color(0xFF1D1A17),
    onSurface = Color(0xFFEDE7E0),
    surfaceVariant = Color(0xFF2B2622),
    onSurfaceVariant = Color(0xFFCFC4B8),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5F150B),
    outline = Color(0xFF7A6E63)
)

@Composable
fun SoilCarbonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Utility for screens that need the legacy View accent. */
fun Activity.accentColor(): Int = 0xFF2E7D32.toInt()
