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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft pink cherry blossom petals drifting and rotating. */
@Composable
internal fun CherryBlossomsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 16, medium = 28, high = 42)
    val petals = remember(count) {
        List(count) { i ->
            PetalSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.1f, 0.28f),
                size = seededRange(i * 53 + 13, 6f, 14f),
                swayAmp = seededRange(i * 67 + 19, 0.05f, 0.18f),
                swayFreq = seededRange(i * 79 + 23, 0.2f, 0.8f),
                swayPhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                rotSpeed = seededRange(i * 97 + 31, 0.15f, 0.7f),
                phaseOffset = seededRange(i * 103 + 37, 0f, 360f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "cherry_blossoms")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cherry_blossoms_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF18101A), Color(0xFF08060C)),
            ),
        )
        val time = phase01(t)
        petals.forEach { petal ->
            val fall = phase01(petal.y0 + petal.fallSpeed * time)
            val y = fall * h
            val sway = sin(time * 2f * PI.toFloat() * petal.swayFreq + petal.swayPhase)
            val x = phase01(petal.x0 + petal.swayAmp * sway) * w
            val rotation = time * 360f * petal.rotSpeed + petal.phaseOffset
            val petalSize = petal.size * (if (isActive) 1f else 0.85f)
            val pink = Color(0xFFF2B6C8)
            val base = TonalPalette.mix(pink, TonalPalette.pick(paletteColors, petal.colorIndex), 0.25f)
            val tinted = TonalPalette.brightness(base, brightness)
            val alpha = if (isActive) 0.7f else 0.4f
            val color = TonalPalette.withAlpha(tinted, alpha)
            val path = buildPetalPath(petalSize)
            withTransform({
                rotate(degrees = rotation, pivot = Offset(x, y))
            }) {
                translate(left = x, top = y) {
                    drawPath(path = path, color = color)
                }
            }
        }
    }
}

private fun buildPetalPath(size: Float): Path {
    val half = size * 0.5f
    return Path().apply {
        moveTo(0f, half * 0.85f)
        quadraticTo(half * 0.95f, half * 0.15f, 0f, -half)
        quadraticTo(-half * 0.95f, half * 0.15f, 0f, half * 0.85f)
        close()
    }
}

private data class PetalSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val size: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val swayPhase: Float,
    val rotSpeed: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)
