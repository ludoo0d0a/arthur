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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.fbm2D
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * A towering, flickering flame with heat distortion — the raw fire effect.
 *
 * Multiple flame tongues rise from the base, each driven by looping fbm noise for
 * organic sway and a layered vertical gradient for the characteristic bright core
 * fading into dark smoke at the tip.
 */
@Composable
internal fun FireEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val tongueCount = qualityCount(quality, low = 3, medium = 5, high = 8)
    val tongues = remember(tongueCount) {
        List(tongueCount) { i ->
            TongueSeed(
                baseX = seededUnit(i * 17 + 3),
                widthFrac = seededRange(i * 29 + 7, 0.04f, 0.12f),
                heightFrac = seededRange(i * 41 + 11, 0.4f, 0.85f),
                swayAmp = seededRange(i * 53 + 13, 0.02f, 0.08f),
                swayFreq = seededRange(i * 67 + 19, 0.8f, 2.5f),
                flickerFreq = seededRange(i * 79 + 23, 1.5f, 4.0f),
                flickerPhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fire")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fire_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        drawRect(color = Color(0xFF050201))
        val time = phase01(t) * (2f * PI.toFloat())
        val activeDim = if (isActive) 1f else 0.55f
        for (index in 0 until tongues.size) {
            val tongue = tongues[index]
            val noiseX = fbm2D(time * 0.5f, tongue.baseX * 10f, octaves = 2, seedOffset = tongue.colorIndex * 101)
            val sway = sin(time * tongue.swayFreq + tongue.flickerPhase) * tongue.swayAmp
            val fRange = flickerRange(time, tongue.flickerFreq, tongue.flickerPhase)
            val baseX = phase01(tongue.baseX + sway * noiseX) * w
            val breath = 0.85f + 0.15f * loopedFbm(time * 0.3f, radius = 1.0f, octaves = 2, seedOffset = tongue.colorIndex * 307)
            val height = tongue.heightFrac * breath * minDim
            val halfWidth = tongue.widthFrac * minDim * fRange
            val tint = TonalPalette.pick(paletteColors, tongue.colorIndex)
            val flameColor = TonalPalette.mix(tint, Color(0xFFFFAA33), 0.3f)
            val brightCore = TonalPalette.brightness(flameColor, brightness * 1.2f * activeDim)
            val outerFlame = TonalPalette.brightness(flameColor, brightness * 0.6f * activeDim)
            val smokeColor = TonalPalette.brightness(Color(0xFF1A0A04), brightness * 0.3f * activeDim)
            val tipY = (1f - height / minDim * 0.9f) * h
            val baseY = h - h * 0.08f
            val f = fRange
            val path = Path().apply {
                moveTo(baseX - halfWidth * f, baseY)
                cubicTo(
                    baseX - halfWidth * 1.3f * f, tipY + height * 0.3f,
                    baseX + halfWidth * 1.3f * f, tipY + height * 0.3f,
                    baseX + halfWidth * f, tipY
                )
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        smokeColor.copy(alpha = 0f),
                        smokeColor.copy(alpha = 0.4f),
                        outerFlame.copy(alpha = 0.7f),
                        brightCore.copy(alpha = 0.9f),
                        brightCore.copy(alpha = 0f),
                    ),
                    startY = tipY,
                    endY = baseY,
                ),
                style = Fill,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brightCore.copy(alpha = 0.6f),
                        brightCore.copy(alpha = 0f),
                    ),
                    center = Offset(baseX, tipY + height * 0.15f),
                    radius = (halfWidth * 0.5f * f).coerceAtLeast(1f),
                ),
                radius = (halfWidth * 0.5f * f).coerceAtLeast(1f),
                center = Offset(baseX, tipY + height * 0.15f),
            )
        }
    }
}

private fun flickerRange(time: Float, freq: Float, phase: Float): Float {
    val f = 0.5f + 0.3f * sin(time * freq + phase) + 0.2f * sin(time * freq * 2.3f + phase * 1.7f)
    return f.coerceIn(0.2f, 1f)
}

private data class TongueSeed(
    val baseX: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val flickerFreq: Float,
    val flickerPhase: Float,
    val colorIndex: Int,
)
