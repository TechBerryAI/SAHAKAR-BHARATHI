package com.example.sahakarbharati.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Suppress("UNUSED_PARAMETER")
@Composable
fun SacredWheel(
    modifier: Modifier = Modifier,
    size: Dp = 144.dp,
    tint: Color = RathYatraColors.GoldMuted,
    hubColor: Color = RathYatraColors.Navy,
    spokeColor: Color = RathYatraColors.SaffronBright,
    showTemple: Boolean = true,
) {
    SahakarLogo(
        modifier = modifier,
        size = size,
        ring = tint,
        disc = hubColor,
        accent = spokeColor,
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SahakarLogo(
    modifier: Modifier = Modifier,
    size: Dp = 144.dp,
    ring: Color = RathYatraColors.GoldMuted,
    disc: Color = RathYatraColors.Navy,
    accent: Color = RathYatraColors.SaffronBright,
    showHarvest: Boolean = true,
) {
    Image(
        painter = painterResource(R.drawable.ic_sahakar_logo),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .semantics { hideFromAccessibility() },
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun SacredWheelSeal(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(RathYatraColors.White, CircleShape)
            .semantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        SahakarLogo(size = size * 0.92f)
    }
}
