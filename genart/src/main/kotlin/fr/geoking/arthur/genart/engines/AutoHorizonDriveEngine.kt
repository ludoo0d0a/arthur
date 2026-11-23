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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount

/** Soft Ambient POV Auto Horizon Drive — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun AutoHorizonDriveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val dashCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val transition = rememberInfiniteTransition(label = "autohorizondrive")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autohorizondrive_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val drift = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 37)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF1A2838), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF0E1620), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8C090), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        val horizon = h * (0.42f + 0.018f * (wash - 0.5f))
        // Distant layered hills frame a quiet, warm horizon.
        val farHill = Path().apply {
            moveTo(0f, horizon + h * 0.015f)
            cubicTo(w * 0.18f, horizon - h * 0.14f, w * 0.28f, horizon - h * 0.09f, w * 0.48f, horizon)
            cubicTo(w * 0.70f, horizon + h * 0.06f, w * 0.82f, horizon - h * 0.13f, w, horizon - h * 0.035f)
            lineTo(w, h * 0.66f)
            lineTo(0f, h * 0.66f)
            close()
        }
        drawPath(farHill, TonalPalette.withAlpha(Color(0xFF111B27), 0.92f * dim))
        val nearHill = Path().apply {
            moveTo(0f, horizon + h * 0.13f)
            cubicTo(w * 0.16f, horizon + h * 0.04f, w * 0.26f, horizon + h * 0.20f, w * 0.47f, horizon + h * 0.12f)
            cubicTo(w * 0.68f, horizon + h * 0.03f, w * 0.86f, horizon + h * 0.22f, w, horizon + h * 0.10f)
            lineTo(w, h * 0.72f)
            lineTo(0f, h * 0.72f)
            close()
        }
        drawPath(nearHill, TonalPalette.withAlpha(Color(0xFF0B121B), 0.94f * dim))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(accent, 0.15f * dim),
                startY = horizon - h * 0.08f,
                endY = horizon + h * 0.08f,
            ),
        )
        // Dark road narrowing toward the horizon, with soft shoulder reflections.
        val road = Path().apply {
            moveTo(w * 0.5f - w * 0.022f, horizon + h * 0.06f)
            lineTo(w * 0.5f + w * 0.022f, horizon + h * 0.06f)
            lineTo(w * 0.94f, h)
            lineTo(w * 0.06f, h)
            close()
        }
        drawPath(road, TonalPalette.withAlpha(Color(0xFF080A0C), 0.82f * dim))
        drawLine(TonalPalette.withAlpha(accent, 0.18f * dim), Offset(w * 0.5f - w * 0.022f, horizon + h * 0.06f), Offset(w * 0.10f, h), strokeWidth = w * 0.006f)
        drawLine(TonalPalette.withAlpha(accent, 0.18f * dim), Offset(w * 0.5f + w * 0.022f, horizon + h * 0.06f), Offset(w * 0.90f, h), strokeWidth = w * 0.006f)
        // Sparse center dashes stream gently toward the viewer and wrap seamlessly.
        repeat(dashCount) { i ->
            val base = i / dashCount.toFloat()
            val p = (base + time) % 1f
            val perspective = p * p
            val y = horizon + h * 0.07f + perspective * (h * 0.72f)
            val dashHalfWidth = w * (0.002f + perspective * 0.012f)
            val dashHeight = h * (0.008f + perspective * 0.038f)
            val xShift = (drift - 0.5f) * w * 0.018f * perspective
            drawRoundRect(
                color = TonalPalette.withAlpha(Color(0xFFE8C090), (0.18f + 0.34f * perspective) * dim),
                topLeft = Offset(w * 0.5f + xShift - dashHalfWidth, y),
                size = Size(dashHalfWidth * 2f, dashHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(dashHalfWidth),
            )
        }
        // Soft foreground silhouette anchors the POV without a hard dashboard edge.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.58f * dim),
                startY = h * 0.78f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f),
        )
    }
}
