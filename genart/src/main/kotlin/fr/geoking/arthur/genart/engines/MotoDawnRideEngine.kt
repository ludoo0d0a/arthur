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
import androidx.compose.ui.geometry.CornerRadius
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

/** Soft Ambient POV Moto Dawn Ride — a quiet road flowing toward first light. */
@Composable
internal fun MotoDawnRideEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val dashCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "motodawnride")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "motodawnride_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val cloud = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 31)
        val horizon = h * (0.48f + 0.018f * (cloud - 0.5f))
        val cx = w * 0.5f
        val dawn = TonalPalette.brightness(Color(0xFFFFB080), brightness * dim)
        val skyTop = TonalPalette.brightness(Color(0xFF2A2030), brightness * dim)
        val ridge = TonalPalette.brightness(Color(0xFF4A3848), brightness * dim)
        val road = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val shoulder = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, ridge), endY = horizon * 1.2f))
        // Diffuse peach dawn glow behind the distant ridges.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(dawn, 0.52f * dim),
                0.38f to TonalPalette.withAlpha(dawn, 0.22f * dim),
                1f to Color.Transparent,
                center = Offset(cx, horizon * 0.94f),
                radius = minDim * 0.60f,
            ),
            radius = minDim * 0.60f,
            center = Offset(cx, horizon * 0.94f),
        )

        // Low, quiet hills framing the horizon.
        val farHills = Path().apply {
            moveTo(0f, horizon * 0.93f)
            cubicTo(w * 0.18f, horizon * 0.82f, w * 0.27f, horizon * 1.04f, w * 0.45f, horizon * 0.91f)
            cubicTo(w * 0.64f, horizon * 0.78f, w * 0.78f, horizon * 1.05f, w, horizon * 0.88f)
            lineTo(w, horizon * 1.13f)
            lineTo(0f, horizon * 1.13f)
            close()
        }
        drawPath(path = farHills, color = TonalPalette.withAlpha(ridge, 0.68f * dim))
        val nearHills = Path().apply {
            moveTo(0f, horizon * 1.04f)
            cubicTo(w * 0.20f, horizon * 0.96f, w * 0.34f, horizon * 1.15f, w * 0.52f, horizon * 1.02f)
            cubicTo(w * 0.70f, horizon * 0.91f, w * 0.84f, horizon * 1.12f, w, horizon * 0.99f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = nearHills, color = TonalPalette.withAlpha(shoulder, 0.84f * dim))

        // Road surface narrows to a soft vanishing point.
        val roadPath = Path().apply {
            moveTo(cx - w * 0.018f, horizon * 1.01f)
            lineTo(cx + w * 0.018f, horizon * 1.01f)
            lineTo(w * 0.98f, h)
            lineTo(w * 0.02f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(road, 0.82f * dim),
                0.30f to road,
                1f to TonalPalette.mix(road, shoulder, 0.36f),
                startY = horizon,
                endY = h,
            ),
        )

        // Faint warm road-edge reflections lead the eye toward dawn.
        val edgeGlow = TonalPalette.withAlpha(dawn, 0.16f * dim)
        drawLine(edgeGlow, Offset(cx - w * 0.018f, horizon), Offset(w * 0.035f, h), minDim * 0.006f)
        drawLine(edgeGlow, Offset(cx + w * 0.018f, horizon), Offset(w * 0.965f, h), minDim * 0.006f)

        // Center dashes expand as they approach, creating a slow POV forward drift.
        for (i in 0 until dashCount) {
            val depth = ((i.toFloat() / dashCount + time * 0.46f) % 1f)
            val perspective = depth * depth
            val y = horizon + (h - horizon) * (0.035f + perspective * 0.94f)
            val scale = 0.25f + depth * depth * 3.2f
            val dashWidth = minDim * 0.006f * scale
            val dashHeight = minDim * 0.026f * scale
            drawRoundRect(
                color = TonalPalette.withAlpha(dawn, (0.17f + 0.31f * depth) * dim),
                topLeft = Offset(cx - dashWidth * 0.5f, y),
                size = Size(dashWidth, dashHeight),
                cornerRadius = CornerRadius(dashWidth * 0.45f),
            )
        }

        // Dark, understated fairing at the bottom edge.
        val fairing = Path().apply {
            moveTo(0f, h * 0.91f)
            cubicTo(w * 0.18f, h * 0.88f, w * 0.28f, h * 0.95f, cx, h * 0.92f)
            cubicTo(w * 0.72f, h * 0.95f, w * 0.84f, h * 0.88f, w, h * 0.91f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = fairing, color = TonalPalette.withAlpha(Color(0xFF080A0C), 0.82f * dim))
        drawPath(
            path = fairing,
            color = TonalPalette.withAlpha(dawn, 0.13f * dim),
            style = Stroke(width = minDim * 0.004f),
        )
    }
}
