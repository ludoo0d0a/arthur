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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Calm underwater school — tiny ellipse silhouettes, no detailed anatomy. */
@Composable
internal fun FishSchoolEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 12, medium = 20, high = 32)
    val fish = remember(count) {
        List(count) { i ->
            FishSeed(
                orbitRadius = seededRange(i * 17 + 3, 0.06f, 0.28f),
                angleOffset = seededRange(i * 29 + 7, 0f, 2f * PI.toFloat()),
                angularSpeed = seededRange(i * 41 + 11, 0.6f, 1.4f),
                scale = seededRange(i * 53 + 13, 0.5f, 1.2f),
                verticalTilt = seededRange(i * 67 + 19, 0.35f, 0.9f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fish_school")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fish_school_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF061525), Color(0xFF020810)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val schoolX = (0.5f + 0.28f * cos(time * 0.45f)) * w
        val schoolY = (0.5f + 0.18f * sin(time * 0.6f)) * h
        val minDim = minOf(w, h)
        val dim = if (isActive) 1f else 0.65f

        fish.forEach { f ->
            val angle = f.angleOffset + time * f.angularSpeed
            val fx = schoolX + cos(angle) * f.orbitRadius * minDim
            val fy = schoolY + sin(angle) * f.orbitRadius * minDim * f.verticalTilt
            val heading = atan2(
                cos(angle + 0.15f) - cos(angle),
                -(sin(angle + 0.15f) - sin(angle)),
            ) * 180f / PI.toFloat()
            val bodyW = f.scale * minDim * 0.035f
            val bodyH = bodyW * 0.45f
            val base = TonalPalette.pick(paletteColors, f.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            val alpha = (0.45f + 0.4f * f.scale.coerceIn(0.4f, 1.2f)) * dim
            rotate(degrees = heading, pivot = Offset(fx, fy)) {
                drawOval(
                    color = TonalPalette.withAlpha(color, alpha),
                    topLeft = Offset(fx - bodyW, fy - bodyH * 0.5f),
                    size = Size(bodyW * 2f, bodyH),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha * 0.7f),
                    radius = bodyH * 0.35f,
                    center = Offset(fx - bodyW * 0.85f, fy),
                )
            }
        }
    }
}

private data class FishSeed(
    val orbitRadius: Float,
    val angleOffset: Float,
    val angularSpeed: Float,
    val scale: Float,
    val verticalTilt: Float,
    val colorIndex: Int,
)
