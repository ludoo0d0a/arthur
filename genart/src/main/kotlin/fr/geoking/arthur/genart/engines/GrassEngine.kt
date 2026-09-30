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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun GrassEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 24, medium = 40, high = 64)
    val blades = remember(count) {
        List(count) { i ->
            val depth = i % 2
            val jitter = seededRange(i * 13 + 5, -0.35f / count, 0.35f / count)
            BladeSeed(
                x0 = ((i + 0.5f) / count) + jitter,
                heightFrac = if (depth == 0) {
                    seededRange(i * 17 + 3, 0.28f, 0.45f)
                } else {
                    seededRange(i * 17 + 3, 0.45f, 0.72f)
                },
                widthPx = if (depth == 0) {
                    seededRange(i * 29 + 7, 2f, 4f)
                } else {
                    seededRange(i * 29 + 7, 4f, 7f)
                },
                phaseOffset = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat()),
                swayAmp = if (depth == 0) {
                    seededRange(i * 53 + 13, 0.04f, 0.09f)
                } else {
                    seededRange(i * 53 + 13, 0.08f, 0.16f)
                },
                colorIndex = i,
                depth = depth,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "grass")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "grass_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF05090F), Color(0xFF101B2B)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val swayScale = if (isActive) 1f else 0.4f
        val activeBlades = if (isActive) blades else blades.filterIndexed { index, _ -> index % 2 == 0 }
        activeBlades.filter { it.depth == 0 }.forEach { blade ->
            drawBlade(blade, w, h, time, swayScale, paletteColors, brightness)
        }
        activeBlades.filter { it.depth == 1 }.forEach { blade ->
            drawBlade(blade, w, h, time, swayScale, paletteColors, brightness)
        }
    }
}

private fun DrawScope.drawBlade(
    blade: BladeSeed,
    w: Float,
    h: Float,
    time: Float,
    swayScale: Float,
    paletteColors: List<Color>,
    brightness: Float,
) {
    val baseX = blade.x0 * w
    val baseY = h
    val bladeHeight = blade.heightFrac * h
    val sway = sin(time + blade.phaseOffset + blade.x0 * 6f) * blade.swayAmp * swayScale
    val controlX = baseX + sway * w * 0.5f
    val controlY = h - bladeHeight * 0.55f
    val tipSway = sin(time + blade.phaseOffset + blade.x0 * 6f + 0.3f) * blade.swayAmp * swayScale * 1.6f
    val tipX = baseX + tipSway * w
    val tipY = h - bladeHeight
    val path = Path().apply {
        moveTo(baseX, baseY)
        quadraticTo(controlX, controlY, tipX, tipY)
    }
    val base = TonalPalette.pick(paletteColors, blade.colorIndex)
    val depthFactor = if (blade.depth == 0) 0.55f else 1f
    val color = TonalPalette.brightness(base, brightness * depthFactor)
    val alpha = if (blade.depth == 0) 0.5f else 0.85f
    drawPath(
        path = path,
        color = TonalPalette.withAlpha(color, alpha),
        style = Stroke(width = blade.widthPx, cap = StrokeCap.Round),
    )
}

private data class BladeSeed(
    val x0: Float,
    val heightFrac: Float,
    val widthPx: Float,
    val phaseOffset: Float,
    val swayAmp: Float,
    val colorIndex: Int,
    val depth: Int,
)
