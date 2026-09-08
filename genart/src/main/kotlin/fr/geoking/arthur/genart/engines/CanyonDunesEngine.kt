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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Warm red-rock canyon dunes with wind-drift ridges and a faint heat-haze shimmer near the horizon. */
@Composable
internal fun CanyonDunesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val layerCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val grainCount = qualityCount(quality, low = 10, medium = 18, high = 28)
    val hazeCount = qualityCount(quality, low = 1, medium = 2, high = 3)

    val layers = remember(layerCount) {
        List(layerCount) { i ->
            val depthFrac = if (layerCount > 1) i / (layerCount - 1f) else 0.5f
            CanyonDuneLayerSeed(
                depthFrac = depthFrac,
                baseYFrac = 0.42f + depthFrac * 0.36f + seededRange(i * 13 + 5, -0.03f, 0.03f),
                ampFrac1 = seededRange(i * 17 + 7, 0.012f, 0.032f),
                freq1 = seededRange(i * 19 + 11, 0.006f, 0.013f),
                phase1 = seededRange(i * 23 + 13, 0f, 2f * PI.toFloat()),
                ampFrac2 = seededRange(i * 29 + 17, 0.006f, 0.016f),
                freq2 = seededRange(i * 31 + 19, 0.014f, 0.026f),
                phase2 = seededRange(i * 37 + 23, 0f, 2f * PI.toFloat()),
                driftSpeed = seededRange(i * 41 + 29, 0.015f, 0.05f) * (0.4f + depthFrac * 0.9f),
                colorIndex = i,
            )
        }
    }
    val grains = remember(grainCount) {
        List(grainCount) { i ->
            CanyonSandGrainSeed(
                x0 = seededUnit(i * 43 + 3),
                y0Frac = seededRange(i * 47 + 7, 0.08f, 0.62f),
                speedMul = seededRange(i * 53 + 11, 0.4f, 1.4f),
                jitterAmpFrac = seededRange(i * 59 + 13, 0.004f, 0.018f),
                jitterFreq = seededRange(i * 61 + 17, 0.6f, 2.2f),
                jitterPhase = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                length = seededRange(i * 71 + 23, 4f, 12f),
                colorIndex = i,
            )
        }
    }
    val hazeBands = remember(hazeCount) {
        List(hazeCount) { i ->
            CanyonHazeBandSeed(
                yFrac = 0.62f + seededRange(i * 79 + 31, -0.03f, 0.05f),
                thicknessFrac = seededRange(i * 83 + 37, 0.008f, 0.016f),
                widthFrac = seededRange(i * 89 + 41, 0.75f, 0.92f),
                shimmerAmpFrac = seededRange(i * 97 + 43, 0.004f, 0.01f),
                shimmerFreq = seededRange(i * 101 + 47, 1.2f, 2.4f),
                shimmerPhase = seededRange(i * 103 + 53, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "canyondunes")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "canyondunes_t",
    )

    val driftScale = if (isActive) 1f else 0.3f
    val sandAlphaScale = if (isActive) 1f else 0.4f
    val hazeScale = if (isActive) 1f else 0.3f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.55f)
        val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.32f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, Color(0xFF120705)),
            ),
        )

        val segments = (w / 8f).toInt().coerceIn(16, 160)
        val step = w / segments

        layers.forEach { layer ->
            val driftPhase = phase01(t * layer.driftSpeed * driftScale) * 2f * PI.toFloat()
            val baseY = layer.baseYFrac * h
            val amp1 = layer.ampFrac1 * h
            val amp2 = layer.ampFrac2 * h

            val path = Path()
            val firstY = baseY +
                amp1 * sin(layer.phase1 + driftPhase) +
                amp2 * sin(layer.phase2 + driftPhase)
            path.moveTo(0f, firstY)
            for (s in 1..segments) {
                val x = s * step
                val y = baseY +
                    amp1 * sin(x * layer.freq1 + layer.phase1 + driftPhase) +
                    amp2 * sin(x * layer.freq2 + layer.phase2 + driftPhase)
                path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            val base = TonalPalette.pick(paletteColors, layer.colorIndex)
            val dimmed = TonalPalette.brightness(base, brightness * (0.4f + layer.depthFrac * 0.6f))
            val alpha = (0.35f + layer.depthFrac * 0.6f).coerceIn(0.2f, 0.95f)
            drawPath(path = path, color = TonalPalette.withAlpha(dimmed, alpha))
        }

        hazeBands.forEach { band ->
            val shimmer = sin(t * band.shimmerFreq * 2f * PI.toFloat() + band.shimmerPhase) *
                band.shimmerAmpFrac * hazeScale
            val bandWidth = band.widthFrac * w
            val centerX = w * 0.5f + shimmer * w
            val y = band.yFrac * h
            val thickness = band.thicknessFrac * h
            val base = TonalPalette.pick(paletteColors, band.colorIndex)
            val tinted = TonalPalette.brightness(base, brightness * 0.85f)
            drawRect(
                color = TonalPalette.withAlpha(tinted, 0.05f * hazeScale),
                topLeft = Offset(centerX - bandWidth / 2f, y - thickness / 2f),
                size = Size(bandWidth, thickness),
            )
        }

        val sandColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
        grains.forEach { grain ->
            val xFrac = phase01(grain.x0 + t * grain.speedMul * driftScale)
            val x = xFrac * w
            val jitterT = t * grain.jitterFreq * 2f * PI.toFloat() + grain.jitterPhase
            val y = grain.y0Frac * h + sin(jitterT) * grain.jitterAmpFrac * h
            val alpha = (0.12f + 0.18f * (1f - grain.y0Frac)) * sandAlphaScale * brightness.coerceAtMost(1f)
            drawLine(
                color = TonalPalette.withAlpha(sandColor, alpha.coerceIn(0f, 0.6f)),
                start = Offset(x - grain.length, y),
                end = Offset(x, y),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class CanyonDuneLayerSeed(
    val depthFrac: Float,
    val baseYFrac: Float,
    val ampFrac1: Float,
    val freq1: Float,
    val phase1: Float,
    val ampFrac2: Float,
    val freq2: Float,
    val phase2: Float,
    val driftSpeed: Float,
    val colorIndex: Int,
)

private data class CanyonSandGrainSeed(
    val x0: Float,
    val y0Frac: Float,
    val speedMul: Float,
    val jitterAmpFrac: Float,
    val jitterFreq: Float,
    val jitterPhase: Float,
    val length: Float,
    val colorIndex: Int,
)

private data class CanyonHazeBandSeed(
    val yFrac: Float,
    val thicknessFrac: Float,
    val widthFrac: Float,
    val shimmerAmpFrac: Float,
    val shimmerFreq: Float,
    val shimmerPhase: Float,
    val colorIndex: Int,
)
