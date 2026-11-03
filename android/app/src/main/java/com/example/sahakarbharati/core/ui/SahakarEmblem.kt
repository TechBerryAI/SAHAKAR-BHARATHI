package com.example.sahakarbharati.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small bar badge from the website Emblem: gold rim, navy ring, wheat only.
 * The full seal (with the curved name) stays on the start and sign-in pages.
 */
@Composable
fun SahakarEmblem(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 26.dp,
) {
    val saffronArc = remember {
        PathParser().parsePathString("M24.19 94.7A76 76 0 0 1 175.81 94.7").toPath()
    }
    val greenArc = remember {
        PathParser().parsePathString("M175.81 105.3A76 76 0 0 1 24.19 105.3").toPath()
    }
    val stems = remember {
        PathParser().parsePathString(
            "M100 125L100 60M105.02 125.07L83.87 69.99M94.98 125.07L116.13 69.99",
        ).toPath()
    }
    val wheat = remember { PathParser().parsePathString(WHEAT).toPath() }
    Canvas(modifier.size(emblemSize)) {
        val fit = this.size.minDimension / 200f
        scale(fit, fit, pivot = Offset.Zero) {
            drawCircle(Color(0xFFD9A441), 99f, Offset(100f, 100f))
            drawCircle(Color(0xFF163A78), 92f, Offset(100f, 100f))
            drawCircle(Color.White, 80f, Offset(100f, 100f))
            drawPath(saffronArc, Color(0xFFF08A24), style = Stroke(width = 7f))
            drawPath(greenArc, Color(0xFF138808), style = Stroke(width = 7f))
            drawCircle(Color(0xFFFFFBF2), 71f, Offset(100f, 100f))
            withTransform({
                translate(100f, 100f)
                scale(1.38f, 1.38f, pivot = Offset.Zero)
                translate(-100f, -88.2f)
            }) {
                drawPath(
                    stems,
                    Color(0xFFB8862B),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round),
                )
                drawPath(wheat, Color(0xFFF08A24))
                drawPath(wheat, Color(0xFFB8862B), style = Stroke(width = 0.8f))
                drawRoundRect(
                    Color(0xFF0E2A5C),
                    topLeft = Offset(93f, 109.5f),
                    size = Size(14f, 5.5f),
                    cornerRadius = CornerRadius(1.6f, 1.6f),
                )
            }
        }
    }
}

private const val WHEAT =
    "M100 61Q100 55.45 94.86 53.37Q94.86 58.92 100 61ZM100 61Q105.14 58.92 105.14 53.37Q100 55.45 100 61Z" +
        "M100 67.2Q100 61.65 94.86 59.57Q94.86 65.12 100 67.2ZM100 67.2Q105.14 65.12 105.14 59.57Q100 61.65 100 67.2Z" +
        "M100 73.4Q100 67.85 94.86 65.77Q94.86 71.32 100 73.4ZM100 73.4Q105.14 71.32 105.14 65.77Q100 67.85 100 73.4Z" +
        "M100 79.6Q100 74.05 94.86 71.97Q94.86 77.52 100 79.6ZM100 79.6Q105.14 77.52 105.14 71.97Q100 74.05 100 79.6Z" +
        "M100 85.8Q100 80.25 94.86 78.17Q94.86 83.72 100 85.8ZM100 85.8Q105.14 83.72 105.14 78.17Q100 80.25 100 85.8Z" +
        "M100 62Q103 56.75 100 51.5Q97 56.75 100 62Z" +
        "M84.23 70.92Q82.24 65.74 76.7 65.65Q78.69 70.82 84.23 70.92ZM84.23 70.92Q88.29 67.14 86.3 61.96Q82.25 65.74 84.23 70.92Z" +
        "M86.45 76.71Q84.46 71.53 78.92 71.43Q80.91 76.61 86.45 76.71ZM86.45 76.71Q90.51 72.93 88.52 67.75Q84.47 71.53 86.45 76.71Z" +
        "M88.68 82.5Q86.69 77.32 81.14 77.22Q83.13 82.4 88.68 82.5ZM88.68 82.5Q92.73 78.71 90.75 73.53Q86.69 77.32 88.68 82.5Z" +
        "M90.9 88.29Q88.91 83.11 83.36 83.01Q85.35 88.19 90.9 88.29ZM90.9 88.29Q94.95 84.5 92.97 79.32Q88.91 83.11 90.9 88.29Z" +
        "M93.12 94.08Q91.13 88.9 85.58 88.8Q87.57 93.98 93.12 94.08ZM93.12 94.08Q97.17 90.29 95.19 85.11Q91.13 88.9 93.12 94.08Z" +
        "M84.59 71.86Q85.51 65.88 80.83 62.05Q79.91 68.03 84.59 71.86Z" +
        "M115.77 70.92Q117.75 65.74 113.7 61.96Q111.71 67.14 115.77 70.92ZM115.77 70.92Q121.31 70.82 123.3 65.65Q117.76 65.74 115.77 70.92Z" +
        "M113.55 76.71Q115.53 71.53 111.48 67.75Q109.49 72.93 113.55 76.71ZM113.55 76.71Q119.09 76.61 121.08 71.43Q115.54 71.53 113.55 76.71Z" +
        "M111.32 82.5Q113.31 77.32 109.25 73.53Q107.27 78.71 111.32 82.5ZM111.32 82.5Q116.87 82.4 118.86 77.22Q113.31 77.32 111.32 82.5Z" +
        "M109.1 88.29Q111.09 83.11 107.03 79.32Q105.05 84.5 109.1 88.29ZM109.1 88.29Q114.65 88.19 116.64 83.01Q111.09 83.11 109.1 88.29Z" +
        "M106.88 94.08Q108.87 88.9 104.81 85.11Q102.83 90.29 106.88 94.08ZM106.88 94.08Q112.43 93.98 114.42 88.8Q108.87 88.9 106.88 94.08Z" +
        "M115.41 71.86Q120.09 68.03 119.17 62.05Q114.49 65.88 115.41 71.86Z"
