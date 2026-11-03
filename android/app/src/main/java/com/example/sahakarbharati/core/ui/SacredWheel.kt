package com.example.sahakarbharati.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    SahakarLogo(modifier = modifier, size = size)
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
    SahakarSeal(
        modifier = modifier,
        size = size,
        compact = size < 64.dp,
    )
}

@Composable
fun SacredWheelSeal(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    SahakarSeal(
        modifier = modifier,
        size = size,
        compact = true,
    )
}

/** Website start-screen seal: gold halo rings around the emblem. */
@Composable
fun StartSeal(
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
) {
    val halo = size * 196f / 148f
    Box(
        modifier = modifier
            .size(halo)
            .semantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x47D9A441), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(halo - 20.dp)
                .border(1.dp, Color(0x80C9963A), CircleShape),
        )
        SahakarSeal(size = size)
    }
}
