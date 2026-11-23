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
import kotlin.math.cos

/** Soft Ambient POV Train Tunnel Pulse — seamless tunnel-light rhythm. */
@Composable
internal fun TrainTunnelPulseEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lampPairs = qualityCount(quality, low = 4, medium = 7, high = 10)
    val transition = rememberInfiniteTransition(label = "trainTunnelPulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trainTunnelPulse_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val vanishingY = h * 0.39f
        val phase = phase01(t)
        val warp = loopedFbm(phase, radius = 1.25f, octaves = 3, seedOffset = 29)
        val pulse = 0.92f + 0.08f * warp
        val wall = TonalPalette.brightness(Color(0xFF14181E), brightness * dim)
        val dark = TonalPalette.brightness(Color(0xFF080A0E), brightness * dim)
        val lamp = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE0E8F0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )

        drawRect(brush = Brush.verticalGradient(listOf(dark, wall, dark)))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(lamp, 0.12f * pulse * dim),
                0.5f to TonalPalette.withAlpha(Color(0xFF8593A3), 0.055f * dim),
                1f to Color.Transparent,
                center = Offset(cx, vanishingY),
                radius = (minDim * 0.7f),
            ),
            radius = minDim * 0.7f,
            center = Offset(cx, vanishingY),
        )

        // Dark floor recedes to the tunnel's vanishing point.
        val floor = Path().apply {
            moveTo(cx - w * 0.035f, vanishingY)
            lineTo(cx + w * 0.035f, vanishingY)
            lineTo(w * 0.92f, h)
            lineTo(w * 0.08f, h)
            close()
        }
        drawPath(
            floor,
            brush = Brush.verticalGradient(
                0f to Color(0xFF10141A),
                1f to Color(0xFF050609),
                startY = vanishingY,
                endY = h,
            ),
        )

        // Repeated arch ribs define the tunnel and give the lights their rhythm.
        for (i in 0..5) {
            val depth = 0.12f + i * 0.16f
            val halfWidth = w * (0.025f + 0.48f * depth)
            val baseY = vanishingY + (h - vanishingY) * depth * 0.86f
            val crownY = vanishingY - h * (0.015f + 0.34f * depth)
            val arch = Path().apply {
                moveTo(cx - halfWidth, baseY)
                cubicTo(cx - halfWidth, crownY + h * 0.03f, cx - halfWidth * 0.46f, crownY, cx, crownY)
                cubicTo(cx + halfWidth * 0.46f, crownY, cx + halfWidth, crownY + h * 0.03f, cx + halfWidth, baseY)
            }
            drawPath(
                path = arch,
                color = TonalPalette.withAlpha(Color(0xFF7C8794), (0.11f + 0.07f * depth) * dim),
                style = Stroke(width = minDim * (0.003f + 0.003f * depth), cap = StrokeCap.Round),
            )
        }

        // Rails and sleepers converge into the center, reinforcing the train POV.
        for (side in -1..1 step 2) {
            drawLine(
                color = TonalPalette.withAlpha(lamp, 0.13f * dim),
                start = Offset(cx + side * w * 0.018f, vanishingY),
                end = Offset(cx + side * w * 0.30f, h),
                strokeWidth = minDim * 0.004f,
            )
        }
        for (i in 0..9) {
            val depth = ((i / 10f + phase) % 1f).coerceIn(0f, 1f)
            val y = vanishingY + (h - vanishingY) * depth * depth
            val half = w * (0.025f + 0.27f * depth * depth)
            val alpha = (0.04f + 0.08f * depth) * dim
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFF9AA3AD), alpha),
                start = Offset(cx - half, y),
                end = Offset(cx + half, y),
                strokeWidth = minDim * (0.002f + 0.004f * depth),
            )
        }

        // Paired lamps flow toward and away from the viewer at a constant loop rate.
        for (i in 0 until lampPairs) {
            val z = ((i / lampPairs.toFloat() + phase) % 1f)
            val depth = 0.06f + 0.88f * z * z
            val y = vanishingY + (h - vanishingY) * depth * 0.82f
            val xOffset = w * (0.025f + 0.46f * depth)
            val radius = minDim * (0.003f + 0.013f * depth)
            val flicker = 0.78f + 0.22f * (0.5f + 0.5f * cos(2f * PI.toFloat() * phase + i * 0.8f))
            for (side in -1..1 step 2) {
                val center = Offset(cx + side * xOffset, y - minDim * (0.025f + 0.015f * depth))
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(lamp, (0.23f * flicker * dim)),
                        0.32f to TonalPalette.withAlpha(lamp, (0.11f * flicker * dim)),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius * 5f,
                    ),
                    radius = radius * 5f,
                    center = center,
                )
                drawOval(
                    color = TonalPalette.withAlpha(lamp, (0.34f * flicker * dim)),
                    topLeft = Offset(center.x - radius, center.y - radius * 0.45f),
                    size = Size(radius * 2f, radius * 0.9f),
                )
            }
        }

        // Soft edge falloff keeps attention on the distant tunnel glow.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF050609), 0.52f * dim),
                0.22f to Color.Transparent,
                0.78f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF050609), 0.52f * dim),
            ),
        )
    }
}
