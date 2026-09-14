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
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Rotating angular sweep vortex with soft radial shadow spokes. */
@Composable
internal fun ConicalVortexEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val sectors = qualityCount(quality, low = 8, medium = 12, high = 16)
    val transition = rememberInfiniteTransition(label = "conical_vortex")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "conical_vortex_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val maxRadius = maxOf(w, h) * 0.8f

        drawRect(color = Color(0xFF06040A))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val baseAngleDeg = time * 360f

        withTransform({
            rotate(baseAngleDeg, center)
        }) {
            val sweep = 360f / sectors

            // Draw shadow spokes first
            for (i in 0 until sectors) {
                val angleRad = (i * sweep) * (PI / 180.0).toFloat()
                val edgeX = center.x + maxRadius * cos(angleRad)
                val edgeY = center.y + maxRadius * sin(angleRad)

                drawLine(
                    color = Color.Black.copy(alpha = 0.5f * dim),
                    start = center,
                    end = Offset(edgeX, edgeY),
                    strokeWidth = 14f
                )
            }

            // Draw color sector wedges
            for (i in 0 until sectors) {
                val startAngle = i * sweep
                val angleRad1 = startAngle * (PI / 180.0).toFloat()
                val angleRad2 = (startAngle + sweep) * (PI / 180.0).toFloat()

                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + maxRadius * cos(angleRad1), center.y + maxRadius * sin(angleRad1))
                    lineTo(center.x + maxRadius * cos(angleRad2), center.y + maxRadius * sin(angleRad2))
                    close()
                }

                val colorA = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), brightness)
                val colorB = TonalPalette.brightness(TonalPalette.pick(paletteColors, i + 1), brightness)
                val morph = sin01(time * 2f * PI.toFloat() + i * 0.4f)
                val color = TonalPalette.mix(colorA, colorB, morph)

                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(color, 0.95f * dim),
                            TonalPalette.withAlpha(color, 0.5f * dim),
                            TonalPalette.withAlpha(color, 0.1f * dim),
                        ),
                        center = center,
                        radius = maxRadius
                    )
                )
            }
        }
    }
}
