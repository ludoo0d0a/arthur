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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/**
 * A [StarFieldEngine] backdrop with several small irregular rock silhouettes, each tumbling and
 * drifting diagonally at its own slow, calm pace and fading in/out at the frame edges.
 */
@Composable
internal fun AsteroidsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rockCount = qualityCount(quality, low = 3, medium = 5, high = 8)
    val rocks = remember(rockCount) { List(rockCount) { i -> asteroidSeed(i) } }

    val transition = rememberInfiniteTransition(label = "asteroids")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "asteroids_drift",
    )
    val tumbleT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "asteroids_tumble",
    )

    val dim = if (isActive) 1f else 0.6f

    Box(modifier = modifier) {
        StarFieldEngine(isActive, paletteColors, quality, brightness, speed, Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val fill = TonalPalette.brightness(Color(0xFF3A3D45), brightness)
            rocks.forEach { rock ->
                val t = phase01(driftT * rock.driftSpeedMult + rock.driftPhase)
                val x = rock.startXFrac * w + (rock.endXFrac - rock.startXFrac) * w * t
                val y = rock.startYFrac * h + (rock.endYFrac - rock.startYFrac) * h * t
                val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
                val alpha = 0.8f * dim * edgeFade
                if (alpha > 0.01f) {
                    val rotationDeg = phase01(tumbleT * rock.spinSpeedMult + rock.spinPhase) * 360f
                    val rockSize = minDim * (0.018f + rock.sizeUnit * 0.03f)
                    val color = fill.copy(alpha = alpha)
                    rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
                        drawCircle(color = color, radius = rock.r1 * rockSize, center = Offset(x + rock.dx1 * rockSize, y + rock.dy1 * rockSize))
                        drawCircle(color = color, radius = rock.r2 * rockSize, center = Offset(x + rock.dx2 * rockSize, y + rock.dy2 * rockSize))
                        drawCircle(color = color, radius = rock.r3 * rockSize, center = Offset(x + rock.dx3 * rockSize, y + rock.dy3 * rockSize))
                    }
                }
            }
        }
    }
}

private fun asteroidSeed(index: Int): AsteroidSeed {
    val s = index * 613 + 5000
    return AsteroidSeed(
        startXFrac = seededRange(s + 11, -0.15f, 0.2f),
        startYFrac = seededRange(s + 21, -0.15f, 1.15f),
        endXFrac = seededRange(s + 31, 0.8f, 1.15f),
        endYFrac = seededRange(s + 41, -0.15f, 1.15f),
        driftSpeedMult = seededRange(s + 51, 0.6f, 1.4f),
        driftPhase = seededRange(s + 61, 0f, 1f),
        spinSpeedMult = seededRange(s + 71, 0.4f, 1.3f),
        spinPhase = seededRange(s + 81, 0f, 2f * PI.toFloat()),
        sizeUnit = seededRange(s + 91, 0f, 1f),
        dx1 = 0f,
        dy1 = 0f,
        r1 = 1f,
        dx2 = seededRange(s + 101, -0.6f, 0.6f),
        dy2 = seededRange(s + 111, -0.6f, 0.6f),
        r2 = seededRange(s + 121, 0.5f, 0.85f),
        dx3 = seededRange(s + 131, -0.6f, 0.6f),
        dy3 = seededRange(s + 141, -0.6f, 0.6f),
        r3 = seededRange(s + 151, 0.4f, 0.7f),
    )
}

private data class AsteroidSeed(
    val startXFrac: Float,
    val startYFrac: Float,
    val endXFrac: Float,
    val endYFrac: Float,
    val driftSpeedMult: Float,
    val driftPhase: Float,
    val spinSpeedMult: Float,
    val spinPhase: Float,
    val sizeUnit: Float,
    val dx1: Float,
    val dy1: Float,
    val r1: Float,
    val dx2: Float,
    val dy2: Float,
    val r2: Float,
    val dx3: Float,
    val dy3: Float,
    val r3: Float,
)
