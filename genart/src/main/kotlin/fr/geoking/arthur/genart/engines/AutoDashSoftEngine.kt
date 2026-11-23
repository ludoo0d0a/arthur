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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Auto Dash Soft — slow seamless night-drive loop. */
@Composable
internal fun AutoDashSoftEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "autodashsoft")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autodashsoft_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val atmosphere = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF80C0E0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )
        val amber = TonalPalette.brightness(Color(0xFFFFC98A), brightness * dim)
        val horizonY = h * (0.43f + 0.018f * (atmosphere - 0.5f))
        val vanishingX = w * 0.5f

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF101418), brightness * dim),
                0.48f to TonalPalette.brightness(Color(0xFF1C242C), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF080A0C), brightness * dim),
            ),
        )

        // Distant, diffuse lights establish a calm vanishing point.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.25f * dim),
                0.35f to TonalPalette.withAlpha(accent, 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(vanishingX, horizonY),
                radius = minDim * 0.34f,
            ),
            radius = minDim * 0.34f,
            center = Offset(vanishingX, horizonY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(amber, 0.26f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.77f, horizonY + h * 0.03f),
                radius = minDim * 0.13f,
            ),
            radius = minDim * 0.13f,
            center = Offset(w * 0.77f, horizonY + h * 0.03f),
        )

        // Road widens toward the viewer with only soft shoulders and no hard traffic geometry.
        val road = Path().apply {
            moveTo(vanishingX - w * 0.018f, horizonY)
            lineTo(vanishingX + w * 0.018f, horizonY)
            lineTo(w * 0.96f, h)
            lineTo(w * 0.04f, h)
            close()
        }
        drawPath(
            road,
            brush = Brush.verticalGradient(
                horizonY to Color(0xFF172027),
                h to Color(0xFF0B0E12),
                startY = horizonY,
                endY = h,
            ),
        )

        // Subdued shoulder lines gently converge toward the horizon.
        val shoulderPaint = TonalPalette.withAlpha(accent, 0.12f * dim)
        drawLine(
            color = shoulderPaint,
            start = Offset(vanishingX - w * 0.02f, horizonY),
            end = Offset(w * 0.04f, h),
            strokeWidth = minDim * 0.002f,
        )
        drawLine(
            color = shoulderPaint,
            start = Offset(vanishingX + w * 0.02f, horizonY),
            end = Offset(w * 0.96f, h),
            strokeWidth = minDim * 0.002f,
        )

        // Center dashes scroll in a loop; perspective expands their spacing and width near the viewer.
        for (i in 0 until count) {
            val phase = (time + i.toFloat() / count) % 1f
            val depth = phase * phase
            val y = horizonY + (h - horizonY) * depth
            val perspective = (0.18f + depth * 1.55f)
            val dashH = minDim * (0.005f + depth * 0.027f)
            val dashW = minDim * (0.003f + depth * 0.008f)
            val shimmer = 0.82f + 0.18f * sin((time + i.toFloat() / count) * 2f * PI.toFloat())
            drawRoundRect(
                color = TonalPalette.withAlpha(accent, (0.24f + 0.28f * depth) * shimmer * dim),
                topLeft = Offset(vanishingX - dashW * perspective * 0.5f, y - dashH * perspective * 0.5f),
                size = Size(dashW * perspective, dashH * perspective),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(dashW * perspective),
            )
        }

        // A broad dashboard silhouette anchors the foreground without drawing literal controls.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.25f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.35f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.92f * dim),
                startY = h * 0.78f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f),
        )

        // Faint curved instrument rim, kept low contrast.
        drawArc(
            color = TonalPalette.withAlpha(amber, 0.10f * dim),
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.83f),
            size = Size(w * 0.52f, h * 0.3f),
            style = Stroke(width = minDim * 0.004f, cap = StrokeCap.Round),
        )
    }
}
