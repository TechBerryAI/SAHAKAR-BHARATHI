package com.example.sahakarbharati.core.ui

import android.graphics.Paint
import android.graphics.PathMeasure
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import android.graphics.Path as AndroidPath

/**
 * Website seal (`web/src/components/Seal.tsx`): gold rim, navy letter-band,
 * saffron-and-green ring, wheat over a fish and waves.
 *
 * [compact] matches the small app-bar badge (`Emblem.tsx`) — no lettering.
 */
@Composable
fun SahakarSeal(
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    compact: Boolean = false,
) {
    val paints = remember { SealPaints() }
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { hideFromAccessibility() },
    ) {
        // DrawScope.size is the pixel Size; the Dp param above must not shadow it.
        val scale = this.size.minDimension / 200f
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.scale(scale, scale)
            if (compact) {
                SealGeometry.drawCompact(native, paints)
            } else {
                SealGeometry.drawFull(native, paints)
            }
            native.restore()
        }
    }
}

private class SealPaints {
    val goldFill = fill(0xFFD9A441.toInt())
    val navyFill = fill(0xFF163A78.toInt())
    val whiteFill = fill(0xFFFFFFFF.toInt())
    val creamFill = fill(0xFFFFFBF2.toInt())
    val saffronStroke = stroke(0xFFF08A24.toInt(), 3.6f)
    val greenStroke = stroke(0xFF138808.toInt(), 3.6f)
    val darkGreenStroke = stroke(0xFF0F6B1C.toInt(), 1.6f)
    val greenFill = fill(0xFF138808.toInt())
    val waveStroke = stroke(0xFF1F5FA8.toInt(), 3f)
    val waveStrokeSoft = stroke(0xFF1F5FA8.toInt(), 2.4f).also { it.alpha = (0.75f * 255).toInt() }
    val fishFill = fill(0xFF0E2A5C.toInt())
    val wheatStroke = stroke(0xFFB8862B.toInt(), 1.8f)
    val wheatFill = fill(0xFFF08A24.toInt())
    val wheatOutline = stroke(0xFFB8862B.toInt(), 0.6f)
    val goldHairline = stroke(0xFFD9A441.toInt(), 0.8f)
    val compactSaffron = stroke(0xFFF08A24.toInt(), 7f)
    val compactGreen = stroke(0xFF138808.toInt(), 7f)
    val compactWheatStroke = stroke(0xFFB8862B.toInt(), 2.2f)
    val compactWheatOutline = stroke(0xFFB8862B.toInt(), 0.8f)
    val english = textPaint(14.5f, 2.6f)
    val hindi = textPaint(15.5f, 1f)

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun textPaint(fontSize: Float, letterPx: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        this.textSize = fontSize
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        letterSpacing = letterPx / fontSize
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }
}

private object SealGeometry {
    val leftWheatStem = svg("M92.35 154.46A55 55 0 0 1 53.36 70.85")
    val leftLeaves = svg("M81.07 151.64Q75.96 149.75 72.05 153.54Q77.16 155.43 81.07 151.64ZM81.07 151.64Q80.53 146.22 75.41 144.36Q75.95 149.78 81.07 151.64ZM70.65 146.51Q66.19 143.59 61.7 146.47Q66.16 149.39 70.65 146.51ZM70.65 146.51Q71.32 141.22 66.76 138.45Q66.09 143.74 70.65 146.51ZM61.53 139.31Q57.92 135.55 53.06 137.45Q56.67 141.21 61.53 139.31ZM61.53 139.31Q63.33 134.41 59.49 130.88Q57.69 135.78 61.53 139.31ZM54.13 130.35Q51.52 125.97 46.49 126.87Q49.11 131.25 54.13 130.35ZM54.13 130.35Q56.92 126.08 53.91 121.96Q51.12 126.23 54.13 130.35ZM48.78 120.03Q47.25 115.29 42.27 115.19Q43.79 119.94 48.78 120.03ZM48.78 120.03Q52.38 116.58 50.28 112.06Q46.68 115.51 48.78 120.03ZM45.71 108.83Q45.31 103.97 40.54 102.93Q40.95 107.79 45.71 108.83ZM45.71 108.83Q49.9 106.33 48.75 101.6Q44.56 104.09 45.71 108.83ZM45.07 97.22Q45.76 92.51 41.39 90.61Q40.7 95.33 45.07 97.22ZM45.07 97.22Q49.61 95.78 49.4 91.02Q44.85 92.46 45.07 97.22ZM46.88 85.75Q48.59 81.41 44.75 78.78Q43.04 83.11 46.88 85.75ZM46.88 85.75Q51.52 85.38 52.21 80.78Q47.57 81.14 46.88 85.75ZM51.06 74.9Q53.65 71.17 50.47 67.92Q47.87 71.66 51.06 74.9ZM51.06 74.9Q55.56 75.6 57.08 71.31Q52.58 70.62 51.06 74.9ZM53.36 70.85Q57.55 69.05 57.33 64.49Q53.14 66.3 53.36 70.85Z")
    val rightWheatStem = svg("M107.65 154.46A55 55 0 0 0 146.64 70.85")
    val rightLeaves = svg("M118.93 151.64Q124.05 149.78 124.59 144.36Q119.47 146.22 118.93 151.64ZM118.93 151.64Q122.84 155.43 127.95 153.54Q124.04 149.75 118.93 151.64ZM129.35 146.51Q133.91 143.74 133.24 138.45Q128.68 141.22 129.35 146.51ZM129.35 146.51Q133.84 149.39 138.3 146.47Q133.81 143.59 129.35 146.51ZM138.47 139.31Q142.31 135.78 140.51 130.88Q136.67 134.41 138.47 139.31ZM138.47 139.31Q143.33 141.21 146.94 137.45Q142.08 135.55 138.47 139.31ZM145.87 130.35Q148.88 126.23 146.09 121.96Q143.08 126.08 145.87 130.35ZM145.87 130.35Q150.89 131.25 153.51 126.87Q148.48 125.97 145.87 130.35ZM151.22 120.03Q153.32 115.51 149.72 112.06Q147.62 116.58 151.22 120.03ZM151.22 120.03Q156.21 119.94 157.73 115.19Q152.75 115.29 151.22 120.03ZM154.29 108.83Q155.44 104.09 151.25 101.6Q150.1 106.33 154.29 108.83ZM154.29 108.83Q159.05 107.79 159.46 102.93Q154.69 103.97 154.29 108.83ZM154.93 97.22Q155.15 92.46 150.6 91.02Q150.39 95.78 154.93 97.22ZM154.93 97.22Q159.3 95.33 158.61 90.61Q154.24 92.51 154.93 97.22ZM153.12 85.75Q152.43 81.14 147.79 80.78Q148.48 85.38 153.12 85.75ZM153.12 85.75Q156.96 83.11 155.25 78.78Q151.41 81.41 153.12 85.75ZM148.94 74.9Q147.42 70.62 142.92 71.31Q144.44 75.6 148.94 74.9ZM148.94 74.9Q152.13 71.66 149.53 67.92Q146.35 71.17 148.94 74.9ZM146.64 70.85Q146.86 66.3 142.67 64.49Q142.45 69.05 146.64 70.85Z")
    val wave1 = svg("M66 134q8.5-6 17 0t17 0 17 0 17 0")
    val wave2 = svg("M72 142q7-5 14 0t14 0 14 0 14 0")
    val fish = svg("M80 124c6-8 20-9.5 30-4.5l8-6.5-1.5 9 1.5 9-8-6.5c-10 5-24 3.5-30-0.5z")
    val fishGills = svg("M95.5 117.6q2.5 4.4 0 9")
    val wheatStems = svg("M100 110L100 45M105.02 110.07L83.87 54.99M94.98 110.07L116.13 54.99")
    val wheatGrains = svg("M100 46Q100 40.45 94.86 38.37Q94.86 43.92 100 46ZM100 46Q105.14 43.92 105.14 38.37Q100 40.45 100 46ZM100 52.2Q100 46.65 94.86 44.57Q94.86 50.12 100 52.2ZM100 52.2Q105.14 50.12 105.14 44.57Q100 46.65 100 52.2ZM100 58.4Q100 52.85 94.86 50.77Q94.86 56.32 100 58.4ZM100 58.4Q105.14 56.32 105.14 50.77Q100 52.85 100 58.4ZM100 64.6Q100 59.05 94.86 56.97Q94.86 62.52 100 64.6ZM100 64.6Q105.14 62.52 105.14 56.97Q100 59.05 100 64.6ZM100 70.8Q100 65.25 94.86 63.17Q94.86 68.72 100 70.8ZM100 70.8Q105.14 68.72 105.14 63.17Q100 65.25 100 70.8ZM100 47Q103 41.75 100 36.5Q97 41.75 100 47ZM84.23 55.92Q82.24 50.74 76.7 50.65Q78.69 55.82 84.23 55.92ZM84.23 55.92Q88.29 52.14 86.3 46.96Q82.25 50.74 84.23 55.92ZM86.45 61.71Q84.46 56.53 78.92 56.43Q80.91 61.61 86.45 61.71ZM86.45 61.71Q90.51 57.93 88.52 52.75Q84.47 56.53 86.45 61.71ZM88.68 67.5Q86.69 62.32 81.14 62.22Q83.13 67.4 88.68 67.5ZM88.68 67.5Q92.73 63.71 90.75 58.53Q86.69 62.32 88.68 67.5ZM90.9 73.29Q88.91 68.11 83.36 68.01Q85.35 73.19 90.9 73.29ZM90.9 73.29Q94.95 69.5 92.97 64.32Q88.91 68.11 90.9 73.29ZM93.12 79.08Q91.13 73.9 85.58 73.8Q87.57 78.98 93.12 79.08ZM93.12 79.08Q97.17 75.29 95.19 70.11Q91.13 73.9 93.12 79.08ZM84.59 56.86Q85.51 50.88 80.83 47.05Q79.91 53.03 84.59 56.86ZM115.77 55.92Q117.75 50.74 113.7 46.96Q111.71 52.14 115.77 55.92ZM115.77 55.92Q121.31 55.82 123.3 50.65Q117.76 50.74 115.77 55.92ZM113.55 61.71Q115.53 56.53 111.48 52.75Q109.49 57.93 113.55 61.71ZM113.55 61.71Q119.09 61.61 121.08 56.43Q115.54 56.53 113.55 61.71ZM111.32 67.5Q113.31 62.32 109.25 58.53Q107.27 63.71 111.32 67.5ZM111.32 67.5Q116.87 67.4 118.86 62.22Q113.31 62.32 111.32 67.5ZM109.1 73.29Q111.09 68.11 107.03 64.32Q105.05 69.5 109.1 73.29ZM109.1 73.29Q114.65 73.19 116.64 68.01Q111.09 68.11 109.1 73.29ZM106.88 79.08Q108.87 73.9 104.81 70.11Q102.83 75.29 106.88 79.08ZM106.88 79.08Q112.43 78.98 114.42 73.8Q108.87 73.9 106.88 79.08ZM115.41 56.86Q120.09 53.03 119.17 47.05Q114.49 50.88 115.41 56.86Z")
    val compactWheatStems = svg("M100 125L100 60M105.02 125.07L83.87 69.99M94.98 125.07L116.13 69.99")
    val compactWheatGrains = svg("M100 61Q100 55.45 94.86 53.37Q94.86 58.92 100 61ZM100 61Q105.14 58.92 105.14 53.37Q100 55.45 100 61ZM100 67.2Q100 61.65 94.86 59.57Q94.86 65.12 100 67.2ZM100 67.2Q105.14 65.12 105.14 59.57Q100 61.65 100 67.2ZM100 73.4Q100 67.85 94.86 65.77Q94.86 71.32 100 73.4ZM100 73.4Q105.14 71.32 105.14 65.77Q100 67.85 100 73.4ZM100 79.6Q100 74.05 94.86 71.97Q94.86 77.52 100 79.6ZM100 79.6Q105.14 77.52 105.14 71.97Q100 74.05 100 79.6ZM100 85.8Q100 80.25 94.86 78.17Q94.86 83.72 100 85.8ZM100 85.8Q105.14 83.72 105.14 78.17Q100 80.25 100 85.8ZM100 62Q103 56.75 100 51.5Q97 56.75 100 62ZM84.23 70.92Q82.24 65.74 76.7 65.65Q78.69 70.82 84.23 70.92ZM84.23 70.92Q88.29 67.14 86.3 61.96Q82.25 65.74 84.23 70.92ZM86.45 76.71Q84.46 71.53 78.92 71.43Q80.91 76.61 86.45 76.71ZM86.45 76.71Q90.51 72.93 88.52 67.75Q84.47 71.53 86.45 76.71ZM88.68 82.5Q86.69 77.32 81.14 77.22Q83.13 82.4 88.68 82.5ZM88.68 82.5Q92.73 78.71 90.75 73.53Q86.69 77.32 88.68 82.5ZM90.9 88.29Q88.91 83.11 83.36 83.01Q85.35 88.19 90.9 88.29ZM90.9 88.29Q94.95 84.5 92.97 79.32Q88.91 83.11 90.9 88.29ZM93.12 94.08Q91.13 88.9 85.58 88.8Q87.57 93.98 93.12 94.08ZM93.12 94.08Q97.17 90.29 95.19 85.11Q91.13 88.9 93.12 94.08ZM84.59 71.86Q85.51 65.88 80.83 62.05Q79.91 68.03 84.59 71.86ZM115.77 70.92Q117.75 65.74 113.7 61.96Q111.71 67.14 115.77 70.92ZM115.77 70.92Q121.31 70.82 123.3 65.65Q117.76 65.74 115.77 70.92ZM113.55 76.71Q115.53 71.53 111.48 67.75Q109.49 72.93 113.55 76.71ZM113.55 76.71Q119.09 76.61 121.08 71.43Q115.54 71.53 113.55 76.71ZM111.32 82.5Q113.31 77.32 109.25 73.53Q107.27 78.71 111.32 82.5ZM111.32 82.5Q116.87 82.4 118.86 77.22Q113.31 77.32 111.32 82.5ZM109.1 88.29Q111.09 83.11 107.03 79.32Q105.05 84.5 109.1 88.29ZM109.1 88.29Q114.65 88.19 116.64 83.01Q111.09 83.11 109.1 88.29ZM106.88 94.08Q108.87 88.9 104.81 85.11Q102.83 90.29 106.88 94.08ZM106.88 94.08Q112.43 93.98 114.42 88.8Q108.87 88.9 106.88 94.08ZM115.41 71.86Q120.09 68.03 119.17 62.05Q114.49 65.88 115.41 71.86Z")
    val saffronArc = svg("M31.67 95.22A68.5 68.5 0 0 1 168.33 95.22")
    val greenArc = svg("M168.33 104.78A68.5 68.5 0 0 1 31.67 104.78")
    val compactSaffronArc = svg("M24.19 94.7A76 76 0 0 1 175.81 94.7")
    val compactGreenArc = svg("M175.81 105.3A76 76 0 0 1 24.19 105.3")
    val englishArc = AndroidPath().apply {
        addArc(21.5f, 21.5f, 178.5f, 178.5f, 180f, 180f)
    }
    val hindiArc = AndroidPath().apply {
        addArc(12f, 12f, 188f, 188f, 180f, -180f)
    }
    private val englishOffset = PathMeasure(englishArc, false).length / 2f
    private val hindiOffset = PathMeasure(hindiArc, false).length / 2f

    fun drawFull(canvas: android.graphics.Canvas, p: SealPaints) {
        canvas.drawCircle(100f, 100f, 99f, p.goldFill)
        canvas.drawCircle(100f, 100f, 95.5f, p.navyFill)
        canvas.drawCircle(100f, 100f, 93.5f, p.goldHairline)
        canvas.drawCircle(100f, 100f, 72f, p.goldFill)
        canvas.drawCircle(100f, 100f, 70.5f, p.whiteFill)
        canvas.drawPath(saffronArc, p.saffronStroke)
        canvas.drawPath(greenArc, p.greenStroke)
        canvas.drawCircle(100f, 100f, 66.6f, p.creamFill)
        canvas.drawPath(leftWheatStem, p.darkGreenStroke)
        canvas.drawPath(leftLeaves, p.greenFill)
        canvas.drawPath(rightWheatStem, p.darkGreenStroke)
        canvas.drawPath(rightLeaves, p.greenFill)
        canvas.drawPath(wave1, p.waveStroke)
        canvas.drawPath(wave2, p.waveStrokeSoft)
        canvas.drawPath(fish, p.fishFill)
        canvas.drawCircle(87.5f, 122.4f, 1.5f, p.creamFill)
        canvas.drawPath(fishGills, p.creamFill.toStroke(1.1f))
        canvas.drawPath(wheatStems, p.wheatStroke)
        canvas.drawPath(wheatGrains, p.wheatFill)
        canvas.drawPath(wheatGrains, p.wheatOutline)
        canvas.drawRoundRect(93.5f, 94.5f, 106.5f, 99.5f, 1.6f, 1.6f, p.fishFill)
        canvas.drawPath(star(16.5f, 95.4f), p.goldFill)
        canvas.drawPath(star(183.5f, 95.4f), p.goldFill)
        canvas.drawTextOnPath("SAHAKAR BHARATI", englishArc, englishOffset, 4.2f, p.english)
        canvas.drawTextOnPath("सहकार भारती", hindiArc, hindiOffset, 5.2f, p.hindi)
    }

    fun drawCompact(canvas: android.graphics.Canvas, p: SealPaints) {
        canvas.drawCircle(100f, 100f, 99f, p.goldFill)
        canvas.drawCircle(100f, 100f, 92f, p.navyFill)
        canvas.drawCircle(100f, 100f, 80f, p.whiteFill)
        canvas.drawPath(compactSaffronArc, p.compactSaffron)
        canvas.drawPath(compactGreenArc, p.compactGreen)
        canvas.drawCircle(100f, 100f, 71f, p.creamFill)
        canvas.save()
        canvas.translate(100f, 100f)
        canvas.scale(1.38f, 1.38f)
        canvas.translate(-100f, -88.2f)
        canvas.drawPath(compactWheatStems, p.compactWheatStroke)
        canvas.drawPath(compactWheatGrains, p.wheatFill)
        canvas.drawPath(compactWheatGrains, p.compactWheatOutline)
        canvas.drawRoundRect(93f, 109.5f, 107f, 115f, 1.6f, 1.6f, p.fishFill)
        canvas.restore()
    }

    private fun Paint.toStroke(width: Float) = Paint(this).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
    }

    private fun star(cx: Float, cy: Float): AndroidPath {
        // Same 5-point star as the website seal (points="16.5,95.4 …").
        val pts = floatArrayOf(
            0f, 0f, 1.12f, 3.06f, 4.37f, 3.18f, 1.81f, 5.19f, 2.7f, 8.32f,
            0f, 6.5f, -2.7f, 8.32f, -1.81f, 5.19f, -4.37f, 3.18f, -1.12f, 3.06f,
        )
        return AndroidPath().apply {
            moveTo(cx + pts[0], cy + pts[1])
            var i = 2
            while (i < pts.size) {
                lineTo(cx + pts[i], cy + pts[i + 1])
                i += 2
            }
            close()
        }
    }
}

private fun svg(data: String): AndroidPath =
    PathParser.createPathFromPathData(data) ?: AndroidPath()
