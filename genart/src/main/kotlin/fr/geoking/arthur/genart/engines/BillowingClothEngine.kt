package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/**
 * Laundry-line cloth: translucent sheets pinned along a line, billowing sideways on a shared gust.
 */
@Composable
internal fun BillowingClothEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val clothCount = qualityCount(quality, low = 4, medium = 5, high = 7)
    val segments = qualityCount(quality, low = 18, medium = 26, high = 34)
    val sheets = remember(clothCount) {
        List(clothCount) { i ->
            BillowSheet(
                pinX = seededRange(i * 17 + 3, 0.08f, 0.92f),
                widthFrac = seededRange(i * 29 + 7, 0.12f, 0.22f),
                lengthFrac = seededRange(i * 41 + 11, 0.42f, 0.72f),
                phase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                speedMul = seededRange(i * 67 + 19, 0.55f, 1.25f),
                waveCycles = seededRange(i * 79 + 23, 1.2f, 2.6f),
                swayAmp = seededRange(i * 89 + 29, 0.04f, 0.11f),
                colorIndex = i,
                colorMix = seededUnit(i * 97 + 31),
                alpha = seededRange(i * 103 + 37, 0.42f, 0.72f),
                layer = seededUnit(i * 109 + 43),
            )
        }.sortedBy { it.layer }
    }

    val transition = rememberInfiniteTransition(label = "billow_cloth")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "billow_cloth_t",
    )
    val gustT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((11000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "billow_gust_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFD8E6F0), Color(0xFFB4C8D8), Color(0xFF8FA8BC)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val gust = 0.4f + 0.6f * sin01(phase01(gustT) * 2f * PI.toFloat())
        val dim = if (isActive) 1f else 0.55f
        val lineY = h * 0.14f

        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF3A4550), 0.55f * dim),
            start = Offset(w * 0.02f, lineY),
            end = Offset(w * 0.98f, lineY),
            strokeWidth = (minDim * 0.004f).coerceAtLeast(1.2f),
            cap = StrokeCap.Round,
        )

        sheets.forEach { sheet ->
            val path = buildBillowSheet(sheet, w, h, lineY, time, gust, segments)
            val cA = TonalPalette.pick(paletteColors, sheet.colorIndex)
            val cB = TonalPalette.pick(paletteColors, sheet.colorIndex + 2)
            val tint = TonalPalette.brightness(TonalPalette.mix(cA, cB, sheet.colorMix), brightness)
            val shadow = Path().apply { addPath(path, Offset(minDim * 0.012f, minDim * 0.018f)) }
            drawPath(
                path = shadow,
                color = TonalPalette.withAlpha(Color.Black, 0.12f * dim),
            )
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(TonalPalette.mix(tint, Color.White, 0.35f), sheet.alpha * dim),
                        TonalPalette.withAlpha(tint, sheet.alpha * 0.85f * dim),
                        TonalPalette.withAlpha(tint, sheet.alpha * 0.55f * dim),
                    ),
                ),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(Color.White, 0.18f * dim),
                style = Stroke(width = (minDim * 0.003f).coerceAtLeast(0.8f)),
            )
            val pinX = sheet.pinX * w
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFF2C3540), 0.7f * dim),
                radius = (minDim * 0.008f).coerceAtLeast(1.5f),
                center = Offset(pinX, lineY),
            )
        }
    }
}

private fun buildBillowSheet(
    sheet: BillowSheet,
    w: Float,
    h: Float,
    lineY: Float,
    time: Float,
    gust: Float,
    segments: Int,
): Path {
    val halfW = sheet.widthFrac * w * 0.5f
    val length = sheet.lengthFrac * h
    val sway = sheet.swayAmp * w * gust
    val path = Path()
    for (k in 0..segments) {
        val u = k / segments.toFloat()
        val y = lineY + u * length
        val flare = u * u
        val wave = sin(u * sheet.waveCycles * 2f * PI.toFloat() + time * sheet.speedMul + sheet.phase)
        val x = sheet.pinX * w - halfW * (1f - 0.15f * u) + sway * flare * wave
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (k in segments downTo 0) {
        val u = k / segments.toFloat()
        val y = lineY + u * length
        val flare = u * u
        val wave = sin(u * sheet.waveCycles * 2f * PI.toFloat() + time * sheet.speedMul + sheet.phase + 0.35f)
        val x = sheet.pinX * w + halfW * (1f - 0.15f * u) + sway * flare * wave
        path.lineTo(x, y)
    }
    path.close()
    return path
}

private data class BillowSheet(
    val pinX: Float,
    val widthFrac: Float,
    val lengthFrac: Float,
    val phase: Float,
    val speedMul: Float,
    val waveCycles: Float,
    val swayAmp: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val alpha: Float,
    val layer: Float,
)
