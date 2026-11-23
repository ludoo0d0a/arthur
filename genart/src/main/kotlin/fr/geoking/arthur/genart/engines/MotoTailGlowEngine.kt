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

/** Soft Ambient Moto Tail Glow: a distant rider receding through a red haze. */
@Composable
internal fun MotoTailGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val streakCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "mototailglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mototailglow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val time = phase01(t)
        val atmosphere = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 37) - 0.5f
        val flicker = 0.92f + 0.08f * loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 71)
        val red = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFF263F), TonalPalette.pick(paletteColors, 0), 0.2f),
            brightness * dim,
        )
        val hotRed = TonalPalette.brightness(Color(0xFFFF5264), brightness * dim)
        val road = TonalPalette.brightness(Color(0xFF121116), brightness * dim)
        val horizonY = h * (0.49f + drift * 0.018f)
        val bikeY = h * 0.70f
        val tailY = bikeY - minDim * 0.115f
        val tailX = cx + drift * minDim * 0.018f

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF08070C), brightness * dim),
                0.55f to TonalPalette.brightness(Color(0xFF170A10), brightness * dim * (0.92f + 0.08f * atmosphere)),
                1f to Color(0xFF09090D),
            ),
        )

        // Distant red haze spreading out from the rider's wake.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to TonalPalette.withAlpha(red, 0.11f * dim),
                1f to Color.Transparent,
                startY = horizonY - h * 0.16f,
                endY = horizonY + h * 0.36f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(red, 0.17f * dim * flicker),
                0.52f to TonalPalette.withAlpha(red, 0.065f * dim),
                1f to Color.Transparent,
                center = Offset(tailX, tailY),
                radius = minDim * 0.52f,
            ),
            radius = minDim * 0.52f,
            center = Offset(tailX, tailY),
        )

        // Road plane converges behind the small motorcycle silhouette.
        val roadPath = Path().apply {
            moveTo(cx - w * 0.055f, horizonY)
            lineTo(cx + w * 0.055f, horizonY)
            lineTo(w * 1.15f, h)
            lineTo(-w * 0.15f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                0f to Color(0xFF171116),
                0.48f to road,
                1f to Color(0xFF08090D),
                startY = horizonY,
                endY = h,
            ),
        )

        // Red tail-light ribbons and their long, low reflections.
        for (i in 0 until streakCount) {
            val side = if (i % 2 == 0) -1f else 1f
            val lane = i / 2 + 1
            val xEnd = tailX + side * minDim * (0.14f + lane * 0.065f)
            val yEnd = tailY + minDim * (0.12f + lane * 0.035f)
            val sway = sin(time * 2f * PI.toFloat() + i * 1.7f) * minDim * 0.009f
            val path = Path().apply {
                moveTo(tailX + side * minDim * 0.014f, tailY + minDim * 0.012f)
                cubicTo(
                    tailX + sway + side * minDim * 0.035f,
                    tailY + minDim * 0.055f,
                    xEnd - sway * 0.4f,
                    yEnd - minDim * 0.04f,
                    xEnd,
                    yEnd,
                )
            }
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(red, (0.075f + (1f - lane / 7f).coerceIn(0f, 1f) * 0.08f) * dim),
                style = Stroke(width = minDim * (0.005f + (1f - lane / 7f).coerceIn(0f, 1f) * 0.006f), cap = StrokeCap.Round),
            )
        }

        // Soft elongated red reflection on the asphalt.
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(hotRed, 0.24f * dim * flicker),
                0.35f to TonalPalette.withAlpha(red, 0.15f * dim),
                1f to Color.Transparent,
                center = Offset(tailX, bikeY + minDim * 0.17f),
                radius = minDim * 0.33f,
            ),
            topLeft = Offset(tailX - minDim * 0.3f, bikeY + minDim * 0.045f),
            size = Size(minDim * 0.6f, minDim * 0.45f),
        )
        for (i in 0..2) {
            val y = bikeY + minDim * (0.13f + i * 0.07f)
            val width = minDim * (0.095f - i * 0.018f)
            drawLine(
                color = TonalPalette.withAlpha(red, (0.14f - i * 0.035f) * dim),
                start = Offset(cx - width, y),
                end = Offset(cx + width, y),
                strokeWidth = minDim * 0.006f,
                cap = StrokeCap.Round,
            )
        }

        // Distant rear silhouette: wheels, bodywork, seat and fender.
        val silhouette = Color(0xFF08080C)
        drawCircle(silhouette, minDim * 0.052f, Offset(cx, bikeY + minDim * 0.048f), style = Stroke(minDim * 0.012f))
        drawCircle(
            color = TonalPalette.withAlpha(Color(0xFF351019), 0.7f * dim),
            radius = minDim * 0.052f,
            center = Offset(cx, bikeY + minDim * 0.048f),
            style = Stroke(width = minDim * 0.002f),
        )
        val body = Path().apply {
            moveTo(cx - minDim * 0.055f, bikeY - minDim * 0.025f)
            lineTo(cx - minDim * 0.032f, bikeY - minDim * 0.064f)
            quadraticTo(cx, bikeY - minDim * 0.078f, cx + minDim * 0.032f, bikeY - minDim * 0.064f)
            lineTo(cx + minDim * 0.055f, bikeY - minDim * 0.025f)
            lineTo(cx + minDim * 0.035f, bikeY + minDim * 0.024f)
            lineTo(cx - minDim * 0.035f, bikeY + minDim * 0.024f)
            close()
        }
        drawPath(body, silhouette)
        drawRoundRect(
            color = silhouette,
            topLeft = Offset(cx - minDim * 0.032f, bikeY - minDim * 0.1f),
            size = Size(minDim * 0.064f, minDim * 0.025f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(minDim * 0.012f),
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF1A1117), 0.9f * dim),
            start = Offset(cx - minDim * 0.024f, bikeY - minDim * 0.055f),
            end = Offset(cx + minDim * 0.024f, bikeY - minDim * 0.055f),
            strokeWidth = minDim * 0.004f,
            cap = StrokeCap.Round,
        )

        // Compact, saturated tail lamp at the center of the bike.
        drawRoundRect(
            color = TonalPalette.withAlpha(hotRed, 0.8f * dim * flicker),
            topLeft = Offset(tailX - minDim * 0.018f, tailY - minDim * 0.007f),
            size = Size(minDim * 0.036f, minDim * 0.014f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(minDim * 0.007f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(hotRed, 0.52f * dim * flicker),
                1f to Color.Transparent,
                center = Offset(tailX, tailY),
                radius = minDim * 0.11f,
            ),
            radius = minDim * 0.11f,
            center = Offset(tailX, tailY),
        )
    }
}
