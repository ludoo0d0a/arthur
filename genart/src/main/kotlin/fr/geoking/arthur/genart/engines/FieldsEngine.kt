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
import androidx.compose.ui.graphics.drawscope.Fill
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

private val wheatGold = Color(0xFFD9B66B)
private val sageGreen = Color(0xFF9CAA7A)
private val oliveColor = Color(0xFF7C7A45)
private val dustyRose = Color(0xFFC48D82)
private val warmFieldColors = listOf(wheatGold, sageGreen, oliveColor, dustyRose)

private const val SAMPLE_COUNT = 7

/** Wide horizontal bands of muted farmland color with a wavy, wind-swayed top edge. */
@Composable
internal fun FieldsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bandCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val bands = remember(bandCount) {
        List(bandCount) { i ->
            FieldBandSeed(
                phaseOffset = seededRange(i * 37 + 3, 0f, 2f * PI.toFloat()),
                swayAmp = seededRange(i * 53 + 11, 0.25f, 0.55f),
                freq = seededRange(i * 61 + 17, 1.4f, 2.6f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fields")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fields_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFCFE0E8), Color(0xFFE9DFC4)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val swayScale = if (isActive) 1f else 0.3f
        bands.forEachIndexed { i, band ->
            val topFrac = i / bandCount.toFloat()
            val bottomFrac = (i + 1) / bandCount.toFloat()
            val bandHeight = (bottomFrac - topFrac) * h
            val amplitude = bandHeight * band.swayAmp * 0.5f * swayScale
            val bottomY = bottomFrac * h
            val path = Path().apply {
                moveTo(0f, bottomY)
                for (j in 0..SAMPLE_COUNT) {
                    val xFrac = j / SAMPLE_COUNT.toFloat()
                    val x = xFrac * w
                    val wave = sin(time + band.phaseOffset + xFrac * band.freq) * amplitude
                    val y = topFrac * h + wave
                    lineTo(x, y)
                }
                lineTo(w, bottomY)
                close()
            }
            val warmBase = TonalPalette.pick(warmFieldColors, band.colorIndex)
            val tint = TonalPalette.pick(paletteColors, band.colorIndex)
            val mixed = TonalPalette.mix(warmBase, tint, 0.35f)
            val depthFactor = 0.55f + 0.45f * (i / (bandCount - 1).coerceAtLeast(1).toFloat())
            val color = TonalPalette.brightness(mixed, brightness * depthFactor)
            drawPath(path = path, color = color, style = Fill)
        }
    }
}

private data class FieldBandSeed(
    val phaseOffset: Float,
    val swayAmp: Float,
    val freq: Float,
    val colorIndex: Int,
)
