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

/** Soft Ambient POV Auto Tunnel Light Rhythm — a seamless, slow forward glide. */
@Composable
internal fun AutoTunnelLightEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bandCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "autotunnellight")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autotunnellight_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val drift = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 37)
        val cx = w * (0.5f + (drift - 0.5f) * 0.012f)
        val horizonY = h * (0.34f + wash * 0.025f)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD080), TonalPalette.pick(paletteColors, 0), 0.24f),
            brightness * dim,
        )
        val cool = TonalPalette.brightness(Color(0xFF849BB8), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF0A0A0E), brightness * dim),
                0.55f to TonalPalette.brightness(Color(0xFF16161C), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF080A0C), brightness * dim),
            ),
        )

        // Deep, soft glow at the tunnel mouth and a dim center lane.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.30f * dim),
                0.38f to TonalPalette.withAlpha(cool, 0.12f * dim),
                1f to Color.Transparent,
                center = Offset(cx, horizonY),
                radius = (minDim * 0.48f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.48f).coerceAtLeast(1f),
            center = Offset(cx, horizonY),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.48f to TonalPalette.withAlpha(cool, 0.06f * dim),
                1f to Color.Transparent,
                startY = horizonY,
                endY = h,
            ),
            topLeft = Offset(cx - w * 0.12f, horizonY),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h - horizonY),
        )

        // Softly lit tunnel ribs. Even spacing makes the phase reset seamless.
        for (i in 0 until bandCount) {
            val depth = ((i.toFloat() / bandCount + time) % 1f)
            val perspective = depth * depth
            val yTop = horizonY + h * (0.025f + perspective * 0.48f)
            val yBottom = horizonY + h * (0.06f + perspective * 0.70f)
            val halfWidth = w * (0.025f + depth * 0.48f)
            val halfHeight = h * (0.018f + depth * 0.11f)
            val left = cx - halfWidth
            val right = cx + halfWidth
            val top = yTop
            val bottom = yBottom
            val bandAlpha = (0.12f + 0.36f * depth) * dim
            val bandColor = accent

            val rib = Path().apply {
                moveTo(left, bottom)
                lineTo(left, top + halfHeight * 0.45f)
                cubicTo(left, top - halfHeight * 0.50f, cx - halfWidth * 0.48f, top - halfHeight, cx, top - halfHeight)
                cubicTo(cx + halfWidth * 0.48f, top - halfHeight, right, top - halfHeight * 0.50f, right, top + halfHeight * 0.45f)
                lineTo(right, bottom)
            }
            drawPath(
                path = rib,
                color = TonalPalette.withAlpha(bandColor, bandAlpha * 0.38f),
                style = Stroke(width = minDim * (0.018f - depth * 0.010f).coerceAtLeast(0.004f)),
            )
            drawPath(
                path = rib,
                color = TonalPalette.withAlpha(bandColor, bandAlpha),
                style = Stroke(width = minDim * (0.0048f - depth * 0.002f).coerceAtLeast(0.0015f)),
            )
        }

        // Side rails and the central road perspective cue.
        val railAlpha = 0.12f * dim
        drawLine(
            color = TonalPalette.withAlpha(cool, railAlpha),
            start = Offset(cx, horizonY),
            end = Offset(-w * 0.12f, h * 1.05f),
            strokeWidth = minDim * 0.006f,
        )
        drawLine(
            color = TonalPalette.withAlpha(cool, railAlpha),
            start = Offset(cx, horizonY),
            end = Offset(w * 1.12f, h * 1.05f),
            strokeWidth = minDim * 0.006f,
        )
        drawLine(
            color = TonalPalette.withAlpha(accent, 0.10f * dim),
            start = Offset(cx, horizonY + h * 0.06f),
            end = Offset(cx, h * 1.03f),
            strokeWidth = minDim * 0.003f,
        )

        // Dim lower dashboard silhouette frames the point-of-view composition.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.76f * dim),
                startY = h * 0.78f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.78f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.22f),
        )
    }
}
