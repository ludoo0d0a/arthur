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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class PandaBambooStalk(
    val x: Float,
    val width: Float,
    val swayPhase: Float,
    val swayAmp: Float,
)

private data class PandaBambooLeaf(
    val stalkIndex: Int,
    val nodeIndex: Int,
    val side: Float, // -1 or 1
    val length: Float,
    val angle: Float,
)

/** Panda silhouette among softly swaying bamboo stalks, with leaves and filtered light. */
@Composable
internal fun PandaBambooEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val stalkCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val stalks = remember(stalkCount) {
        List(stalkCount) { i ->
            PandaBambooStalk(
                x = seededUnit(i * 17 + 3),
                width = seededRange(i * 29 + 7, 0.012f, 0.022f),
                swayPhase = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat()),
                swayAmp = seededRange(i * 53 + 13, 0.008f, 0.02f),
            )
        }
    }
    val leafCount = qualityCount(quality, low = 0, medium = 8, high = 16)
    val leaves = remember(leafCount, stalkCount) {
        List(leafCount) { i ->
            PandaBambooLeaf(
                stalkIndex = i % stalkCount,
                nodeIndex = (i / stalkCount).coerceIn(0, 2) + 1,
                side = if (i % 2 == 0) -1f else 1f,
                length = seededRange(i * 67 + 19, 0.025f, 0.045f),
                angle = seededRange(i * 71 + 23, 0.3f, 0.9f),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "panda_bamboo_loop")
    val swayAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_bamboo_sway",
    )
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5400 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_bamboo_breath",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val t = phase01(swayAnim)

        // Deep bamboo forest background
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1A2C1E),
                0.4f to Color(0xFF243028),
                1f to Color(0xFF101812),
            ),
        )

        // Filtered canopy light from above
        val canopyLight = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF8AAE68), TonalPalette.pick(paletteColors, 1), 0.2f),
            brightness * restingDim,
        )
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(canopyLight, 0.18f * restingDim),
                0.5f to TonalPalette.withAlpha(canopyLight, 0.06f),
                1f to Color.Transparent,
                center = Offset(w * 0.45f, h * 0.08f),
                radius = w * 0.5f,
            ),
        )

        // Forest floor
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF2A3828), 0.5f * restingDim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, h * 0.82f),
                radius = minDim * 0.5f,
            ),
            topLeft = Offset(w * 0.5f - minDim * 0.5f, h * 0.72f),
            size = Size(minDim, minDim * 0.2f),
        )

        val bambooColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF4A6B3A), TonalPalette.pick(paletteColors, 2), 0.2f),
            brightness * restingDim,
        )
        val leafColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF3A5A2E), TonalPalette.pick(paletteColors, 2), 0.25f),
            brightness * restingDim,
        )

        // Bamboo stalks
        stalks.forEachIndexed { si, stalk ->
            val baseX = stalk.x * w
            val amp = stalk.swayAmp * minDim * (if (isActive) 1f else 0.4f)
            val tipX = baseX + sin(t * 2f * PI.toFloat() + stalk.swayPhase) * amp
            drawLine(
                color = bambooColor,
                start = Offset(baseX, h * 0.95f),
                end = Offset(tipX, h * 0.05f),
                strokeWidth = stalk.width * minDim,
                cap = StrokeCap.Round,
            )
            // Soft nodes
            for (n in 1..3) {
                val ny = h * (0.95f - n * 0.22f)
                val nx = baseX + (tipX - baseX) * (1f - ny / h)
                drawLine(
                    color = TonalPalette.withAlpha(bambooColor, 0.5f),
                    start = Offset(nx - stalk.width * minDim * 0.8f, ny),
                    end = Offset(nx + stalk.width * minDim * 0.8f, ny),
                    strokeWidth = minDim * 0.006f,
                    cap = StrokeCap.Round,
                )
            }

            // Leaves growing from nodes
            if (quality != GenartQuality.Low) {
                leaves.filter { it.stalkIndex == si }.forEach { leaf ->
                    val ny = h * (0.95f - leaf.nodeIndex * 0.22f)
                    val nx = baseX + (tipX - baseX) * (1f - ny / h)
                    val leafSway = sin(t * 2f * PI.toFloat() + stalk.swayPhase + leaf.angle) * minDim * 0.008f * (if (isActive) 1f else 0.3f)
                    drawLine(
                        color = leafColor,
                        start = Offset(nx, ny),
                        end = Offset(
                            nx + leaf.side * leaf.length * minDim + leafSway,
                            ny - leaf.length * minDim * 0.4f,
                        ),
                        strokeWidth = minDim * 0.006f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        val breath = sin(phase01(breathAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.02f else 0.008f) * breath
        val white = TonalPalette.brightness(Color(0xFFE6E2DA), brightness * restingDim)
        val black = TonalPalette.brightness(Color(0xFF12100E), brightness * restingDim)
        val cx = w * 0.55f
        val cy = h * 0.62f
        val bodyRx = minDim * 0.16f * scale
        val bodyRy = minDim * 0.14f * scale
        drawOval(
            color = white,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        // Belly patch
        drawOval(
            color = TonalPalette.withAlpha(black, 0.45f),
            topLeft = Offset(cx - bodyRx * 0.5f, cy + bodyRy * 0.1f),
            size = Size(bodyRx, bodyRy * 0.9f),
        )
        val headR = minDim * 0.09f * scale
        val head = Offset(cx - bodyRx * 0.15f, cy - bodyRy * 1.15f)
        drawCircle(color = white, radius = headR, center = head)
        drawCircle(color = black, radius = headR * 0.35f, center = Offset(head.x - headR * 0.7f, head.y - headR * 0.7f))
        drawCircle(color = black, radius = headR * 0.35f, center = Offset(head.x + headR * 0.7f, head.y - headR * 0.7f))
        // Eye patches
        if (quality != GenartQuality.Low) {
            drawOval(
                color = black,
                topLeft = Offset(head.x - headR * 0.65f, head.y - headR * 0.2f),
                size = Size(headR * 0.45f, headR * 0.35f),
            )
            drawOval(
                color = black,
                topLeft = Offset(head.x + headR * 0.2f, head.y - headR * 0.2f),
                size = Size(headR * 0.45f, headR * 0.35f),
            )
        }
        // Paws
        drawCircle(color = black, radius = minDim * 0.04f * scale, center = Offset(cx - bodyRx * 0.9f, cy))
        drawCircle(color = black, radius = minDim * 0.04f * scale, center = Offset(cx + bodyRx * 0.85f, cy + bodyRy * 0.1f))
        // Feet
        drawCircle(color = black, radius = minDim * 0.038f * scale, center = Offset(cx - bodyRx * 0.3f, cy + bodyRy * 1.3f))
        drawCircle(color = black, radius = minDim * 0.038f * scale, center = Offset(cx + bodyRx * 0.3f, cy + bodyRy * 1.3f))

        // Bamboo stick held by the panda
        drawLine(
            color = bambooColor,
            start = Offset(cx + bodyRx * 0.3f, cy - bodyRy * 0.2f),
            end = Offset(cx + bodyRx * 0.5f, cy - bodyRy * 1.8f),
            strokeWidth = minDim * 0.012f,
            cap = StrokeCap.Round,
        )
        // Leaf clusters on the held bamboo
        if (quality != GenartQuality.Low) {
            val heldTipX = cx + bodyRx * 0.5f
            val heldTipY = cy - bodyRy * 1.8f
            for (i in 0..2) {
                val lSway = sin(t * 2f * PI.toFloat() + i) * minDim * 0.005f * (if (isActive) 1f else 0.3f)
                drawLine(
                    color = leafColor,
                    start = Offset(heldTipX, heldTipY + minDim * 0.02f * i),
                    end = Offset(heldTipX + minDim * 0.03f * (if (i % 2 == 0) 1f else -1f) + lSway, heldTipY - minDim * 0.015f + minDim * 0.015f * i),
                    strokeWidth = minDim * 0.005f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Dappled light spots on the ground
        if (quality == GenartQuality.High) {
            for (i in 0 until 4) {
                val spotX = w * (0.25f + 0.15f * i + 0.02f * sin(t * 2f * PI.toFloat() + i))
                val spotY = h * (0.78f + 0.03f * i)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(canopyLight, 0.1f * restingDim),
                        1f to Color.Transparent,
                        center = Offset(spotX, spotY),
                        radius = minDim * 0.04f,
                    ),
                    radius = minDim * 0.04f,
                    center = Offset(spotX, spotY),
                )
            }
        }
    }
}
