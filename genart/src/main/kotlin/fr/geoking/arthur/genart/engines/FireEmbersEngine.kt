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

@Composable
internal fun FireEmbersEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 20, medium = 35, high = 55)
    val embers = remember(count) {
        List(count) { i ->
            EmberSeed(
                startX = seededUnit(i * 17 + 3),
                phaseOffset = seededUnit(i * 29 + 7),
                riseSpeedFactor = seededRange(i * 41 + 11, 0.6f, 1.4f),
                jitterAmp = seededRange(i * 53 + 13, 0.01f, 0.035f),
                jitterFreq = seededRange(i * 67 + 19, 0.5f, 2.2f),
                flickerFreq = seededRange(i * 79 + 23, 0.5f, 2.4f),
                flickerPhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                radius = seededRange(i * 97 + 31, 3f, 9f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fire_embers")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fire_embers_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF040202),
                0.7f to Color(0xFF0A0503),
                1f to Color(0xFF2A0F06),
                startY = 0f,
                endY = h,
            ),
        )
        val time = phase01(t) * (2f * PI.toFloat())
        val renderCount = if (isActive) embers.size else (embers.size * 0.6f).toInt().coerceAtLeast(1)
        val flickerRange = if (isActive) 0.3f else 0.15f
        val activeDim = if (isActive) 1f else 0.55f
        for (index in 0 until renderCount) {
            val ember = embers[index]
            val life = phase01(t * ember.riseSpeedFactor + ember.phaseOffset)
            val jitterX = ember.jitterAmp * sin(time * ember.jitterFreq + ember.phaseOffset * 10f)
            val x = (ember.startX + jitterX) * w
            val y = (1f - life) * h
            val flicker = (1f - flickerRange) + flickerRange * sin(time * ember.flickerFreq + ember.flickerPhase)
            val fadeIn = (life / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - life) / 0.55f).coerceIn(0f, 1f)
            val alphaLife = (fadeIn * fadeOut).coerceIn(0f, 1f) * activeDim
            val base = TonalPalette.pick(paletteColors, ember.colorIndex)
            val glowColor = TonalPalette.brightness(base, (brightness * flicker).coerceAtLeast(0f))
            val glowRadius = ember.radius * 3.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, glowColor.copy(alpha = 0f)),
                    center = Offset(x, y),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(x, y),
                alpha = alphaLife,
            )
            drawCircle(
                color = glowColor,
                radius = ember.radius,
                center = Offset(x, y),
                alpha = alphaLife,
            )
        }
    }
}

private data class EmberSeed(
    val startX: Float,
    val phaseOffset: Float,
    val riseSpeedFactor: Float,
    val jitterAmp: Float,
    val jitterFreq: Float,
    val flickerFreq: Float,
    val flickerPhase: Float,
    val radius: Float,
    val colorIndex: Int,
)
