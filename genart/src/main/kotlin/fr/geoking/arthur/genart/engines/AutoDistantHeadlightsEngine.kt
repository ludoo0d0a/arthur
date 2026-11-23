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

/** Soft Ambient POV Auto Distant Headlights — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun AutoDistantHeadlightsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val transition = rememberInfiniteTransition(label = "autodistantheadlights")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autodistantheadlights_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val haze = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 37)
        val sway = sin(time * 2f * PI.toFloat())
        val horizon = h * (0.47f + 0.018f * (haze - 0.5f))
        val cx = w * (0.5f + 0.018f * sway)
        val roadAccent = TonalPalette.brightness(Color(0xFFFFE8A0), brightness * dim)
        val headlight = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFF2C2), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF080A10), brightness * dim),
                horizon / h to TonalPalette.brightness(Color(0xFF12161E), brightness * dim),
                1f to Color(0xFF080A0C),
            ),
        )

        // Low fog behind the road softens the distant headlights into the night.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.58f to TonalPalette.withAlpha(Color(0xFFB4C1D0), (0.035f + 0.025f * haze) * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.18f,
                endY = horizon + h * 0.16f,
            ),
        )

        // Road sides converge at the vanishing point and bend gently as the viewpoint drifts.
        val road = Path().apply {
            moveTo(cx - minDim * 0.055f, horizon)
            cubicTo(cx - minDim * 0.13f, h * 0.70f, w * 0.16f, h * 0.82f, w * 0.08f, h)
            lineTo(w * 0.92f, h)
            cubicTo(w * 0.84f, h * 0.82f, cx + minDim * 0.13f, h * 0.70f, cx + minDim * 0.055f, horizon)
            close()
        }
        drawPath(
            path = road,
            brush = Brush.verticalGradient(
                0f to Color(0xFF252932),
                0.38f to Color(0xFF171B22),
                1f to Color(0xFF0B0D12),
                startY = horizon,
                endY = h,
            ),
        )

        // A subtle center seam and soft edge traces give the road depth without hard glare.
        val centerLine = Path().apply {
            moveTo(cx, horizon + h * 0.025f)
            cubicTo(cx - minDim * 0.018f, h * 0.64f, cx + minDim * 0.035f, h * 0.82f, w * (0.5f + 0.035f * sway), h)
        }
        drawPath(centerLine, TonalPalette.withAlpha(roadAccent, 0.10f * dim), style = Stroke(width = minDim * 0.006f))
        val leftEdge = Path().apply {
            moveTo(cx - minDim * 0.055f, horizon)
            cubicTo(cx - minDim * 0.13f, h * 0.70f, w * 0.16f, h * 0.82f, w * 0.08f, h)
        }
        val rightEdge = Path().apply {
            moveTo(cx + minDim * 0.055f, horizon)
            cubicTo(cx + minDim * 0.13f, h * 0.70f, w * 0.84f, h * 0.82f, w * 0.92f, h)
        }
        drawPath(leftEdge, Color(0xFF31353A).copy(alpha = 0.34f * dim), style = Stroke(width = minDim * 0.004f))
        drawPath(rightEdge, Color(0xFF31353A).copy(alpha = 0.34f * dim), style = Stroke(width = minDim * 0.004f))

        // Reflected light lies low on the asphalt and tapers toward the far lights.
        val reflection = Path().apply {
            moveTo(cx - minDim * 0.012f, horizon + h * 0.018f)
            cubicTo(cx - minDim * 0.035f, h * 0.65f, w * 0.38f, h * 0.84f, w * 0.33f, h)
            lineTo(w * 0.67f, h)
            cubicTo(w * 0.62f, h * 0.84f, cx + minDim * 0.035f, h * 0.65f, cx + minDim * 0.012f, horizon + h * 0.018f)
            close()
        }
        drawPath(
            path = reflection,
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.24f to TonalPalette.withAlpha(roadAccent, (0.12f + 0.06f * haze) * dim),
                1f to Color.Transparent,
                startY = horizon,
                endY = h,
            ),
        )

        // A distant pair of warm lights, held in place while haze and their bloom breathe.
        val lightY = horizon + h * 0.006f
        val separation = minDim * (0.022f + 0.004f * haze)
        val glowRadius = minDim * (0.13f + 0.012f * haze)
        for (side in -1..1 step 2) {
            val lightCenter = Offset(cx + side * separation, lightY)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(headlight, 0.62f * dim),
                    0.16f to TonalPalette.withAlpha(roadAccent, 0.30f * dim),
                    1f to Color.Transparent,
                    center = lightCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = lightCenter,
            )
            drawCircle(color = TonalPalette.withAlpha(headlight, 0.92f * dim), radius = minDim * 0.0055f, center = lightCenter)
        }
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(roadAccent, 0.16f * dim),
                1f to Color.Transparent,
                center = Offset(cx, lightY),
                radius = minDim * 0.22f,
            ),
            radius = minDim * 0.22f,
            center = Offset(cx, lightY),
        )

        // Sparse roadside reflectors recede toward the same point.
        repeat(detailCount) { index ->
            val f = (index + 1) / (detailCount + 1f)
            val y = horizon + (h - horizon) * f * 0.88f
            val halfRoad = minDim * (0.055f + 0.39f * f)
            val reflectorR = minDim * (0.003f + 0.0035f * f)
            val alpha = (0.18f + 0.10f * loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 100 + index)) * dim
            drawCircle(Color(0xFFC4A978).copy(alpha = alpha), reflectorR, Offset(cx - halfRoad, y))
            drawCircle(Color(0xFFC4A978).copy(alpha = alpha), reflectorR, Offset(cx + halfRoad, y))
        }

        // Soft dashboard silhouette anchors the point-of-view composition.
        val dash = Path().apply {
            moveTo(0f, h * 0.94f)
            cubicTo(w * 0.24f, h * 0.89f, w * 0.32f, h * 0.92f, cx, h * 0.93f)
            cubicTo(w * 0.68f, h * 0.92f, w * 0.76f, h * 0.89f, w, h * 0.94f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(dash, Color(0xFF050609).copy(alpha = 0.72f))
    }
}
