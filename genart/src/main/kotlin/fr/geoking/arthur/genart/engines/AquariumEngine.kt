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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Cozy fish tank — a few winding fish and slow rising bubbles over a soft water gradient. */
@Composable
internal fun AquariumEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val fishCount = qualityCount(quality, low = 2, medium = 3, high = 4)
    val fish = remember(fishCount) {
        List(fishCount) { i ->
            AquariumFishSeed(
                orbitRadius = seededRange(i * 17 + 3, 0.05f, 0.18f),
                angleOffset = seededRange(i * 29 + 7, 0f, 2f * PI.toFloat()),
                angularSpeed = seededRange(i * 41 + 11, 0.4f, 0.9f),
                scale = seededRange(i * 53 + 13, 0.6f, 1.1f),
                verticalTilt = seededRange(i * 67 + 19, 0.35f, 0.75f),
                colorIndex = i,
            )
        }
    }
    val bubbleCount = qualityCount(quality, low = 6, medium = 10, high = 16)
    val bubbles = remember(bubbleCount) {
        List(bubbleCount) { i ->
            AquariumBubbleSeed(
                x0 = seededUnit(i * 71 + 5),
                y0 = seededUnit(i * 83 + 9),
                riseSpeed = seededRange(i * 97 + 13, 0.2f, 0.6f),
                radius = seededRange(i * 101 + 17, 2f, 6f),
                wobbleAmp = seededRange(i * 109 + 21, 0.008f, 0.025f),
                wobbleFreq = seededRange(i * 113 + 25, 0.4f, 1.4f),
                wobblePhase = seededRange(i * 127 + 29, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "aquarium")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aquarium_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E5A66), Color(0xFF072A33)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.6f
        val minDim = minOf(w, h)
        val tankX = 0.5f * w
        val tankY = 0.48f * h

        fish.forEach { f ->
            val angle = f.angleOffset + time * f.angularSpeed
            val fx = tankX + cos(angle) * f.orbitRadius * minDim
            val fy = tankY + sin(angle) * f.orbitRadius * minDim * f.verticalTilt
            val heading = atan2(
                cos(angle + 0.15f) - cos(angle),
                -(sin(angle + 0.15f) - sin(angle)),
            ) * 180f / PI.toFloat()
            val bodyW = f.scale * minDim * 0.03f
            val bodyH = bodyW * 0.5f
            val base = TonalPalette.pick(paletteColors, f.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            val alpha = (0.5f + 0.35f * f.scale.coerceIn(0.5f, 1.1f)) * dim
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

        val bubbleTime = phase01(t)
        bubbles.forEach { b ->
            val life = phase01(b.y0 + b.riseSpeed * bubbleTime)
            val y = (1f - life) * h
            val wobble = sin(bubbleTime * 2f * PI.toFloat() * b.wobbleFreq + b.wobblePhase) * b.wobbleAmp
            val x = phase01(b.x0 + wobble) * w
            val fade = (life / 0.1f).coerceIn(0f, 1f) * ((1f - life) / 0.2f).coerceIn(0f, 1f)
            val color = TonalPalette.brightness(Color(0xFFCFF3FF), brightness)
            val alpha = 0.4f * fade * dim
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha * 0.3f),
                radius = b.radius,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = b.radius,
                center = Offset(x, y),
                style = Stroke(width = 1f),
            )
        }
    }
}

private data class AquariumFishSeed(
    val orbitRadius: Float,
    val angleOffset: Float,
    val angularSpeed: Float,
    val scale: Float,
    val verticalTilt: Float,
    val colorIndex: Int,
)

private data class AquariumBubbleSeed(
    val x0: Float,
    val y0: Float,
    val riseSpeed: Float,
    val radius: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float,
)
