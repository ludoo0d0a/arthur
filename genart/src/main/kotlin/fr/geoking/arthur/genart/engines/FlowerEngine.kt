package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FlowerPetalSeed(
    val angle: Float,
    val radiusFrac: Float,
    val colorIndex: Int,
)

/** One thin stem topped with a soft pastel bloom that sways and breathes gently. */
@Composable
internal fun FlowerEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val petalCount = qualityCount(quality, low = 6, medium = 8, high = 10)
    val petals = remember(petalCount) {
        List(petalCount) { i ->
            FlowerPetalSeed(
                angle = (i.toFloat() / petalCount) * 2f * PI.toFloat(),
                radiusFrac = seededRange(i * 17 + 5, 0.85f, 1.05f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "flower")
    val swayAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flower_sway",
    )
    val breathAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flower_breath",
    )

    val activeScale = if (isActive) 1f else 0.4f
    val activeAlpha = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF12140F), Color(0xFF1B2115)),
            ),
        )

        val swayTime = phase01(swayAnim) * 2f * PI.toFloat()
        val swayAngle = sin(swayTime) * 0.05f * activeScale
        val breathFactor = 1f + sin(breathAnim * PI.toFloat()) * 0.06f * activeScale

        val baseX = w * 0.5f
        val baseY = h * 0.92f
        val stemHeight = h * 0.65f
        val tipX = baseX + sin(swayAngle) * stemHeight
        val tipY = baseY - cos(swayAngle) * stemHeight
        val controlX = baseX + sin(swayAngle) * stemHeight * 0.55f
        val controlY = baseY - stemHeight * 0.55f

        val stemColor = TonalPalette.brightness(TonalPalette.mix(Color(0xFF3F6B34), Color(0xFF5C8A44), 0.4f), brightness)
        val stemPath = Path().apply {
            moveTo(baseX, baseY)
            quadraticBezierTo(controlX, controlY, tipX, tipY)
        }
        drawPath(
            path = stemPath,
            color = TonalPalette.withAlpha(stemColor, 0.85f * activeAlpha),
            style = Stroke(width = minDim * 0.010f, cap = StrokeCap.Round),
        )

        val bloomRadius = minDim * 0.16f * breathFactor
        val petalRadius = minDim * 0.11f * breathFactor

        petals.forEach { petal ->
            val petalCenter = Offset(
                tipX + cos(petal.angle) * bloomRadius * petal.radiusFrac,
                tipY + sin(petal.angle) * bloomRadius * petal.radiusFrac,
            )
            val pastel = TonalPalette.mix(
                TonalPalette.pick(paletteColors, petal.colorIndex),
                pastelFor(petal.colorIndex),
                0.6f,
            )
            val color = TonalPalette.brightness(pastel, brightness)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(color, 0.55f * activeAlpha),
                        TonalPalette.withAlpha(color, 0f),
                    ),
                    center = petalCenter,
                    radius = petalRadius,
                ),
                radius = petalRadius,
                center = petalCenter,
            )
        }

        val center = Offset(tipX, tipY)
        val centerRadius = minDim * 0.045f * breathFactor
        val centerColor = TonalPalette.brightness(Color(0xFFF2C94C), brightness)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(centerColor, 0.75f * activeAlpha),
                    TonalPalette.withAlpha(centerColor, 0f),
                ),
                center = center,
                radius = centerRadius,
            ),
            radius = centerRadius,
            center = center,
        )
    }
}

private fun pastelFor(index: Int): Color = when (index % 3) {
    0 -> Color(0xFFF7C6D9)
    1 -> Color(0xFFD8C6F7)
    else -> Color(0xFFF7EFC6)
}
