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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Layered low-frequency radial blobs approximating a drifting color-noise field. */
@Composable
internal fun SoftNoiseFieldEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 10, medium = 16, high = 24)
    val cells = remember(count) {
        List(count) { i ->
            NoiseCell(
                x0 = seededUnit(i * 13 + 1),
                y0 = seededUnit(i * 19 + 5),
                driftX = seededRange(i * 31 + 7, -0.16f, 0.16f),
                driftY = seededRange(i * 43 + 11, -0.14f, 0.14f),
                radiusFrac = seededRange(i * 59 + 17, 0.18f, 0.42f),
                wobbleFreq = seededRange(i * 71 + 23, 0.15f, 0.55f),
                wobblePhase = seededRange(i * 83 + 29, 0f, 2f * PI.toFloat()),
                breatheFreq = seededRange(i * 91 + 37, 0.2f, 0.6f),
                alphaBase = seededRange(i * 97 + 31, 0.08f, 0.22f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "soft_noise_field")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "soft_noise_field_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF06050A))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        cells.forEach { cell ->
            val wobble = sin(time * 2f * PI.toFloat() * cell.wobbleFreq + cell.wobblePhase)
            val breathe = sin(time * 2f * PI.toFloat() * cell.breatheFreq + cell.wobblePhase)
            val x = phase01(cell.x0 + time * cell.driftX + wobble * 0.05f) * w
            val y = phase01(cell.y0 + time * cell.driftY + wobble * 0.04f) * h
            val radius = cell.radiusFrac * minDim * (0.85f + 0.2f * breathe)
            val alphaPulse = 0.85f + 0.15f * breathe
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, cell.colorIndex),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, cell.alphaBase * dim * alphaPulse),
                        TonalPalette.withAlpha(tint, cell.alphaBase * 0.25f * dim * alphaPulse),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class NoiseCell(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val radiusFrac: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float,
    val breatheFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
