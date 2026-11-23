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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class Bubble(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val tint: Int,
)

/** Close-up iridescent bubble membranes with a slow seamless shimmer. */
@Composable
internal fun MicroBubbleSkinEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val bubbles = remember(detailCount) {
        List(30) { i ->
            val col = i % 6
            val row = i / 6
            Bubble(
                x = (col + 0.42f + seededRange(i * 17 + 3, -0.16f, 0.16f)) / 6f,
                y = (row + 0.52f + seededRange(i * 29 + 7, -0.14f, 0.14f)) / 5f,
                radius = seededRange(i * 41 + 11, 0.082f, 0.115f),
                phase = seededUnit(i * 53 + 13) * (2f * PI.toFloat()),
                tint = i % 3,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "microbubbleskin")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "microbubbleskin_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.2f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(time, radius = 1.45f, octaves = 2, seedOffset = 37)
        val deep = TonalPalette.brightness(Color(0xFF081018), brightness * dim)
        val teal = TonalPalette.brightness(Color(0xFF102028), brightness * dim * (0.94f + 0.06f * drift))
        val cyan = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF80E0FF), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to teal,
                0.48f to deep,
                1f to TonalPalette.brightness(Color(0xFF0B1B24), brightness * dim),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(cyan, 0.16f * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF256078), 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.43f + 0.08f * (warp - 0.5f)), h * 0.47f),
                radius = minDim * 0.82f,
            ),
            radius = minDim * 0.82f,
            center = Offset(w * (0.43f + 0.08f * (warp - 0.5f)), h * 0.47f),
        )

        bubbles.forEachIndexed { i, bubble ->
            val pulse = 0.96f + 0.04f * sin(time * 2f * PI.toFloat() + bubble.phase)
            val r = minDim * bubble.radius * pulse
            val x = w * bubble.x + minDim * 0.006f * sin(time * 2f * PI.toFloat() + bubble.phase)
            val y = h * bubble.y + minDim * 0.004f * sin(time * 2f * PI.toFloat() + bubble.phase + 1f)
            val center = Offset(x, y)
            val hue = when (bubble.tint) {
                0 -> Color(0xFF55D8E8)
                1 -> Color(0xFF799AFF)
                else -> Color(0xFF8CE8D2)
            }

            // A quiet pool of reflected light beneath each thin membrane.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(hue, 0.22f * dim),
                    0.72f to TonalPalette.withAlpha(Color(0xFF173D4B), 0.13f * dim),
                    1f to Color.Transparent,
                    center = center,
                    radius = r * 1.18f,
                ),
                radius = r * 1.18f,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFF8EEBFF), 0.13f * dim),
                    0.58f to TonalPalette.withAlpha(hue, 0.09f * dim),
                    1f to TonalPalette.withAlpha(Color(0xFF07131B), 0.28f * dim),
                    center = Offset(x - r * 0.16f, y - r * 0.12f),
                    radius = r * 1.05f,
                ),
                radius = r,
                center = center,
            )
            drawCircle(
                color = TonalPalette.withAlpha(cyan, (0.32f + 0.12f * warp) * dim),
                radius = r,
                center = center,
                style = Stroke(width = minDim * 0.0032f),
            )
            // Asymmetric curved glint makes each sphere read as a glossy membrane.
            drawArc(
                color = TonalPalette.withAlpha(Color(0xFFC7F8FF), (0.50f + 0.22f * warp) * dim),
                startAngle = 202f + bubble.tint * 13f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = Offset(x - r * 0.82f, y - r * 0.82f),
                size = androidx.compose.ui.geometry.Size(r * 1.64f, r * 1.64f),
                style = Stroke(width = minDim * 0.006f),
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, 0.42f * dim),
                radius = minDim * 0.004f,
                center = Offset(x - r * 0.38f, y - r * 0.42f),
            )
            if (i % 4 == 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(Color(0xFFB8F5FF), 0.22f * dim),
                        1f to Color.Transparent,
                        center = Offset(x - r * 0.35f, y - r * 0.38f),
                        radius = r * 0.24f,
                    ),
                    radius = r * 0.24f,
                    center = Offset(x - r * 0.35f, y - r * 0.38f),
                )
            }
        }
        repeat(detailCount) { i ->
            val x = w * (0.08f + seededUnit(i * 83 + 19) * 0.84f)
            val y = h * (0.08f + seededUnit(i * 97 + 31) * 0.84f)
            val twinkle = 0.45f + 0.55f * loopedFbm(time, radius = 1.1f, seedOffset = i * 109 + 47)
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFA8F2FF), 0.42f * twinkle * dim),
                radius = minDim * 0.0025f,
                center = Offset(x, y),
            )
        }
    }
}
