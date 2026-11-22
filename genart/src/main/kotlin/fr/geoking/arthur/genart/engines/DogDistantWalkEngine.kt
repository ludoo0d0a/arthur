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
import kotlin.math.PI
import kotlin.math.sin

/** Distant dog silhouette walking slowly along a soft dusk horizon. */
@Composable
internal fun DogDistantWalkEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dog_distant_walk_loop")
    val walkAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_distant_walk_t",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val t = phase01(walkAnim)
        val haze = loopedFbm(t, radius = 1.5f, octaves = 2, seedOffset = 23)

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1A2438),
                0.5f to Color(0xFF3A2E3A),
                1f to Color(0xFF1A1210),
            ),
        )

        val horizonY = h * (0.58f + 0.01f * haze)
        drawRect(
            color = TonalPalette.brightness(Color(0xFF1C1612), brightness * restingDim),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY),
        )
        // Soft ground wash
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF4A3828), 0.35f * restingDim),
                1f to Color.Transparent,
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h * 0.2f),
        )

        // Distant walk: ping-pong along horizon (smooth, no pop)
        val walkCycle = if (t < 0.5f) t * 2f else (1f - t) * 2f
        val dogX = w * (0.18f + 0.64f * walkCycle)
        val bob = sin(t * 4f * PI.toFloat()) * minDim * 0.004f * (if (isActive) 1f else 0.4f)
        val dogY = horizonY - minDim * 0.02f + bob
        val scale = minDim * 0.035f
        val sil = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF0E0C0A), TonalPalette.pick(paletteColors, 0), 0.15f),
            brightness * restingDim,
        )

        // Body
        drawOval(
            color = sil,
            topLeft = Offset(dogX - scale * 1.4f, dogY - scale * 1.1f),
            size = Size(scale * 2.8f, scale * 1.5f),
        )
        // Head
        drawCircle(color = sil, radius = scale * 0.7f, center = Offset(dogX + scale * 1.5f, dogY - scale * 0.9f))
        // Ears / legs as simple strokes
        if (quality != GenartQuality.Low) {
            val legPhase = sin(t * 8f * PI.toFloat()) * scale * 0.25f * (if (isActive) 1f else 0.3f)
            val legs = Path().apply {
                moveTo(dogX - scale * 0.8f, dogY)
                lineTo(dogX - scale * 0.9f, dogY + scale * 1.2f + legPhase)
                moveTo(dogX + scale * 0.2f, dogY)
                lineTo(dogX + scale * 0.35f, dogY + scale * 1.2f - legPhase)
                moveTo(dogX + scale * 0.9f, dogY)
                lineTo(dogX + scale * 1.0f, dogY + scale * 1.15f + legPhase * 0.7f)
            }
            drawPath(legs, color = sil, style = Stroke(width = scale * 0.35f, cap = StrokeCap.Round))
            val ear = Path().apply {
                val hx = dogX + scale * 1.5f
                val hy = dogY - scale * 0.9f
                moveTo(hx - scale * 0.2f, hy - scale * 0.3f)
                lineTo(hx - scale * 0.55f, hy - scale * 1.0f)
                lineTo(hx + scale * 0.1f, hy - scale * 0.45f)
                close()
            }
            drawPath(ear, color = sil)
        }
    }
}
