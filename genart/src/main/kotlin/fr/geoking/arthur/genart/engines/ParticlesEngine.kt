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
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ParticlesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 48, medium = 96, high = 160)
    val particles = remember(count) {
        List(count) { i ->
            ParticleSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                vx = seededRange(i * 41 + 11, -0.12f, 0.12f),
                vy = seededRange(i * 53 + 13, -0.08f, 0.08f),
                radius = seededRange(i * 67 + 19, 2.5f, 7f),
                colorIndex = i,
                trail = seededRange(i * 71 + 23, 0.35f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "particles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particles_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0B1220), Color(0xFF020617)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.85f,
            ),
        )
        val time = phase01(t) * (2f * PI.toFloat())
        particles.forEach { p ->
            val driftX = p.x0 + p.vx * time + 0.04f * sin(time * 1.3f + p.x0 * 6f)
            val driftY = p.y0 + p.vy * time + 0.05f * cos(time * 1.1f + p.y0 * 5f)
            val x = ((driftX % 1f) + 1f) % 1f * w
            val y = ((driftY % 1f) + 1f) % 1f * h
            val base = TonalPalette.pick(paletteColors, p.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            val trailSteps = if (isActive) 6 else 3
            for (s in trailSteps downTo 1) {
                val back = s / trailSteps.toFloat()
                val tx = x - p.vx * w * 0.08f * back * p.trail
                val ty = y - p.vy * h * 0.08f * back * p.trail
                drawCircle(
                    color = TonalPalette.withAlpha(color, (0.12f + 0.18f * (1f - back)) * brightness.coerceAtMost(1f)),
                    radius = p.radius * (0.6f + 0.5f * (1f - back)),
                    center = Offset(tx, ty),
                )
            }
            drawCircle(
                color = TonalPalette.withAlpha(color, (0.55f + 0.35f * p.trail).coerceAtMost(1f)),
                radius = p.radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class ParticleSeed(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val colorIndex: Int,
    val trail: Float,
)
