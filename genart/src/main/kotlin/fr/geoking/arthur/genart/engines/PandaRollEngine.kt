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
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class PandaRollFirefly(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
)

/** Soft panda silhouette gently rocking / rolling side to side in a moonlit bamboo glade. */
@Composable
internal fun PandaRollEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val fireflyCount = qualityCount(quality, low = 2, medium = 4, high = 7)
    val fireflies = remember(fireflyCount) {
        List(fireflyCount) { i ->
            PandaRollFirefly(
                x0 = seededUnit(i * 23 + 7),
                y0 = seededUnit(i * 31 + 11),
                driftX = seededRange(i * 41 + 13, 0.01f, 0.03f),
                driftY = seededRange(i * 47 + 17, 0.008f, 0.02f),
                phaseOffset = seededRange(i * 59 + 19, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "panda_roll_loop")
    val rollAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_roll_t",
    )
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((6200 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_roll_breath",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_roll_drift",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.55f
        val t = phase01(rollAnim)
        val dt = phase01(driftAnim)

        // Moonlit bamboo glade background
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF142018),
                0.4f to Color(0xFF1C2820),
                1f to Color(0xFF0C120E),
            ),
        )

        // Distant bamboo silhouettes
        val bambooSil = TonalPalette.brightness(Color(0xFF1A2A18), brightness * restingDim * 0.5f)
        for (i in 0 until 6) {
            val bx = w * (0.05f + i * 0.19f)
            val bw = minDim * 0.012f
            drawLine(
                color = bambooSil,
                start = Offset(bx, h * 0.95f),
                end = Offset(bx + sin(dt * 2f * PI.toFloat() + i) * minDim * 0.008f * (if (isActive) 1f else 0.3f), h * 0.02f),
                strokeWidth = bw,
            )
        }

        // Moonlight glow from upper left
        val moonGlow = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFD0D8E8), TonalPalette.pick(paletteColors, 1), 0.15f),
            brightness * restingDim,
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(moonGlow, 0.2f * restingDim),
                0.5f to TonalPalette.withAlpha(moonGlow, 0.06f),
                1f to Color.Transparent,
                center = Offset(w * 0.2f, h * 0.08f),
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.2f, h * 0.08f),
        )

        // Grass ground
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF2A3828), 0.4f * restingDim),
                1f to Color.Transparent,
                center = Offset(cx, h * 0.75f),
                radius = minDim * 0.4f,
            ),
            topLeft = Offset(cx - minDim * 0.4f, h * 0.65f),
            size = Size(minDim * 0.8f, minDim * 0.22f),
        )

        val rock = sin(t * 2f * PI.toFloat()) * (if (isActive) 18f else 6f)
        val breath = sin(phase01(breathAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.015f else 0.006f) * breath
        val white = TonalPalette.brightness(Color(0xFFE8E4DC), brightness * restingDim)
        val black = TonalPalette.brightness(Color(0xFF12100E), brightness * restingDim)
        val bodyR = minDim * 0.20f * scale

        rotate(degrees = rock, pivot = Offset(cx, cy + bodyR * 0.3f)) {
            drawCircle(color = white, radius = bodyR, center = Offset(cx, cy))
            // Belly shadow
            drawCircle(
                color = TonalPalette.withAlpha(black, 0.5f),
                radius = bodyR * 0.55f,
                center = Offset(cx, cy + bodyR * 0.15f),
            )
            val headR = bodyR * 0.55f
            val head = Offset(cx - bodyR * 0.15f, cy - bodyR * 0.75f)
            drawCircle(color = white, radius = headR, center = head)
            // Ears
            drawCircle(color = black, radius = headR * 0.32f, center = Offset(head.x - headR * 0.7f, head.y - headR * 0.7f))
            drawCircle(color = black, radius = headR * 0.32f, center = Offset(head.x + headR * 0.7f, head.y - headR * 0.7f))
            // Eye patches
            if (quality != GenartQuality.Low) {
                drawOval(
                    color = black,
                    topLeft = Offset(head.x - headR * 0.7f, head.y - headR * 0.2f),
                    size = Size(headR * 0.5f, headR * 0.4f),
                )
                drawOval(
                    color = black,
                    topLeft = Offset(head.x + headR * 0.2f, head.y - headR * 0.2f),
                    size = Size(headR * 0.5f, headR * 0.4f),
                )
                // Nose
                drawCircle(
                    color = black,
                    radius = headR * 0.07f,
                    center = Offset(head.x, head.y + headR * 0.15f),
                )
            }
            // Limb blobs suggesting a roll
            drawCircle(color = black, radius = bodyR * 0.22f, center = Offset(cx - bodyR * 0.75f, cy + bodyR * 0.35f))
            drawCircle(color = black, radius = bodyR * 0.22f, center = Offset(cx + bodyR * 0.8f, cy - bodyR * 0.15f))
            drawCircle(color = black, radius = bodyR * 0.2f, center = Offset(cx + bodyR * 0.35f, cy + bodyR * 0.75f))
            drawCircle(color = black, radius = bodyR * 0.2f, center = Offset(cx - bodyR * 0.4f, cy + bodyR * 0.72f))

            // Moonlit highlight on body
            if (quality == GenartQuality.High) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(moonGlow, 0.15f * restingDim),
                        1f to Color.Transparent,
                        center = Offset(cx - bodyR * 0.3f, cy - bodyR * 0.4f),
                        radius = bodyR * 0.5f,
                    ),
                    radius = bodyR * 0.5f,
                    center = Offset(cx - bodyR * 0.3f, cy - bodyR * 0.4f),
                )
            }
        }

        // Floating fireflies
        fireflies.forEach { ff ->
            val mx = phase01(ff.x0 + dt * ff.driftX) * w
            val my = phase01(ff.y0 + dt * ff.driftY) * h
            val alpha = (0.15f + 0.1f * sin(dt * 2f * PI.toFloat() + ff.phaseOffset)) * restingDim
            val r = minDim * 0.022f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFE0F0C0), alpha),
                    1f to Color.Transparent,
                    center = Offset(mx, my),
                    radius = r,
                ),
                radius = r,
                center = Offset(mx, my),
            )
        }
    }
}
