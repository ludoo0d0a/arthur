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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class MoteSeed(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
)

/** A curled-up sleeping pet silhouette, breathing slowly on a cozy backdrop. */
@Composable
internal fun SleepingPetEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val moteCount = qualityCount(quality, low = 0, medium = 1, high = 2)
    val motes = remember(moteCount) {
        List(moteCount) { i ->
            MoteSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                driftX = seededRange(i * 41 + 11, 0.02f, 0.05f),
                driftY = seededRange(i * 53 + 13, 0.015f, 0.04f),
                phaseOffset = seededRange(i * 61 + 17, 0f, 2f * PI.toFloat()),
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sleeping_pet_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breath",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )

    val restingDim = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val centerY = h * 0.55f
        val minDim = w.coerceAtMost(h)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3B2A22), Color(0xFF241A16), Color(0xFF150F0D)),
                center = Offset(centerX, centerY),
                radius = w.coerceAtLeast(h) * 0.8f,
            ),
        )

        val breathT = phase01(breathAnim)
        val breathFactor = sin(breathT * 2f * PI.toFloat()) * 0.5f + 0.5f
        val breathAmplitude = if (isActive) 0.03f else 0.01f
        val scale = 1f + breathAmplitude * breathFactor

        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A1D18), TonalPalette.pick(paletteColors, 0), 0.3f),
            brightness * restingDim,
        )

        val bodyRadiusX = minDim * 0.32f * scale
        val bodyRadiusY = minDim * 0.22f * scale
        val bodyCenter = Offset(centerX, centerY + minDim * 0.05f)

        val headRadius = minDim * 0.16f * scale
        val headCenter = Offset(
            centerX - bodyRadiusX * 0.55f,
            centerY - bodyRadiusY * 0.35f,
        )

        val tuckRadius = minDim * 0.12f * scale
        val tuckCenter = Offset(
            centerX + bodyRadiusX * 0.35f,
            centerY - bodyRadiusY * 0.15f,
        )

        drawOval(
            color = bodyColor,
            topLeft = Offset(bodyCenter.x - bodyRadiusX, bodyCenter.y - bodyRadiusY),
            size = Size(bodyRadiusX * 2f, bodyRadiusY * 2f),
        )
        drawCircle(
            color = bodyColor,
            radius = tuckRadius,
            center = tuckCenter,
        )
        drawCircle(
            color = bodyColor,
            radius = headRadius,
            center = headCenter,
        )

        val time = phase01(driftAnim)
        motes.forEach { mote ->
            val mx = phase01(mote.x0 + time * mote.driftX) * w
            val my = phase01(mote.y0 + time * mote.driftY + 0.02f * sin(time * 2f * PI.toFloat() + mote.phaseOffset)) * h
            val glowAlpha = (0.18f + 0.10f * (0.5f + 0.5f * sin(time * 2f * PI.toFloat() + mote.phaseOffset))) * restingDim
            val glowColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
            val glowRadius = minDim * 0.02f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(glowColor, glowAlpha), Color.Transparent),
                    center = Offset(mx, my),
                    radius = glowRadius * 3f,
                ),
                radius = glowRadius * 3f,
                center = Offset(mx, my),
            )
        }
    }
}
