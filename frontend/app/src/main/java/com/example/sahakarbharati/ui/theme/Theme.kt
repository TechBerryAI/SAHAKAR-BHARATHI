package com.example.sahakarbharati.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)

private val StitchColorScheme = lightColorScheme(
    primary = RathYatraColors.Navy,
    onPrimary = RathYatraColors.White,
    primaryContainer = RathYatraColors.Navy,
    onPrimaryContainer = RathYatraColors.OnNavyMuted,
    secondary = RathYatraColors.Saffron,
    onSecondary = RathYatraColors.White,
    secondaryContainer = RathYatraColors.SaffronBright,
    onSecondaryContainer = RathYatraColors.BurntSaffron,
    tertiary = RathYatraColors.GoldDark,
    onTertiary = RathYatraColors.White,
    tertiaryContainer = RathYatraColors.Gold,
    onTertiaryContainer = RathYatraColors.NavyDeep,
    background = RathYatraColors.Ivory,
    onBackground = RathYatraColors.OnSurface,
    surface = RathYatraColors.Ivory,
    onSurface = RathYatraColors.OnSurface,
    surfaceVariant = RathYatraColors.SurfaceContainerHighest,
    onSurfaceVariant = RathYatraColors.OnSurfaceVariant,
    outline = RathYatraColors.Outline,
    outlineVariant = RathYatraColors.SurfaceDim,
    error = RathYatraColors.Emergency,
    onError = RathYatraColors.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    inverseSurface = RathYatraColors.Navy,
    inverseOnSurface = RathYatraColors.Ivory,
    inversePrimary = RathYatraColors.SaffronBright,
    surfaceTint = RathYatraColors.Navy,
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun SahakarBharatiTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Stitch screenshots are the visual source of truth. Dynamic/system
    // colors would replace the ceremonial navy / ivory / saffron palette.
    MaterialTheme(
        colorScheme = StitchColorScheme,
        typography = Typography,
        shapes = RathYatraShapes,
        content = content,
    )
}
