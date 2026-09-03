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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Calm dusk-sky flock of chevron birds orbiting a slow shared drift center. */
@Composable
internal fun BirdFlockEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 10, medium = 18, high = 28)
    val birds = remember(count) {
        List(count) { i ->
            BirdSeed(
                orbitRadius = seededRange(i * 17 + 3, 0.05f, 0.22f),
                angleOffset = seededRange(i * 29 + 7, 0f, 2f * PI.toFloat()),
                angularSpeedJitter = seededRange(i * 41 + 11, 0.75f, 1.35f),
                flapPhase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                flapFrequency = seededRange(i * 67 + 19, 3.2f, 5.5f),
                scale = seededRange(i * 79 + 23, 0.55f, 1.15f),
                colorIndex = i,
                verticalTilt = seededRange(i * 89 + 29, 0.4f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "birdflock")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "birdflock_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1023), Color(0xFF03040A)),
            ),
        )
        for (i in 0 until 24) {
            val sx = seededUnit(i * 13 + 5) * w
            val sy = seededUnit(i * 31 + 9) * h * 0.7f
            val sAlpha = seededRange(i * 47 + 15, 0.06f, 0.22f)
            drawCircle(
                color = Color.White.copy(alpha = sAlpha),
                radius = seededRange(i * 61 + 21, 0.6f, 1.6f),
                center = Offset(sx, sy),
            )
        }

        val time = phase01(t) * (2f * PI.toFloat())
        val flockCenterX = 0.5f + 0.3f * cos(time * 0.5f)
        val flockCenterY = 0.35f + 0.15f * sin(time * 0.7f)
        val cx = flockCenterX * w
        val cy = flockCenterY * h
        val minDim = minOf(w, h)
        val flapAmplitude = if (isActive) 0.5f else 0.15f
        val flapSpeed = if (isActive) 1f else 0.4f
        val dimFactor = if (isActive) 1f else 0.7f

        birds.forEach { bird ->
            val angle = bird.angleOffset + time * bird.angularSpeedJitter
            val bx = cx + cos(angle) * bird.orbitRadius * minDim
            val by = cy + sin(angle) * bird.orbitRadius * minDim * bird.verticalTilt

            val halfAngle = 0.9f + flapAmplitude * sin(time * bird.flapFrequency * flapSpeed + bird.flapPhase)
            val wingLength = bird.scale * minDim * 0.05f
            val leftTip = Offset(
                bx - sin(halfAngle) * wingLength,
                by + cos(halfAngle) * wingLength * bird.verticalTilt,
            )
            val rightTip = Offset(
                bx + sin(halfAngle) * wingLength,
                by + cos(halfAngle) * wingLength * bird.verticalTilt,
            )

            val depthFactor = bird.scale.coerceIn(0.4f, 1.2f)
            val base = TonalPalette.pick(paletteColors, bird.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            val alpha = ((0.4f + 0.5f * depthFactor) * dimFactor).coerceIn(0f, 1f)
            val strokeWidth = 1.2f + 1.4f * depthFactor

            drawLine(
                color = TonalPalette.withAlpha(color, alpha),
                start = leftTip,
                end = Offset(bx, by),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = TonalPalette.withAlpha(color, alpha),
                start = Offset(bx, by),
                end = rightTip,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class BirdSeed(
    val orbitRadius: Float,
    val angleOffset: Float,
    val angularSpeedJitter: Float,
    val flapPhase: Float,
    val flapFrequency: Float,
    val scale: Float,
    val colorIndex: Int,
    val verticalTilt: Float,
)
