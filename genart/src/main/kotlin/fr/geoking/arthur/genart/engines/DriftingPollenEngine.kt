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

/** Warm pollen motes suspended in still air, wandering gently in place. */
@Composable
internal fun DriftingPollenEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 24, high = 36)
    val motes = remember(count) {
        List(count) { i ->
            PollenSeed(
                x0 = seededUnit(i * 17 + 5),
                y0 = seededUnit(i * 29 + 11),
                wanderAmpX = seededRange(i * 41 + 13, 0.015f, 0.045f),
                wanderAmpY = seededRange(i * 53 + 17, 0.015f, 0.045f),
                wanderFreqX = seededRange(i * 67 + 19, 0.3f, 0.9f),
                wanderFreqY = seededRange(i * 71 + 23, 0.3f, 0.9f),
                wanderPhaseX = seededRange(i * 83 + 29, 0f, 2f * PI.toFloat()),
                wanderPhaseY = seededRange(i * 97 + 31, 0f, 2f * PI.toFloat()),
                pulseFreq = seededRange(i * 101 + 37, 0.25f, 0.9f),
                pulsePhase = seededRange(i * 113 + 41, 0f, 2f * PI.toFloat()),
                radius = seededRange(i * 127 + 43, 1.8f, 4.5f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "drifting_pollen")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drifting_pollen_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0E1208), Color(0xFF06080A)),
            ),
        )
        val time = phase01(t)
        val angle = time * 2f * PI.toFloat()
        val pulseAmp = if (isActive) 0.5f else 0.22f
        val dim = if (isActive) 1f else 0.55f
        motes.forEach { mote ->
            val x = phase01(mote.x0 + mote.wanderAmpX * sin(angle * mote.wanderFreqX + mote.wanderPhaseX)) * w
            val y = phase01(mote.y0 + mote.wanderAmpY * cos(angle * mote.wanderFreqY + mote.wanderPhaseY)) * h
            val pulse = 0.4f + pulseAmp * (0.5f + 0.5f * sin(angle * mote.pulseFreq + mote.pulsePhase))
            val base = TonalPalette.mix(Color(0xFFE3E86B), TonalPalette.pick(paletteColors, mote.colorIndex), 0.35f)
            val glow = TonalPalette.brightness(base, brightness)
            val glowR = mote.radius * 3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(glow, 0.5f * pulse * dim),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = glowR,
                ),
                radius = glowR,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(glow, (0.8f * pulse * dim).coerceIn(0f, 1f)),
                radius = mote.radius * pulse,
                center = Offset(x, y),
            )
        }
    }
}

private data class PollenSeed(
    val x0: Float,
    val y0: Float,
    val wanderAmpX: Float,
    val wanderAmpY: Float,
    val wanderFreqX: Float,
    val wanderFreqY: Float,
    val wanderPhaseX: Float,
    val wanderPhaseY: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val radius: Float,
    val colorIndex: Int,
)
