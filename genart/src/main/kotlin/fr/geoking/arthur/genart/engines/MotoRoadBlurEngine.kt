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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Moto Road Blur — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun MotoRoadBlurEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val streakCount = qualityCount(quality, low = 5, medium = 9, high = 14)
    val transition = rememberInfiniteTransition(label = "motoroadblur")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "motoroadblur_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val centerX = w * 0.5f
        val time = phase01(t)
        val warp = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 23)
        val horizonY = h * (0.43f + (warp - 0.5f) * 0.035f)
        val roadBottomY = h * 0.93f
        val roadTopHalf = w * 0.065f
        val roadBottomHalf = w * 0.62f
        val shoulder = TonalPalette.brightness(Color(0xFF1A1C20), brightness * dim)
        val night = TonalPalette.brightness(Color(0xFF0C0E12), brightness * dim)
        val asphalt = TonalPalette.brightness(Color(0xFF161A20), brightness * dim)
        val steel = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF808890), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * dim,
        )
        val headlight = TonalPalette.brightness(Color(0xFFAEBCC8), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(shoulder, night)))
        // Distant smoky city light at the vanishing point.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(steel, 0.19f * dim),
                0.38f to TonalPalette.withAlpha(steel, 0.07f * dim),
                1f to Color.Transparent,
                center = Offset(centerX, horizonY),
                radius = (minDim * 0.52f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.52f).coerceAtLeast(1f),
            center = Offset(centerX, horizonY),
        )

        // Broad roadway and feathered shoulders.
        val road = Path().apply {
            moveTo(centerX - roadTopHalf, horizonY)
            lineTo(centerX + roadTopHalf, horizonY)
            lineTo(centerX + roadBottomHalf, roadBottomY)
            lineTo(centerX - roadBottomHalf, roadBottomY)
            close()
        }
        drawPath(road, color = asphalt)
        val leftEdge = Path().apply {
            moveTo(centerX - roadTopHalf, horizonY)
            lineTo(centerX - roadBottomHalf, roadBottomY)
        }
        val rightEdge = Path().apply {
            moveTo(centerX + roadTopHalf, horizonY)
            lineTo(centerX + roadBottomHalf, roadBottomY)
        }
        drawPath(leftEdge, color = TonalPalette.withAlpha(steel, 0.10f * dim), style = Stroke(width = minDim * 0.012f))
        drawPath(rightEdge, color = TonalPalette.withAlpha(steel, 0.10f * dim), style = Stroke(width = minDim * 0.012f))

        // Center dashes travel toward the rider and broaden with perspective.
        for (i in 0 until 7) {
            val fraction = ((i / 7f + time) % 1f)
            val perspective = fraction * fraction
            val y = horizonY + (roadBottomY - horizonY) * fraction
            val dashH = minDim * (0.004f + 0.045f * perspective)
            val dashW = w * (0.003f + 0.018f * perspective)
            val alpha = (0.22f + 0.38f * perspective) * dim
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.28f to TonalPalette.withAlpha(headlight, alpha),
                    0.72f to TonalPalette.withAlpha(headlight, alpha),
                    1f to Color.Transparent,
                    startX = centerX - dashW,
                    endX = centerX + dashW,
                ),
                topLeft = Offset(centerX - dashW, y),
                size = Size(dashW * 2f, dashH),
            )
        }

        // Peripheral lights smear diagonally away from the road's vanishing point.
        for (i in 0 until streakCount) {
            val lane = i / streakCount.toFloat()
            val side = if (i % 2 == 0) -1f else 1f
            val progress = (lane + time) % 1f
            val depth = progress * progress
            val y = horizonY + (roadBottomY - horizonY) * (0.12f + 0.86f * progress)
            val x = centerX + side * (roadTopHalf * (1f - progress) + roadBottomHalf * progress) * 1.18f
            val length = minDim * (0.025f + 0.24f * depth)
            val sway = sin((time + lane) * 2f * PI.toFloat()) * minDim * 0.008f
            val glowRadius = (minDim * (0.018f + 0.042f * depth)).coerceAtLeast(1f)
            val glowColor = if (i % 3 == 0) Color(0xFF8695A6) else Color(0xFF596A7A)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(glowColor, (0.24f + 0.18f * depth) * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(x, y),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to TonalPalette.withAlpha(glowColor, (0.22f + 0.25f * depth) * dim),
                    1f to Color.Transparent,
                    startX = x - length * 0.5f,
                    endX = x + length * 0.5f,
                ),
                start = Offset(x - length * 0.5f, y + sway),
                end = Offset(x + length * 0.5f, y - sway),
                strokeWidth = minDim * (0.002f + 0.006f * depth),
                cap = StrokeCap.Round,
            )
        }

        // A low, indistinct fairing silhouette anchors the first-person viewpoint.
        val fairing = Path().apply {
            moveTo(0f, h * 0.91f)
            cubicTo(w * 0.28f, h * 0.85f, w * 0.36f, h * 0.96f, centerX, h * 0.94f)
            cubicTo(w * 0.64f, h * 0.96f, w * 0.72f, h * 0.85f, w, h * 0.91f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            fairing,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF080A0C), Color(0xFF050608)),
                startY = h * 0.9f,
                endY = h,
            ),
        )
        drawLine(
            color = TonalPalette.withAlpha(steel, 0.09f * dim),
            start = Offset(w * 0.25f, h * 0.925f),
            end = Offset(w * 0.75f, h * 0.925f),
            strokeWidth = minDim * 0.003f,
            cap = StrokeCap.Round,
        )
    }
}
