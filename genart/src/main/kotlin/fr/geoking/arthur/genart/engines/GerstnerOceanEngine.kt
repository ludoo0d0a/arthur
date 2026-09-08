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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.cos
import kotlin.math.sin

/** Open-water surface built from stacked bands of summed Gerstner-like sine wave trains. */
@Composable
internal fun GerstnerOceanEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bandCount = qualityCount(quality, low = 3, medium = 4, high = 4)
    val trainCount = qualityCount(quality, low = 3, medium = 4, high = 4)
    val sampleCount = qualityCount(quality, low = 24, medium = 28, high = 32)

    val bands = remember(bandCount, trainCount) {
        List(bandCount) { i ->
            val depthFrac = if (bandCount > 1) i / (bandCount - 1f) else 0.5f
            val bandAmpFrac = 0.018f + depthFrac * 0.05f
            val freqBase = 5f + (1f - depthFrac) * 9f
            val trains = List(trainCount) { j ->
                val hash = i * 191 + j * 37
                WaveTrainSeed(
                    ampFrac = seededRange(hash + 3, 0.5f, 1f) * (bandAmpFrac / trainCount),
                    freq = seededRange(hash + 7, 0.75f, 1.25f) * freqBase,
                    angle = seededRange(hash + 11, -0.55f, 0.55f),
                    phaseSpeed = seededRange(hash + 13, 0.35f, 0.95f) * (if (j % 2 == 0) 1f else -0.6f),
                    phase = seededRange(hash + 17, 0f, 6.2832f),
                )
            }
            SwellBandSeed(
                depthFrac = depthFrac,
                baseYFrac = 0.30f + depthFrac * 0.58f + seededRange(i * 13 + 5, -0.02f, 0.02f),
                colorIndex = i,
                trains = trains,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "gerstner_ocean")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gerstner_ocean_t",
    )

    val motionScale = if (isActive) 1f else 0.35f
    val dimming = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF08324A), Color(0xFF01111C)),
            ),
        )

        val step = w / sampleCount
        val tScaled = t * motionScale

        fun waveY(band: SwellBandSeed, xFrac: Float): Float {
            var offset = 0f
            band.trains.forEach { train ->
                offset += train.ampFrac * h * sin(
                    train.freq * xFrac * cos(train.angle) * 6.2832f +
                        tScaled * train.phaseSpeed * 6.2832f +
                        train.phase,
                )
            }
            return band.baseYFrac * h + offset
        }

        bands.forEach { band ->
            val path = Path()
            path.moveTo(0f, waveY(band, 0f))
            for (s in 1..sampleCount) {
                val x = s * step
                path.lineTo(x, waveY(band, x / w))
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            val base = TonalPalette.pick(paletteColors, band.colorIndex)
            val colorFactor = (brightness * (0.95f - band.depthFrac * 0.4f)).coerceAtLeast(0.15f)
            val tinted = TonalPalette.brightness(base, colorFactor)
            val alpha = ((0.35f + band.depthFrac * 0.55f) * dimming).coerceIn(0.2f, 0.95f)
            drawPath(path = path, color = TonalPalette.withAlpha(tinted, alpha))

            if (band.depthFrac > 0.45f) {
                val crest = Path()
                crest.moveTo(0f, waveY(band, 0f))
                for (s in 1..sampleCount) {
                    val x = s * step
                    crest.lineTo(x, waveY(band, x / w))
                }
                val foamAlpha = (0.1f + band.depthFrac * 0.1f) * dimming * brightness.coerceAtMost(1f)
                drawPath(
                    path = crest,
                    color = TonalPalette.withAlpha(Color.White, foamAlpha.coerceIn(0f, 0.22f)),
                    style = Stroke(width = 1.4f),
                )
            }
        }
    }
}

private data class WaveTrainSeed(
    val ampFrac: Float,
    val freq: Float,
    val angle: Float,
    val phaseSpeed: Float,
    val phase: Float,
)

private data class SwellBandSeed(
    val depthFrac: Float,
    val baseYFrac: Float,
    val colorIndex: Int,
    val trains: List<WaveTrainSeed>,
)
