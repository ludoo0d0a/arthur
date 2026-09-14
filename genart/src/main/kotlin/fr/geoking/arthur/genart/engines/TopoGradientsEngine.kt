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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/** Topographic elevation contour bands with layered gradient fills and ridge drop shadows. */
@Composable
internal fun TopoGradientsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bands = qualityCount(quality, low = 6, medium = 10, high = 14)
    val transition = rememberInfiniteTransition(label = "topo_gradients")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "topo_gradients_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF030508))

        val dim = if (isActive) 1f else 0.55f
        val time = phase01(t)

        for (i in 0 until bands) {
            val normI = i.toFloat() / bands
            val baseY = h * (0.15f + 0.75f * normI)
            val shadowYOffset = h * 0.03f

            val shadowPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY + shadowYOffset)
                val steps = 50
                for (s in 0..steps) {
                    val normX = s.toFloat() / steps
                    val x = w * normX
                    val wave = sin(normX * 2f * PI.toFloat() * 1.8f + time * 2f * PI.toFloat() + i * 0.5f) * (h * 0.07f) +
                            sin(normX * 2f * PI.toFloat() * 3.5f - time * 2f * PI.toFloat() * 0.7f) * (h * 0.03f)
                    val y = baseY + shadowYOffset + wave
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }

            // Draw Layer Shadow
            drawPath(
                path = shadowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f * dim),
                        Color.Transparent,
                    ),
                    startY = baseY,
                    endY = baseY + shadowYOffset * 3f
                )
            )

            val topoPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY)
                val steps = 50
                for (s in 0..steps) {
                    val normX = s.toFloat() / steps
                    val x = w * normX
                    val wave = sin(normX * 2f * PI.toFloat() * 1.8f + time * 2f * PI.toFloat() + i * 0.5f) * (h * 0.07f) +
                            sin(normX * 2f * PI.toFloat() * 3.5f - time * 2f * PI.toFloat() * 0.7f) * (h * 0.03f)
                    val y = baseY + wave
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }

            val colorA = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), brightness)
            val colorB = TonalPalette.brightness(TonalPalette.pick(paletteColors, i + 1), brightness)
            val morph = sin01(time * 2f * PI.toFloat() + i * 0.6f)
            val color = TonalPalette.mix(colorA, colorB, morph)

            // Draw Topo Gradient Band
            drawPath(
                path = topoPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(colorA, 0.88f * dim),
                        TonalPalette.withAlpha(color, 0.92f * dim),
                        TonalPalette.withAlpha(colorB, 0.88f * dim),
                    ),
                    startX = 0f,
                    endX = w,
                )
            )

            // Draw Top Edge Ridge Highlight
            drawPath(
                path = topoPath,
                color = Color.White.copy(alpha = 0.25f * dim),
                style = Stroke(width = 2.5f)
            )
        }
    }
}
