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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Velo Dawn Lane — a quiet road opening into a peach dawn. */
@Composable
internal fun VeloDawnLaneEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val glints = List(count) { i ->
        Triple(seededUnit(i * 17 + 3), seededUnit(i * 29 + 7), seededRange(i * 41 + 11, 0.0015f, 0.004f))
    }
    val transition = rememberInfiniteTransition(label = "velodawnlane")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "velodawnlane_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.4f, octaves = 3, seedOffset = 17)
        val horizonY = h * (0.48f + (drift - 0.5f) * 0.018f)
        val vanishingX = w * (0.5f + (loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 41) - 0.5f) * 0.025f)
        val skyTop = TonalPalette.brightness(Color(0xFF3A4858), brightness * dim)
        val dawn = TonalPalette.brightness(Color(0xFFE8C090), brightness * dim)
        val glow = TonalPalette.brightness(TonalPalette.mix(Color(0xFFFFE8C0), TonalPalette.pick(paletteColors, 0), 0.25f), brightness * dim)
        val foreground = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)

        drawRect(brush = Brush.verticalGradient(0f to skyTop, 0.72f to dawn, 1f to Color(0xFF9A7056)))

        // Pale sunrise bloom remains broad and diffuse at the road's vanishing point.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(glow, 0.30f * dim),
                0.34f to TonalPalette.withAlpha(dawn, 0.16f * dim),
                1f to Color.Transparent,
                center = Offset(vanishingX, horizonY + h * 0.025f),
                radius = (minDim * 0.48f),
            ),
            radius = minDim * 0.48f,
            center = Offset(vanishingX, horizonY + h * 0.025f),
        )

        // Low, quiet field silhouettes frame the lane without competing with the sky.
        val fieldY = horizonY + h * 0.11f
        val leftField = Path().apply {
            moveTo(0f, horizonY + h * 0.045f)
            cubicTo(w * 0.15f, horizonY + h * 0.09f, w * 0.27f, fieldY - h * 0.02f, vanishingX, fieldY)
            lineTo(vanishingX, h)
            lineTo(0f, h)
            close()
        }
        val rightField = Path().apply {
            moveTo(w, horizonY + h * 0.04f)
            cubicTo(w * 0.82f, horizonY + h * 0.1f, w * 0.68f, fieldY - h * 0.015f, vanishingX, fieldY)
            lineTo(vanishingX, h)
            lineTo(w, h)
            close()
        }
        val fieldBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF746653).copy(alpha = 0.62f * dim), foreground),
            startY = horizonY,
            endY = h,
        )
        drawPath(leftField, brush = fieldBrush)
        drawPath(rightField, brush = fieldBrush)

        // Road widens toward the rider, with softly feathered shoulders.
        val roadTop = horizonY + h * 0.035f
        val roadPath = Path().apply {
            moveTo(vanishingX - w * 0.012f, roadTop)
            lineTo(vanishingX + w * 0.012f, roadTop)
            lineTo(w * 0.83f, h)
            lineTo(w * 0.17f, h)
            close()
        }
        drawPath(
            roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF292A2D), TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)),
                startY = roadTop,
                endY = h,
            ),
        )

        // Edge lines converge and fade into the dawn haze.
        listOf(0.17f, 0.83f).forEach { edgeX ->
            drawLine(
                color = TonalPalette.withAlpha(glow, 0.20f * dim),
                start = Offset(vanishingX + (edgeX - 0.5f) * w * 0.035f, roadTop),
                end = Offset(w * edgeX, h),
                strokeWidth = minDim * 0.003f,
                cap = StrokeCap.Round,
            )
        }

        // Short center dashes move gently toward the viewer in a perfect loop.
        val dashPhase = (time * 0.22f) % 1f
        for (i in 0 until 8) {
            val depth = ((i / 8f + dashPhase) % 1f)
            val y = roadTop + (h - roadTop) * depth * depth
            val perspective = 0.012f + depth * 0.028f
            val dashH = h * (0.008f + depth * 0.022f)
            val alpha = (0.10f + depth * 0.28f) * dim
            drawRoundRect(
                color = TonalPalette.withAlpha(glow, alpha),
                topLeft = Offset(vanishingX - w * perspective * 0.5f, y),
                size = Size(w * perspective, dashH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(minDim * 0.004f),
            )
        }

        // Sparse field glints give the dawn atmosphere depth without adding traffic-like motion.
        glints.forEachIndexed { i, (x0, y0, radius) ->
            val n = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = i * 73 + 5)
            val x = x0 * w
            val y = horizonY + y0 * h * 0.30f + sin(time * 2f * PI.toFloat() + i) * h * 0.003f
            val r = minDim * radius * (0.75f + 0.5f * n)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(glow, (0.18f + 0.12f * n) * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = r * 4f,
                ),
                radius = r * 4f,
                center = Offset(x, y),
            )
        }

        // Dark foreground vignette grounds the first-person cycling view.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(foreground, 0.56f * dim),
                startY = h * 0.68f,
                endY = h,
            ),
        )
    }
}
