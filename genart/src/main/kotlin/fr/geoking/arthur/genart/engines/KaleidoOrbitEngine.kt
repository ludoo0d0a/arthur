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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Warm kaleidoscopic petals orbiting around a softly lit central rosette. */
@Composable
internal fun KaleidoOrbitEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val orbitCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "kaleidoorbit")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kaleidoorbit_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val center = Offset(w * 0.5f, h * 0.5f)
        val time = phase01(t)
        val phase = time * (2f * PI.toFloat())
        val warp = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 23)
        val breathe = (0.5f + 0.5f * sin(phase)) * (0.75f + 0.25f * warp)
        val backgroundTop = TonalPalette.brightness(Color(0xFF101018), brightness * dim)
        val backgroundBottom = TonalPalette.brightness(Color(0xFF201828), brightness * dim)
        val gold = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD080), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        val rose = TonalPalette.brightness(Color(0xFFB86A88), brightness * dim)
        val lilac = TonalPalette.brightness(Color(0xFF786080), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom)))

        val glowRadius = minDim * 0.58f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(gold, (0.12f + 0.04f * breathe) * dim),
                0.42f to TonalPalette.withAlpha(rose, 0.075f * dim),
                1f to Color.Transparent,
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )

        // Fine concentric guide rings give the orbiting forms a soft glass lens.
        for (ring in 0..3) {
            val radius = minDim * (0.105f + ring * 0.085f)
            drawCircle(
                color = TonalPalette.withAlpha(if (ring % 2 == 0) gold else lilac, (0.12f + 0.025f * breathe) * dim),
                radius = radius,
                center = center,
                style = Stroke(width = minDim * 0.0018f),
            )
        }

        val petals = 8
        for (i in 0 until petals) {
            val angle = phase + i * (2f * PI.toFloat() / petals)
            val orbit = minDim * 0.17f
            val petalCenter = Offset(
                center.x + cos(angle) * orbit,
                center.y + sin(angle) * orbit,
            )
            val petalLength = minDim * (0.13f + 0.012f * breathe)
            val petalWidth = minDim * 0.045f
            val petal = Path().apply {
                moveTo(petalCenter.x, petalCenter.y)
                cubicTo(
                    petalCenter.x - petalWidth, petalCenter.y - petalLength * 0.25f,
                    petalCenter.x - petalWidth * 0.72f, petalCenter.y - petalLength * 0.82f,
                    petalCenter.x, petalCenter.y - petalLength,
                )
                cubicTo(
                    petalCenter.x + petalWidth * 0.72f, petalCenter.y - petalLength * 0.82f,
                    petalCenter.x + petalWidth, petalCenter.y - petalLength * 0.25f,
                    petalCenter.x, petalCenter.y,
                )
                close()
            }
            val petalColor = when (i % 3) {
                0 -> gold
                1 -> rose
                else -> lilac
            }
            rotate(degrees = angle * (180f / PI.toFloat()) + 90f, pivot = petalCenter) {
                drawPath(
                    path = petal,
                    brush = Brush.verticalGradient(
                        0f to TonalPalette.withAlpha(petalColor, 0.12f * dim),
                        0.55f to TonalPalette.withAlpha(petalColor, 0.34f * dim),
                        1f to TonalPalette.withAlpha(gold, 0.18f * dim),
                        startY = petalCenter.y - petalLength,
                        endY = petalCenter.y + petalLength * 0.12f,
                    ),
                )
                drawPath(
                    path = petal,
                    color = TonalPalette.withAlpha(petalColor, 0.24f * dim),
                    style = Stroke(width = minDim * 0.0015f),
                )
            }
            // Orbiting pearls are deliberately dim and broad, without hard light sources.
        }
        for (i in 0 until orbitCount) {
            val beadAngle = phase + i * (2f * PI.toFloat() / orbitCount) - phase * 0.08f
            val bead = Offset(
                center.x + cos(beadAngle) * minDim * 0.31f,
                center.y + sin(beadAngle) * minDim * 0.31f,
            )
            val beadRadius = minDim * 0.013f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(gold, 0.30f * dim),
                    1f to Color.Transparent,
                    center = bead,
                    radius = beadRadius * 3f,
                ),
                radius = beadRadius * 3f,
                center = bead,
            )
            drawCircle(
                color = TonalPalette.withAlpha(gold, 0.38f * dim),
                radius = beadRadius * (0.85f + breathe * 0.15f),
                center = bead,
            )
        }

        // The central rosette anchors the slow orbit and gently pulses in brightness.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(gold, (0.38f + 0.08f * breathe) * dim),
                0.45f to TonalPalette.withAlpha(rose, 0.24f * dim),
                1f to Color.Transparent,
                center = center,
                radius = minDim * 0.10f,
            ),
            radius = minDim * 0.10f,
            center = center,
        )
        drawCircle(
            color = TonalPalette.withAlpha(gold, 0.42f * dim),
            radius = minDim * 0.018f,
            center = center,
            style = Stroke(width = minDim * 0.002f),
        )
    }
}
