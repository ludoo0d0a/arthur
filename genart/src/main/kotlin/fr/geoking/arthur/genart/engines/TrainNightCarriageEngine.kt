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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Quiet night train interior with warm ceiling lamps and passing lights beyond the windows. */
@Composable
internal fun TrainNightCarriageEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "trainnightcarriage")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trainnightcarriage_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val ambient = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 17)
        val warm = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFC070), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        val coolGlow = TonalPalette.brightness(Color(0xFF657B96), brightness * dim)
        val deep = TonalPalette.brightness(Color(0xFF0C0E14), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF090B10), deep, Color(0xFF181C28))))

        // Dark windows sit in the side walls, showing indistinct landscape and lights sliding past.
        val windowTop = h * 0.15f
        val windowBottom = h * 0.57f
        val windowHeight = windowBottom - windowTop
        val windowLeft = w * 0.055f
        val windowRight = w * 0.945f
        val windowWidth = w * 0.245f
        listOf(windowLeft, windowRight - windowWidth).forEachIndexed { side, x ->
            drawRoundRect(
                color = TonalPalette.withAlpha(Color(0xFF292C34), 0.84f * dim),
                topLeft = Offset(x - minDim * 0.012f, windowTop - minDim * 0.012f),
                size = Size(windowWidth + minDim * 0.024f, windowHeight + minDim * 0.024f),
                cornerRadius = CornerRadius(minDim * 0.022f),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF070B14),
                    0.5f to Color(0xFF111927),
                    1f to Color(0xFF080B12),
                    startY = windowTop,
                    endY = windowBottom,
                ),
                topLeft = Offset(x, windowTop),
                size = Size(windowWidth, windowHeight),
                cornerRadius = CornerRadius(minDim * 0.014f),
            )
            val drift = (time * 0.34f + side * 0.31f) % 1f
            for (i in 0 until detailCount) {
                val yFrac = (0.12f + i * 0.17f + side * 0.09f) % 0.9f
                val bandY = windowTop + windowHeight * yFrac
                val bandW = windowWidth * (0.18f + 0.22f * loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 101 + i * 13 + side * 51))
                val bandX = x + ((drift + i * 0.23f) % 1f) * (windowWidth + bandW) - bandW
                val bandColor = if (i % 3 == 0) warm else coolGlow
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to TonalPalette.withAlpha(bandColor, 0.09f * dim),
                        1f to Color.Transparent,
                        startX = bandX,
                        endX = bandX + bandW,
                    ),
                    topLeft = Offset(bandX, bandY),
                    size = Size(bandW, minDim * (0.012f + 0.006f * (i % 2))),
                    cornerRadius = CornerRadius(minDim * 0.02f),
                )
            }
            // Soft interior reflections on the glass.
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFD5D9E0), 0.045f * dim),
                    0.16f to Color.Transparent,
                    0.82f to Color.Transparent,
                    1f to TonalPalette.withAlpha(Color(0xFFD5D9E0), 0.035f * dim),
                    startX = x,
                    endX = x + windowWidth,
                ),
                topLeft = Offset(x, windowTop),
                size = Size(windowWidth, windowHeight),
                cornerRadius = CornerRadius(minDim * 0.014f),
            )
        }

        // Carriage side panels frame the windows and converge toward the far end.
        val wall = Path().apply {
            moveTo(0f, h * 0.08f)
            lineTo(w * 0.045f, h * 0.15f)
            lineTo(w * 0.045f, h * 0.68f)
            lineTo(0f, h * 0.88f)
            close()
        }
        drawPath(wall, color = TonalPalette.withAlpha(Color(0xFF10131A), 0.74f * dim))
        val rightWall = Path().apply {
            moveTo(w, h * 0.08f)
            lineTo(w * 0.955f, h * 0.15f)
            lineTo(w * 0.955f, h * 0.68f)
            lineTo(w, h * 0.88f)
            close()
        }
        drawPath(rightWall, color = TonalPalette.withAlpha(Color(0xFF10131A), 0.74f * dim))

        // Far end door and its dark glass panel.
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF252933), 0.70f * dim),
            topLeft = Offset(w * 0.425f, h * 0.20f),
            size = Size(w * 0.15f, h * 0.37f),
            cornerRadius = CornerRadius(minDim * 0.018f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF080B12), Color(0xFF151A24))),
            topLeft = Offset(w * 0.442f, h * 0.22f),
            size = Size(w * 0.116f, h * 0.30f),
            cornerRadius = CornerRadius(minDim * 0.012f),
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF69717E), 0.20f * dim),
            start = Offset(w * 0.5f, h * 0.22f),
            end = Offset(w * 0.5f, h * 0.52f),
            strokeWidth = minDim * 0.003f,
        )

        // Ceiling taper and small warm lamps receding down the aisle.
        val ceiling = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.66f, h * 0.24f)
            lineTo(w * 0.34f, h * 0.24f)
            close()
        }
        drawPath(ceiling, brush = Brush.verticalGradient(listOf(Color(0xFF171A21), Color(0xFF0D1017))))
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF343741), 0.30f * dim),
            start = Offset(w * 0.05f, h * 0.06f),
            end = Offset(w * 0.38f, h * 0.23f),
            strokeWidth = minDim * 0.006f,
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF343741), 0.30f * dim),
            start = Offset(w * 0.95f, h * 0.06f),
            end = Offset(w * 0.62f, h * 0.23f),
            strokeWidth = minDim * 0.006f,
        )
        for (i in 0 until 4) {
            val depth = i / 3f
            val y = h * (0.055f + 0.145f * depth)
            val lampW = minDim * (0.045f - 0.025f * depth)
            val lampH = minDim * (0.018f - 0.010f * depth)
            val centerX = w * 0.5f
            val pulse = 0.94f + 0.06f * sin(time * 2f * PI.toFloat() + i * 0.75f)
            val glowRadius = minDim * (0.20f - 0.10f * depth)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(warm, 0.13f * pulse * dim),
                    1f to Color.Transparent,
                    center = Offset(centerX, y + lampH),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(centerX, y + lampH),
            )
            drawRoundRect(
                color = TonalPalette.withAlpha(Color(0xFFFFE2B1), 0.68f * pulse * dim),
                topLeft = Offset(centerX - lampW / 2f, y),
                size = Size(lampW, lampH),
                cornerRadius = CornerRadius(lampH),
            )
        }

        // Receding floor and aisle, with a faint reflected lamp line.
        val floor = Path().apply {
            moveTo(w * 0.34f, h * 0.56f)
            lineTo(w * 0.66f, h * 0.56f)
            lineTo(w * 0.99f, h)
            lineTo(w * 0.01f, h)
            close()
        }
        drawPath(floor, brush = Brush.verticalGradient(listOf(Color(0xFF151821), Color(0xFF080A0F)), startY = h * 0.56f, endY = h))
        drawLine(
            color = TonalPalette.withAlpha(warm, 0.10f * (0.8f + 0.2f * ambient) * dim),
            start = Offset(w * 0.5f, h * 0.58f),
            end = Offset(w * 0.5f, h),
            strokeWidth = minDim * 0.012f,
        )

        // Seat backs form soft silhouettes on both sides, smaller toward the far door.
        for (row in 0 until 4) {
            val depth = row / 3f
            val y = h * (0.49f + depth * 0.105f)
            val seatW = w * (0.19f - depth * 0.07f)
            val seatH = h * (0.19f - depth * 0.07f)
            val gap = w * (0.41f - depth * 0.08f)
            val alpha = (0.90f - depth * 0.22f) * dim
            for (side in 0..1) {
                val cx = if (side == 0) w * 0.5f - gap else w * 0.5f + gap
                val x = cx - seatW / 2f
                drawRoundRect(
                    color = TonalPalette.withAlpha(Color(0xFF07090D), alpha),
                    topLeft = Offset(x, y + seatH * 0.28f),
                    size = Size(seatW, seatH * 0.72f),
                    cornerRadius = CornerRadius(minDim * 0.022f),
                )
                drawRoundRect(
                    color = TonalPalette.withAlpha(if (row % 2 == 0) Color(0xFF171A21) else Color(0xFF13161D), alpha),
                    topLeft = Offset(x + seatW * 0.07f, y),
                    size = Size(seatW * 0.86f, seatH * 0.46f),
                    cornerRadius = CornerRadius(minDim * 0.035f),
                )
                drawLine(
                    color = TonalPalette.withAlpha(warm, 0.07f * dim),
                    start = Offset(x + seatW * 0.18f, y + seatH * 0.43f),
                    end = Offset(x + seatW * 0.82f, y + seatH * 0.43f),
                    strokeWidth = minDim * (0.002f + 0.001f * depth),
                )
            }
        }

        // Window frame bars and a dark vignette keep the cabin intimate.
        drawLine(color = TonalPalette.withAlpha(Color(0xFF06070A), 0.70f * dim), start = Offset(w * 0.32f, h * 0.13f), end = Offset(w * 0.32f, h * 0.62f), strokeWidth = minDim * 0.018f)
        drawLine(color = TonalPalette.withAlpha(Color(0xFF06070A), 0.70f * dim), start = Offset(w * 0.68f, h * 0.13f), end = Offset(w * 0.68f, h * 0.62f), strokeWidth = minDim * 0.018f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF050609), 0.42f * dim),
                startY = h * 0.72f,
                endY = h,
            ),
        )
    }
}
