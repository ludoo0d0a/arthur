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

@Composable
internal fun FallingLeavesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 18, medium = 30, high = 48)
    val leaves = remember(count) {
        List(count) { i ->
            LeafSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.12f, 0.3f),
                size = seededRange(i * 67 + 19, 7f, 16f),
                swayAmp = seededRange(i * 53 + 13, 0.06f, 0.16f),
                swayFreq = seededRange(i * 71 + 23, 0.2f, 0.7f),
                swayPhase = seededRange(i * 83 + 29, 0f, 2f * PI.toFloat()),
                rotSpeed = seededRange(i * 97 + 31, 0.1f, 0.6f),
                phaseOffset = seededRange(i * 103 + 37, 0f, 360f),
                colorIndex = i,
                glow = seededRange(i * 109 + 41, 0.4f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "falling_leaves")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "falling_leaves_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF10161F), Color(0xFF020617)),
            ),
        )
        val time = phase01(t)
        leaves.forEach { leaf ->
            val fallPhase = phase01(leaf.y0 + leaf.fallSpeed * time)
            val y = fallPhase * h
            val swayTime = time * 2f * PI.toFloat() * leaf.swayFreq + leaf.swayPhase
            val x = phase01(leaf.x0 + leaf.swayAmp * sin(swayTime)) * w
            val rotation = time * 360f * leaf.rotSpeed + leaf.phaseOffset
            val leafSize = leaf.size * (if (isActive) 1f else 0.85f)

            val base = TonalPalette.pick(paletteColors, leaf.colorIndex)
            val tinted = TonalPalette.brightness(base, brightness * (0.75f + 0.4f * leaf.glow))
            val alpha = if (isActive) (0.55f + 0.4f * leaf.glow).coerceAtMost(1f) else (0.3f + 0.2f * leaf.glow)
            val color = TonalPalette.withAlpha(tinted, alpha)

            val leafPath = buildLeafPath(leafSize)
            withTransform({
                rotate(degrees = rotation, pivot = Offset(x, y))
            }) {
                translate(left = x, top = y) {
                    drawPath(path = leafPath, color = color)
                }
            }
        }
    }
}

private fun buildLeafPath(leafSize: Float): Path {
    val half = leafSize * 0.5f
    return Path().apply {
        moveTo(0f, half)
        quadraticTo(half * 0.9f, half * 0.2f, 0f, -half)
        quadraticTo(-half * 0.9f, half * 0.2f, 0f, half)
        close()
    }
}

private data class LeafSeed(
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
    val glow: Float,
)
