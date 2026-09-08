package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft nebula color clouds drifting slowly across a deep sky. */
@Composable
internal fun NebulaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 8)
    val clouds = remember(count) {
        List(count) { i ->
            NebulaCloud(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                speedX = seededRange(i * 41 + 11, 0.02f, 0.08f),
                speedY = seededRange(i * 53 + 13, 0.01f, 0.05f),
                radiusFrac = seededRange(i * 67 + 19, 0.22f, 0.48f),
                pulseFreq = seededRange(i * 79 + 23, 0.15f, 0.5f),
                pulsePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 97 + 31, 0.1f, 0.28f),
                colorIndex = i,
            )
        }
    }
    val starCount = qualityCount(quality, low = 20, medium = 32, high = 48)
    val stars = remember(starCount) {
        List(starCount) { j ->
            Triple(
                seededUnit(j * 11 + 5),
                seededUnit(j * 23 + 9),
                seededRange(j * 37 + 13, 0.1f, 0.5f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "nebula")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nebula_t",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF030208))
            stars.forEach { (sx, sy, a) ->
                drawCircle(
                    color = Color.White.copy(alpha = a * 0.7f),
                    radius = 1f,
                    center = Offset(sx * w, sy * h),
                )
            }
        }
        // Real gaussian blur on the gas clouds only — keeps the star pinpoints crisp above.
        Canvas(modifier = Modifier.fillMaxSize().blur(20.dp)) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.55f
            clouds.forEach { cloud ->
                val x = phase01(cloud.x0 + time * cloud.speedX) * w
                val y = phase01(cloud.y0 + time * cloud.speedY) * h
                val pulse = 0.85f + 0.15f * sin(time * 2f * PI.toFloat() * cloud.pulseFreq + cloud.pulsePhase)
                val radius = cloud.radiusFrac * minDim * pulse
                val base = TonalPalette.pick(paletteColors, cloud.colorIndex)
                val tint = TonalPalette.brightness(base, brightness)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, cloud.alphaBase * dim),
                            TonalPalette.withAlpha(tint, cloud.alphaBase * 0.35f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private data class NebulaCloud(
    val x0: Float,
    val y0: Float,
    val speedX: Float,
    val speedY: Float,
    val radiusFrac: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
