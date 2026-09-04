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

/** Soft glowing fireflies drifting and pulsing in a dark field. */
@Composable
internal fun FirefliesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 24, high = 36)
    val bugs = remember(count) {
        List(count) { i ->
            FireflySeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                driftX = seededRange(i * 41 + 11, 0.03f, 0.12f),
                driftY = seededRange(i * 53 + 13, 0.02f, 0.08f),
                pulseFreq = seededRange(i * 67 + 19, 0.4f, 1.6f),
                pulsePhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                radius = seededRange(i * 89 + 29, 2.5f, 6.5f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fireflies")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fireflies_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF05080E), Color(0xFF02040A)),
            ),
        )
        val time = phase01(t)
        val pulseAmp = if (isActive) 0.55f else 0.25f
        val dim = if (isActive) 1f else 0.6f
        bugs.forEach { bug ->
            val x = phase01(bug.x0 + time * bug.driftX) * w
            val y = phase01(bug.y0 + time * bug.driftY + 0.02f * sin(time * 2f * PI.toFloat() + bug.pulsePhase)) * h
            val pulse = 0.35f + pulseAmp * (0.5f + 0.5f * sin(time * 2f * PI.toFloat() * bug.pulseFreq + bug.pulsePhase))
            val base = TonalPalette.mix(Color(0xFFF5E6A0), TonalPalette.pick(paletteColors, bug.colorIndex), 0.35f)
            val glow = TonalPalette.brightness(base, brightness)
            val glowR = bug.radius * 3.5f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(glow, 0.55f * pulse * dim),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = glowR,
                ),
                radius = glowR,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(glow, (0.85f * pulse * dim).coerceIn(0f, 1f)),
                radius = bug.radius * pulse,
                center = Offset(x, y),
            )
        }
    }
}

private data class FireflySeed(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val radius: Float,
    val colorIndex: Int,
)
