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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Quiet station platform with warm lamps receding into the night. */
@Composable
internal fun TrainPlatformGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lampCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "trainplatformglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trainplatformglow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val atmosphere = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val pulse = 0.94f + 0.06f * loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 37)
        val warm = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFE0A0), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )
        val skyTop = TonalPalette.brightness(Color(0xFF101418), brightness * dim)
        val skyBottom = TonalPalette.brightness(Color(0xFF1A2028), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom)))

        val horizon = h * 0.48f
        // Distant station silhouette fades into the low night haze.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.54f * dim),
                startY = horizon - h * 0.11f,
                endY = horizon + h * 0.08f,
            ),
            topLeft = Offset(0f, horizon - h * 0.11f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.19f),
        )

        // Platform floor narrows toward a single quiet vanishing point.
        val leftFar = Offset(w * 0.39f, horizon)
        val rightFar = Offset(w * 0.61f, horizon)
        val leftNear = Offset(-w * 0.14f, h)
        val rightNear = Offset(w * 1.14f, h)
        val floor = Path().apply {
            moveTo(leftFar.x, leftFar.y)
            lineTo(rightFar.x, rightFar.y)
            lineTo(rightNear.x, rightNear.y)
            lineTo(leftNear.x, leftNear.y)
            close()
        }
        drawPath(
            floor,
            brush = Brush.verticalGradient(
                0f to Color(0xFF1B2229),
                0.35f to Color(0xFF12171C),
                1f to Color(0xFF080A0C),
                startY = horizon,
                endY = h,
            ),
        )

        // Track-side shadow and the platform's pale tactile safety strip.
        val edgePath = Path().apply {
            moveTo(w * 0.54f, horizon + h * 0.03f)
            lineTo(w * 1.04f, h)
        }
        drawPath(edgePath, color = Color(0xFF050709), style = Stroke(width = minDim * 0.035f))
        drawPath(
            edgePath,
            color = TonalPalette.withAlpha(Color(0xFFE6C77E), 0.34f * dim),
            style = Stroke(width = minDim * 0.009f),
        )

        // Parallel station roof beams and lamps recede in perspective.
        for (i in 0 until lampCount) {
            val depth = (i + 1f) / (lampCount + 1f)
            val y = horizon - h * (0.02f + 0.35f * (1f - depth))
            val perspective = 0.18f + 0.82f * depth * depth
            val x = w * (0.24f + 0.18f * (1f - depth))
            val lampW = minDim * 0.035f * perspective
            val lampH = minDim * 0.012f * perspective
            val radius = minDim * (0.18f + 0.28f * depth)
            val breathe = 0.93f + 0.07f * sin(time * 2f * PI.toFloat() + i * 0.8f)

            // Soft light pools spill across the station floor.
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(warm, 0.18f * pulse * breathe * dim),
                    0.58f to TonalPalette.withAlpha(warm, 0.055f * dim),
                    1f to Color.Transparent,
                    center = Offset(x + w * 0.23f * depth, y + h * 0.28f * perspective),
                    radius = radius,
                ),
                topLeft = Offset(x + w * 0.23f * depth - radius, y + h * 0.28f * perspective - radius * 0.38f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.76f),
            )

            // Lamp body and a narrow, diffused core; no hard point light.
            drawRoundRect(
                color = TonalPalette.withAlpha(Color(0xFF30343A), 0.78f * dim),
                topLeft = Offset(x - lampW * 0.7f, y),
                size = androidx.compose.ui.geometry.Size(lampW * 1.4f, lampH * 2.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(lampH),
            )
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFFFF2D4), 0.82f * pulse * dim),
                    1f to TonalPalette.withAlpha(warm, 0.05f * dim),
                    center = Offset(x, y + lampH),
                    radius = lampW * 2.8f,
                ),
                topLeft = Offset(x - lampW * 2.8f, y + lampH - lampW * 1.6f),
                size = androidx.compose.ui.geometry.Size(lampW * 5.6f, lampW * 3.2f),
            )
        }

        // Subtle roof edge and supporting posts frame the receding platform.
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF07090C), 0.68f * dim),
            start = Offset(w * 0.08f, horizon - h * 0.23f),
            end = Offset(w * 0.48f, horizon - h * 0.04f),
            strokeWidth = minDim * 0.018f,
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF07090C), 0.68f * dim),
            start = Offset(w * 0.08f, horizon - h * 0.23f),
            end = Offset(w * 0.08f, h * 0.77f),
            strokeWidth = minDim * 0.014f,
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF07090C), 0.58f * dim),
            start = Offset(w * 0.45f, horizon - h * 0.035f),
            end = Offset(w * 0.45f, h * 0.56f),
            strokeWidth = minDim * 0.008f,
        )

        // A low veil of haze softens the far end of the platform.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(warm, 0.06f * atmosphere * dim),
                startY = horizon - h * 0.05f,
                endY = horizon + h * 0.19f,
            ),
        )
    }
}
