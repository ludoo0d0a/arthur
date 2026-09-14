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
import androidx.compose.ui.graphics.drawscope.DrawScope
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/** Layered undulating sine wave ribbons with gradient fills and soft shadow underlayers. */
@Composable
internal fun GradientWavesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val layers = qualityCount(quality, low = 5, medium = 8, high = 12)
    val transition = rememberInfiniteTransition(label = "gradient_waves")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradient_waves_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF04060B))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        for (i in 0 until layers) {
            val progress = i.toFloat() / layers
            val colorA = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), brightness)
            val colorB = TonalPalette.brightness(TonalPalette.pick(paletteColors, i + 1), brightness)

            val shadowPath = Path()
            val wavePath = Path()

            val baseScaleY = h * (0.25f + 0.65f * progress)
            val shadowOffsetY = h * 0.025f

            // Build Shadow Path
            shadowPath.moveTo(0f, h)
            shadowPath.lineTo(0f, baseScaleY + shadowOffsetY)
            val steps = 40
            for (step in 0..steps) {
                val x = w * (step.toFloat() / steps)
                val normX = step.toFloat() / steps
                val waveOffset = sin(normX * 2f * PI.toFloat() * 1.5f + time * 2f * PI.toFloat() + i * 0.7f) * (h * 0.08f) +
                        sin(normX * 2f * PI.toFloat() * 3f - time * 2f * PI.toFloat() * 0.8f) * (h * 0.04f)
                val y = baseScaleY + shadowOffsetY + waveOffset
                shadowPath.lineTo(x, y)
            }
            shadowPath.lineTo(w, h)
            shadowPath.close()

            // Draw Layer Drop Shadow
            drawPath(
                path = shadowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f * dim),
                        Color.Transparent
                    ),
                    startY = baseScaleY,
                    endY = baseScaleY + shadowOffsetY * 3f
                )
            )

            // Build Wave Path
            wavePath.moveTo(0f, h)
            wavePath.lineTo(0f, baseScaleY)
            for (step in 0..steps) {
                val x = w * (step.toFloat() / steps)
                val normX = step.toFloat() / steps
                val waveOffset = sin(normX * 2f * PI.toFloat() * 1.5f + time * 2f * PI.toFloat() + i * 0.7f) * (h * 0.08f) +
                        sin(normX * 2f * PI.toFloat() * 3f - time * 2f * PI.toFloat() * 0.8f) * (h * 0.04f)
                val y = baseScaleY + waveOffset
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, h)
            wavePath.close()

            // Draw Gradient Wave Layer
            val morph = sin01(time * 2f * PI.toFloat() + i * 0.5f)
            val gradientColor = TonalPalette.mix(colorA, colorB, morph)
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(colorA, 0.85f * dim),
                        TonalPalette.withAlpha(gradientColor, 0.9f * dim),
                        TonalPalette.withAlpha(colorB, 0.85f * dim),
                    ),
                    startX = 0f,
                    endX = w,
                )
            )
        }
    }
}
