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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit

private val hazeColor = Color(0xFF3B4A6B)
private val moonColor = Color(0xFFF7F1DE)

@Composable
internal fun MountainsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val layerCount = qualityCount(quality, low = 3, medium = 4, high = 6)
    val layers = remember(layerCount) {
        List(layerCount) { i ->
            val distanceT = 1f - i / (layerCount - 1).coerceAtLeast(1).toFloat()
            val peakCount = 6 + (seededUnit(i * 100 + 77) * 5f).toInt().coerceIn(0, 4)
            val peakHeights = List(peakCount) { j ->
                seededUnit((i * 100 + j) * 13 + 5)
            }
            MountainLayerSeed(
                peakHeights = peakHeights,
                distanceT = distanceT,
                baselineFrac = fracLerp(0.78f, 0.35f, distanceT),
                amplitudeFrac = fracLerp(0.22f, 0.05f, distanceT),
                speedFactor = fracLerp(1f, 0.15f, distanceT),
                colorIndex = i,
            )
        }
    }
    val stars = remember {
        List(26) { i ->
            MountainSkyStarSeed(
                xFrac = seededUnit(i * 31 + 4001),
                yFrac = seededRange(i * 47 + 4002, 0.03f, 0.42f),
                radius = seededRange(i * 59 + 4003, 0.8f, 2.2f),
                alpha = seededRange(i * 71 + 4004, 0.25f, 0.85f),
            )
        }
    }
    val moonXFrac = remember { seededRange(9001, 0.62f, 0.86f) }
    val moonYFrac = remember { seededRange(9002, 0.12f, 0.28f) }

    val transition = rememberInfiniteTransition(label = "mountains")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((45000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mountains_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF10142B), Color(0xFF020617)),
            ),
        )

        val glowFactor = if (isActive) 1f else 0.55f
        val moonRadius = minOf(w, h) * 0.07f
        val moonCenter = Offset(w * moonXFrac, h * moonYFrac)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(moonColor, 0.85f * brightness.coerceAtMost(1.3f) * glowFactor),
                    TonalPalette.withAlpha(moonColor, 0.16f * glowFactor),
                    Color.Transparent,
                ),
                center = moonCenter,
                radius = moonRadius * 4.5f,
            ),
            radius = moonRadius * 4.5f,
            center = moonCenter,
        )
        drawCircle(
            color = TonalPalette.withAlpha(moonColor, (0.9f * brightness.coerceAtMost(1.2f)).coerceIn(0f, 1f)),
            radius = moonRadius,
            center = moonCenter,
        )

        stars.forEach { star ->
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, star.alpha * brightness.coerceAtMost(1.2f)),
                radius = star.radius,
                center = Offset(w * star.xFrac, h * star.yFrac),
            )
        }

        val driftDamp = if (isActive) 1f else 0.2f
        layers.forEach { layer ->
            val path = buildRidgePath(layer, w, h)
            val baseColor = TonalPalette.pick(paletteColors, layer.colorIndex)
            val tinted = TonalPalette.mix(baseColor, hazeColor, layer.distanceT * 0.7f)
            val brightnessFactor = fracLerp(0.9f, 0.35f, layer.distanceT) * brightness.coerceIn(0f, 1.5f)
            val alpha = fracLerp(0.95f, 0.3f, layer.distanceT)
            val color = TonalPalette.withAlpha(TonalPalette.brightness(tinted, brightnessFactor), alpha)

            val driftOffset = phase01(t * layer.speedFactor * driftDamp) * w
            translate(left = -driftOffset) {
                drawPath(path = path, color = color, style = Fill)
            }
            translate(left = -driftOffset + w) {
                drawPath(path = path, color = color, style = Fill)
            }
        }
    }
}

private fun buildRidgePath(layer: MountainLayerSeed, w: Float, h: Float): Path {
    val path = Path()
    val baselineY = h * layer.baselineFrac
    val amplitude = h * layer.amplitudeFrac
    val peakCount = layer.peakHeights.size
    val step = w / peakCount
    path.moveTo(0f, h)
    path.lineTo(0f, baselineY - layer.peakHeights[0] * amplitude)
    for (j in 1..peakCount) {
        val x = j * step
        val heightSeed = layer.peakHeights[j % peakCount]
        val y = baselineY - heightSeed * amplitude
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.close()
    return path
}

private fun fracLerp(from: Float, to: Float, t: Float): Float =
    from + (to - from) * t.coerceIn(0f, 1f)

private data class MountainLayerSeed(
    val peakHeights: List<Float>,
    val distanceT: Float,
    val baselineFrac: Float,
    val amplitudeFrac: Float,
    val speedFactor: Float,
    val colorIndex: Int,
)

private data class MountainSkyStarSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val alpha: Float,
)
