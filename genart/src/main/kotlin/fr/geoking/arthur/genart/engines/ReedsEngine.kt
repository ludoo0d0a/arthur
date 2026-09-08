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
internal fun ReedsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 12, medium = 20, high = 32)
    val reeds = remember(count) {
        List(count) { i ->
            val depth = i % 2
            val jitter = seededRange(i * 13 + 5, -0.5f / count, 0.5f / count)
            ReedSeed(
                x0 = ((i + 0.5f) / count) + jitter,
                heightFrac = if (depth == 0) {
                    seededRange(i * 17 + 3, 0.45f, 0.65f)
                } else {
                    seededRange(i * 17 + 3, 0.65f, 0.92f)
                },
                widthPx = if (depth == 0) {
                    seededRange(i * 29 + 7, 1.5f, 2.5f)
                } else {
                    seededRange(i * 29 + 7, 2.5f, 4f)
                },
                phaseOffset = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat()),
                swayAmp = if (depth == 0) {
                    seededRange(i * 53 + 13, 0.03f, 0.06f)
                } else {
                    seededRange(i * 53 + 13, 0.05f, 0.1f)
                },
                colorIndex = i,
                depth = depth,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reeds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reeds_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF03141A), Color(0xFF0A2C30)),
            ),
        )
        val waterTint = TonalPalette.pick(paletteColors, 0)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(waterTint, 0f),
                    TonalPalette.withAlpha(waterTint, 0.22f),
                ),
            ),
            topLeft = Offset(0f, h * 0.72f),
            size = Size(w, h * 0.28f),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val swayScale = if (isActive) 1f else 0.4f
        val activeReeds = if (isActive) reeds else reeds.filterIndexed { index, _ -> index % 2 == 0 }
        activeReeds.filter { it.depth == 0 }.forEach { reed ->
            drawReed(reed, w, h, time, swayScale, paletteColors, brightness)
        }
        activeReeds.filter { it.depth == 1 }.forEach { reed ->
            drawReed(reed, w, h, time, swayScale, paletteColors, brightness)
        }
    }
}

private fun DrawScope.drawReed(
    reed: ReedSeed,
    w: Float,
    h: Float,
    time: Float,
    swayScale: Float,
    paletteColors: List<Color>,
    brightness: Float,
) {
    val baseX = reed.x0 * w
    val baseY = h
    val reedHeight = reed.heightFrac * h
    val sway = sin(time + reed.phaseOffset + reed.x0 * 6f) * reed.swayAmp * swayScale
    val controlX = baseX + sway * w * 0.5f
    val controlY = h - reedHeight * 0.55f
    val tipSway = sin(time + reed.phaseOffset + reed.x0 * 6f + 0.3f) * reed.swayAmp * swayScale * 1.6f
    val tipX = baseX + tipSway * w
    val tipY = h - reedHeight
    val path = Path().apply {
        moveTo(baseX, baseY)
        quadraticBezierTo(controlX, controlY, tipX, tipY)
    }
    val base = TonalPalette.pick(paletteColors, reed.colorIndex)
    val depthFactor = if (reed.depth == 0) 0.55f else 1f
    val color = TonalPalette.brightness(base, brightness * depthFactor)
    val alpha = if (reed.depth == 0) 0.5f else 0.85f
    drawPath(
        path = path,
        color = TonalPalette.withAlpha(color, alpha),
        style = Stroke(width = reed.widthPx, cap = StrokeCap.Round),
    )
}

private data class ReedSeed(
    val x0: Float,
    val heightFrac: Float,
    val widthPx: Float,
    val phaseOffset: Float,
    val swayAmp: Float,
    val colorIndex: Int,
    val depth: Int,
)
