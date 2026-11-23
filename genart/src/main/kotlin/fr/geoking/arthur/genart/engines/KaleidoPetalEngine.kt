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
import kotlin.math.sin

/** Soft Ambient Kaleido Petal — mirrored radial petals in a slow seamless loop. */
@Composable
internal fun KaleidoPetalEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detail = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "kaleidopetal")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kaleidopetal_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val center = Offset(w * 0.5f, h * 0.5f)
        val time = phase01(t)
        val motion = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 183)
        val pulse = 0.96f + 0.04f * sin(time * 2f * PI.toFloat())
        val rotation = 4f * sin(time * 2f * PI.toFloat())
        val dark = TonalPalette.brightness(Color(0xFF140C18), brightness * dim * (0.94f + motion * 0.06f))
        val plum = TonalPalette.brightness(Color(0xFF2A1830), brightness * dim)
        val rose = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFF80C0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        val lavender = TonalPalette.brightness(Color(0xFFB080D0), brightness * dim)
        val petalCount = 8
        val outerRadius = minDim * 0.43f * pulse

        drawRect(brush = Brush.verticalGradient(listOf(dark, plum)))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(rose, 0.24f * dim),
                0.55f to TonalPalette.withAlpha(lavender, 0.10f * dim),
                1f to Color.Transparent,
                center = center,
                radius = minDim * 0.68f,
            ),
            radius = minDim * 0.68f,
            center = center,
        )

        // The single leaf silhouette is reflected around the center eight times.
        // Keeping all lobes tied to the same radius gives the mandala a clean symmetry.
        val petal = Path().apply {
            moveTo(center.x, center.y - minDim * 0.055f)
            cubicTo(
                center.x - minDim * 0.12f, center.y - minDim * 0.20f,
                center.x - minDim * 0.12f, center.y - outerRadius * 0.76f,
                center.x, center.y - outerRadius,
            )
            cubicTo(
                center.x + minDim * 0.12f, center.y - outerRadius * 0.76f,
                center.x + minDim * 0.12f, center.y - minDim * 0.20f,
                center.x, center.y - minDim * 0.055f,
            )
            close()
        }
        for (i in 0 until petalCount) {
            rotate(rotation + i * 360f / petalCount, center) {
                drawPath(
                    path = petal,
                    brush = Brush.verticalGradient(
                        0f to TonalPalette.withAlpha(rose, 0.08f * dim),
                        0.42f to TonalPalette.withAlpha(lavender, 0.32f * dim),
                        1f to TonalPalette.withAlpha(rose, 0.56f * dim),
                        startY = center.y - outerRadius,
                        endY = center.y,
                    ),
                )
                drawPath(
                    path = petal,
                    color = TonalPalette.withAlpha(rose, 0.45f * dim),
                    style = Stroke(width = minDim * 0.0025f),
                )
            }
        }

        // Petal veins and concentric mirrored rings provide detail without hard points.
        val veinCount = minOf(detail, 7)
        for (i in 0 until petalCount) {
            rotate(rotation + i * 360f / petalCount, center) {
                drawLine(
                    color = TonalPalette.withAlpha(Color(0xFFFFC0E0), 0.26f * dim),
                    start = Offset(center.x, center.y - minDim * 0.09f),
                    end = Offset(center.x, center.y - outerRadius * 0.82f),
                    strokeWidth = minDim * 0.0018f,
                )
                for (v in 1..veinCount) {
                    val fraction = v / (veinCount + 1f)
                    val y = center.y - minDim * (0.12f + fraction * 0.25f)
                    val spread = minDim * (0.015f + fraction * 0.045f)
                    drawLine(
                        color = TonalPalette.withAlpha(lavender, 0.18f * dim),
                        start = Offset(center.x, y),
                        end = Offset(center.x - spread, y - minDim * 0.018f),
                        strokeWidth = minDim * 0.0014f,
                    )
                    drawLine(
                        color = TonalPalette.withAlpha(lavender, 0.18f * dim),
                        start = Offset(center.x, y),
                        end = Offset(center.x + spread, y - minDim * 0.018f),
                        strokeWidth = minDim * 0.0014f,
                    )
                }
            }
        }

        for (ring in 1..3) {
            drawCircle(
                color = TonalPalette.withAlpha(rose, (0.20f - ring * 0.035f) * dim),
                radius = minDim * (0.095f + ring * 0.035f),
                center = center,
                style = Stroke(width = minDim * 0.002f),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFD0E8), 0.88f * dim),
                0.35f to TonalPalette.withAlpha(rose, 0.56f * dim),
                1f to TonalPalette.withAlpha(plum, 0.02f * dim),
                center = center,
                radius = minDim * 0.075f,
            ),
            radius = minDim * 0.075f,
            center = center,
        )
    }
}
