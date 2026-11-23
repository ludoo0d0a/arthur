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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV US Route 66 Desert Wash — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun UsRoute66WashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "usroute66wash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "usroute66wash_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 23) - 0.5f
        val dawn = TonalPalette.brightness(Color(0xFFFFD080), brightness * dim)
        val terra = TonalPalette.brightness(Color(0xFFC89050), brightness * dim)
        val mesa = TonalPalette.brightness(Color(0xFF6A4020), brightness * dim)
        val night = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val horizonY = h * (0.52f + 0.012f * drift)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.mix(Color(0xFF342B32), terra, 0.32f + 0.06f * wash),
                0.5f to terra,
                1f to mesa,
            ),
        )
        // Diffuse sunset glow, broad enough to feel like a wash rather than a lamp.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to TonalPalette.withAlpha(dawn, 0.12f * dim),
                1f to Color.Transparent,
                startY = horizonY - h * 0.22f,
                endY = horizonY + h * 0.18f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(dawn, (0.28f + 0.06f * wash) * dim),
                0.45f to TonalPalette.withAlpha(terra, 0.12f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.51f + drift * 0.08f), horizonY - h * 0.07f),
                radius = m * 0.48f,
            ),
            radius = m * 0.48f,
            center = Offset(w * (0.51f + drift * 0.08f), horizonY - h * 0.07f),
        )

        // Distant mesas and low desert ridges.
        val farMesa = Path().apply {
            moveTo(0f, horizonY + h * 0.04f)
            lineTo(0f, horizonY - h * 0.05f)
            lineTo(w * 0.10f, horizonY - h * 0.06f)
            lineTo(w * 0.17f, horizonY - h * 0.12f)
            lineTo(w * 0.26f, horizonY - h * 0.12f)
            lineTo(w * 0.34f, horizonY - h * 0.04f)
            lineTo(w * 0.47f, horizonY - h * 0.08f)
            lineTo(w * 0.57f, horizonY - h * 0.14f)
            lineTo(w * 0.68f, horizonY - h * 0.13f)
            lineTo(w * 0.78f, horizonY - h * 0.04f)
            lineTo(w * 0.88f, horizonY - h * 0.10f)
            lineTo(w, horizonY - h * 0.07f)
            lineTo(w, horizonY + h * 0.05f)
            close()
        }
        drawPath(farMesa, color = TonalPalette.withAlpha(Color(0xFF815234), 0.72f * dim))
        val nearMesa = Path().apply {
            moveTo(0f, horizonY + h * 0.17f)
            lineTo(0f, horizonY + h * 0.02f)
            lineTo(w * 0.14f, horizonY + h * 0.06f)
            lineTo(w * 0.25f, horizonY - h * 0.00f)
            lineTo(w * 0.36f, horizonY + h * 0.08f)
            lineTo(w * 0.52f, horizonY + h * 0.03f)
            lineTo(w * 0.64f, horizonY + h * 0.10f)
            lineTo(w * 0.79f, horizonY + h * 0.01f)
            lineTo(w, horizonY + h * 0.04f)
            lineTo(w, horizonY + h * 0.19f)
            close()
        }
        drawPath(nearMesa, color = TonalPalette.withAlpha(mesa, 0.92f * dim))

        // Desert floor wash beneath the ridgelines.
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(terra, 0.18f * dim),
                1f to mesa,
                startY = horizonY + h * 0.10f,
                endY = h,
            ),
            topLeft = Offset(0f, horizonY + h * 0.10f),
            size = Size(w, h * 0.9f),
        )
        // Road edges converge at a warm vanishing point.
        val road = Path().apply {
            moveTo(w * 0.47f, horizonY + h * 0.05f)
            lineTo(w * 0.53f, horizonY + h * 0.05f)
            lineTo(w * 0.94f, h)
            lineTo(w * 0.06f, h)
            close()
        }
        drawPath(
            road,
            brush = Brush.verticalGradient(
                0f to Color(0xFF403934),
                1f to night,
                startY = horizonY,
                endY = h,
            ),
        )
        val roadEdge = TonalPalette.withAlpha(dawn, 0.40f * dim)
        drawLine(roadEdge, Offset(w * 0.47f, horizonY + h * 0.05f), Offset(w * 0.06f, h), m * 0.004f)
        drawLine(roadEdge, Offset(w * 0.53f, horizonY + h * 0.05f), Offset(w * 0.94f, h), m * 0.004f)

        // Perspective center dashes phase along the same seamless loop.
        for (i in 0..5) {
            val depth = ((i / 6f + time * 0.16f) % 1f)
            val y = horizonY + h * 0.07f + (depth * depth) * (h - horizonY)
            val halfWidth = m * (0.002f + 0.009f * depth)
            val dashLength = h * (0.012f + 0.05f * depth)
            drawLine(
                color = TonalPalette.withAlpha(dawn, (0.48f + 0.18f * depth) * dim),
                start = Offset(w * 0.5f - halfWidth, y),
                end = Offset(w * 0.5f + halfWidth, y),
                strokeWidth = dashLength,
            )
        }

        // Small, understated roadside shield sign: graphic badge rather than legible text.
        val signCenter = Offset(w * 0.84f, horizonY - h * 0.015f + drift * m * 0.01f)
        val signW = m * 0.075f
        val signH = m * 0.09f
        val sign = Path().apply {
            moveTo(signCenter.x - signW * 0.5f, signCenter.y - signH * 0.5f)
            lineTo(signCenter.x + signW * 0.5f, signCenter.y - signH * 0.5f)
            lineTo(signCenter.x + signW * 0.45f, signCenter.y + signH * 0.12f)
            lineTo(signCenter.x, signCenter.y + signH * 0.58f)
            lineTo(signCenter.x - signW * 0.45f, signCenter.y + signH * 0.12f)
            close()
        }
        drawPath(sign, color = TonalPalette.withAlpha(Color(0xFF201A18), 0.82f * dim))
        drawPath(sign, color = TonalPalette.withAlpha(dawn, 0.52f * dim), style = Stroke(width = m * 0.002f))
        drawLine(
            color = TonalPalette.withAlpha(dawn, 0.68f * dim),
            start = Offset(signCenter.x - signW * 0.22f, signCenter.y),
            end = Offset(signCenter.x + signW * 0.22f, signCenter.y),
            strokeWidth = m * 0.003f,
        )

        // A handful of low desert shimmer points near the horizon.
        repeat(detailCount) { i ->
            val x = ((i + 1f) / (detailCount + 1f) + 0.015f * sin(time * 2f * PI.toFloat() + i)) * w
            val y = horizonY + h * (0.08f + (i % 3) * 0.018f)
            val alpha = (0.07f + 0.07f * loopedFbm(time, radius = 1.2f, seedOffset = 91 + i)) * dim
            drawCircle(TonalPalette.withAlpha(dawn, alpha), m * 0.002f, Offset(x, y))
        }
    }
}
