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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Cat silhouette on a windowsill watching soft dawn light through the pane. */
@Composable
internal fun CatWindowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cat_window_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5200 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_window_breath",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_window_drift",
    )
    val restingDim = if (isActive) 1f else 0.65f
    val detail = when (quality) {
        GenartQuality.Low -> 0
        GenartQuality.Medium -> 1
        GenartQuality.High -> 2
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val t = phase01(driftAnim)
        val haze = loopedFbm(t, radius = 1.4f, octaves = 2, seedOffset = 11)

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1A2238),
                0.45f to Color(0xFF2E3A55),
                1f to Color(0xFF12161F),
            ),
        )

        val paneLeft = w * 0.12f
        val paneTop = h * 0.08f
        val paneW = w * 0.76f
        val paneH = h * 0.62f
        val dawn = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD8A8), TonalPalette.pick(paletteColors, 1), 0.25f),
            brightness * restingDim,
        )
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(dawn, 0.55f + 0.08f * haze),
                0.55f to TonalPalette.withAlpha(Color(0xFF6B8AB8), 0.35f),
                1f to Color.Transparent,
                center = Offset(paneLeft + paneW * 0.55f, paneTop + paneH * 0.35f),
                radius = paneW * 0.7f,
            ),
            topLeft = Offset(paneLeft, paneTop),
            size = Size(paneW, paneH),
        )

        val frame = TonalPalette.brightness(Color(0xFF1C1410), brightness * restingDim)
        val frameStroke = minDim * 0.018f
        drawRect(color = frame, topLeft = Offset(paneLeft, paneTop), size = Size(paneW, frameStroke))
        drawRect(color = frame, topLeft = Offset(paneLeft, paneTop + paneH - frameStroke), size = Size(paneW, frameStroke))
        drawRect(color = frame, topLeft = Offset(paneLeft, paneTop), size = Size(frameStroke, paneH))
        drawRect(color = frame, topLeft = Offset(paneLeft + paneW - frameStroke, paneTop), size = Size(frameStroke, paneH))
        drawRect(
            color = frame,
            topLeft = Offset(paneLeft + paneW * 0.5f - frameStroke * 0.5f, paneTop),
            size = Size(frameStroke, paneH),
        )

        val sillY = paneTop + paneH
        drawRect(
            color = TonalPalette.brightness(Color(0xFF2A211C), brightness * restingDim),
            topLeft = Offset(paneLeft - minDim * 0.02f, sillY),
            size = Size(paneW + minDim * 0.04f, minDim * 0.06f),
        )

        val breathT = phase01(breathAnim)
        val breath = sin(breathT * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.025f else 0.01f) * breath
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF1A1412), TonalPalette.pick(paletteColors, 0), 0.2f),
            brightness * restingDim,
        )
        val cx = w * 0.58f
        val cy = sillY - minDim * 0.02f
        val bodyRx = minDim * 0.14f * scale
        val bodyRy = minDim * 0.10f * scale
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy * 1.6f),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        val headR = minDim * 0.075f * scale
        val head = Offset(cx - bodyRx * 0.15f, cy - bodyRy * 2.15f)
        drawCircle(color = bodyColor, radius = headR, center = head)
        val ear = Path().apply {
            moveTo(head.x - headR * 0.75f, head.y - headR * 0.2f)
            lineTo(head.x - headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x - headR * 0.15f, head.y - headR * 0.55f)
            close()
            moveTo(head.x + headR * 0.15f, head.y - headR * 0.55f)
            lineTo(head.x + headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x + headR * 0.75f, head.y - headR * 0.2f)
            close()
        }
        drawPath(ear, color = bodyColor)

        if (detail > 0) {
            val tail = Path().apply {
                val base = Offset(cx + bodyRx * 0.85f, cy - bodyRy * 0.9f)
                val tipX = base.x + minDim * 0.08f
                val tipY = base.y - minDim * (0.06f + 0.02f * sin(t * 2f * PI.toFloat()))
                moveTo(base.x, base.y)
                quadraticTo(base.x + minDim * 0.05f, base.y - minDim * 0.08f, tipX, tipY)
            }
            drawPath(
                path = tail,
                color = bodyColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = minDim * 0.022f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}
