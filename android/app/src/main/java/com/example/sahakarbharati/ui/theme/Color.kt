package com.example.sahakarbharati.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared palette with the website (`web/src/index.css`).
 * Cream pages, navy titles, saffron pills, gold card lines.
 */
object RathYatraColors {
    val Ivory = Color(0xFFF6EFE4)
    val Cream = Color(0xFFFFFDF9)
    val CreamEnd = Color(0xFFF6EFE4)
    val SoftWhite = Color(0xFFF3EBE1)
    val Paper = Color(0xFFFFFCF8)
    val White = Color(0xFFFFFCF8)
    val Navy = Color(0xFF17263F)
    val TitleNavy = Color(0xFF163A78)
    val NavyDeep = Color(0xFF021129)
    val NavyHero = Color(0xFF0C1B34)
    val Saffron = Color(0xFFE9872A)
    val SaffronBright = Color(0xFFF4A24A)
    val SaffronTop = Color(0xFFF09A3C)
    val SaffronBottom = Color(0xFFE07A1C)
    val SaffronPressed = Color(0xFFC56A16)
    val AppBarTop = Color(0xFFF3A24A)
    val AppBarBottom = Color(0xFFE18322)
    val Gold = Color(0xFFC9963A)
    val GoldMuted = Color(0xFFE8C179)
    val GoldPale = Color(0xFFFFDEA5)
    val GoldDark = Color(0xFFA86A22)
    val GoldLine = Color(0x47A87A34)
    val HonourGold = Color(0xFFA86A22)
    val SubtitleBurnt = Color(0xFFB85A0B)
    val Success = Color(0xFF2E7D5B)
    val DeepGreen = Color(0xFF0E3328)
    val DeepGreenSoft = Color(0xFF1B5A44)
    val AvatarGreenTop = Color(0xFF1D6248)
    val Emergency = Color(0xFFC93B2B)
    val OnSurface = Color(0xFF1C1915)
    val OnSurfaceVariant = Color(0xFF6A6258)
    val OnSurfaceVariantCool = Color(0xFF6A6258)
    val Outline = Color(0xFFE4D6C4)
    val OutlineCool = Color(0xFF75777E)
    val SurfaceContainer = Color(0xFFF4EDE2)
    val SurfaceContainerLow = Color(0xFFFAF3E8)
    val SurfaceContainerHigh = Color(0xFFEEE7DD)
    val SurfaceContainerHighest = Color(0xFFE8E2D7)
    val SurfaceDim = Color(0xFFE0D9CF)
    val PrimaryFixed = Color(0xFFFFDCC3)
    val SecondaryPale = Color(0xFFFFF1DF)
    val OnNavyMuted = Color(0xFF7F8DAB)
    val NavyInk = Color(0xFF0C1B34)
    val BurntSaffron = Color(0xFFB85A0B)
    val ButtonShadow = Color(0x3DB05612)
    val ChipSaffronBg = Color(0x24E9872A)
    val ChipGreenBg = Color(0x1A0F3D2E)

    val SaffronButtonBrush: Brush
        get() = Brush.verticalGradient(listOf(SaffronTop, SaffronBottom))
    val AppBarBrush: Brush
        get() = Brush.verticalGradient(listOf(AppBarTop, AppBarBottom))
    val ScreenBrush: Brush
        get() = Brush.verticalGradient(listOf(Cream, CreamEnd))
}

val Ivory = RathYatraColors.Ivory
val CeremonialNavy = RathYatraColors.Navy
val SacredSaffron = RathYatraColors.Saffron
val MutedGold = RathYatraColors.Gold
val ForestGreen = RathYatraColors.Success
val CrimsonRed = RathYatraColors.Emergency
