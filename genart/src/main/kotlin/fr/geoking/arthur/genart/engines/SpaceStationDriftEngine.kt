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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/**
 * A single small angular station silhouette drifting diagonally over the [StarFieldEngine] backdrop,
 * rotating only a few degrees over its whole slow loop and fading in/out at the frame edges.
 */
@Composable
internal fun SpaceStationDriftEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val startXFrac = remember { seededRange(4201, -0.1f, 0.15f) }
    val startYFrac = remember { seededRange(4211, 0.55f, 0.85f) }
    val endXFrac = remember { seededRange(4221, 0.85f, 1.1f) }
    val endYFrac = remember { seededRange(4231, 0.1f, 0.35f) }
    val rotationStartDeg = remember { seededRange(4241, -6f, 6f) }
    val rotationEndDeg = remember { seededRange(4251, -6f, 6f) }

    val transition = rememberInfiniteTransition(label = "spacestation")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spacestation_drift",
    )

    val dim = if (isActive) 1f else 0.6f

    Box(modifier = modifier) {
        StarFieldEngine(isActive, paletteColors, quality, brightness, speed, Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = phase01(driftT)
            val x = startXFrac * w + (endXFrac - startXFrac) * w * t
            val y = startYFrac * h + (endYFrac - startYFrac) * h * t
            val rotationDeg = rotationStartDeg + (rotationEndDeg - rotationStartDeg) * t
            val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val alpha = 0.6f * dim * edgeFade
            if (alpha > 0.01f) {
                val minDim = w.coerceAtMost(h)
                val moduleSize = minDim * 0.05f
                val fill = Color(0xFF232B3A).copy(alpha = alpha)
                rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
                    drawRect(
                        color = fill,
                        topLeft = Offset(x - moduleSize * 0.8f, y - moduleSize * 0.25f),
                        size = Size(moduleSize * 1.6f, moduleSize * 0.5f),
                    )
                    drawRect(
                        color = fill,
                        topLeft = Offset(x - moduleSize * 0.2f, y - moduleSize * 0.7f),
                        size = Size(moduleSize * 0.4f, moduleSize * 1.4f),
                    )
                    drawOval(
                        color = fill,
                        topLeft = Offset(x - moduleSize * 0.9f, y - moduleSize * 0.45f),
                        size = Size(moduleSize * 0.5f, moduleSize * 0.5f),
                    )
                }
            }
        }
    }
}
