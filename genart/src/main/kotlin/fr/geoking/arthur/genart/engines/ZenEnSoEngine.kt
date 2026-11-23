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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Soft Ambient Zen En So — seamless loop via loopedFbm. */
@Composable
internal fun ZenEnSoEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "zenenso")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zenenso_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val bleed = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 31)
        val paperTop = TonalPalette.brightness(Color(0xFFF5F2EA), brightness * dim * (0.96f + wash * 0.04f))
        val paperBottom = TonalPalette.brightness(Color(0xFFE0D8C8), brightness * dim)
        drawRect(brush = Brush.verticalGradient(listOf(paperTop, paperBottom)))

        // A barely visible paper stain keeps the center warm without competing with the brush.
        val cx = w * 0.5f
        val cy = h * 0.51f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFC5B8A2), 0.11f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = minDim * 0.47f,
            ),
            radius = minDim * 0.47f,
            center = Offset(cx, cy),
        )

        val ink = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A2A28), TonalPalette.pick(paletteColors, 0), 0.12f),
            brightness * dim,
        )
        val radius = minDim * 0.285f
        val motion = (bleed - 0.5f) * 0.018f
        val start = -PI.toFloat() * 0.77f + motion
        val sweep = PI.toFloat() * 1.72f
        val samples = when (quality) {
            GenartQuality.Low -> 76
            GenartQuality.Medium -> 112
            GenartQuality.High -> 148
        }

        // Diffused ink beneath the dry brush edge.
        val diffuse = TonalPalette.withAlpha(ink, (0.10f + bleed * 0.06f) * dim)
        drawCircle(
            color = diffuse,
            radius = radius + minDim * 0.023f,
            center = Offset(cx, cy),
            style = Stroke(width = minDim * 0.025f),
        )

        for (i in 0..samples) {
            val u = i / samples.toFloat()
            val angle = start + sweep * u
            val breath = loopedFbm(time, radius = 1.15f + u * 0.2f, octaves = 2, seedOffset = 73 + i / 8)
            val wobble = (breath - 0.5f) * minDim * 0.012f
            val r = radius + wobble
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            val taper = (0.48f + 0.52f * sin(PI.toFloat() * u).coerceAtLeast(0f))
            val bristle = 0.82f + 0.18f * loopedFbm(time, radius = 1.4f, octaves = 1, seedOffset = 211 + i * 3)
            val dotRadius = minDim * 0.0105f * taper * bristle
            drawCircle(
                color = TonalPalette.withAlpha(ink, (0.84f + 0.12f * breath) * dim),
                radius = dotRadius,
                center = Offset(x, y),
            )
            // Fine dry-brush fibers sit along the outer edge, strongest at the two tapered ends.
            if (i % 4 == 0 && (u < 0.2f || u > 0.8f)) {
                val fiberOffset = minDim * 0.015f * (0.5f + 0.5f * breath)
                val fiberX = cx + cos(angle) * (r + fiberOffset)
                val fiberY = cy + sin(angle) * (r + fiberOffset)
                drawCircle(
                    color = TonalPalette.withAlpha(ink, 0.17f * dim),
                    radius = dotRadius * 0.22f,
                    center = Offset(fiberX, fiberY),
                )
            }
        }

        // Soft pooled ink at the ends of the unfinished circle.
        listOf(start, start + sweep).forEachIndexed { index, angle ->
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            drawCircle(
                color = TonalPalette.withAlpha(ink, (0.2f + 0.04f * bleed) * dim),
                radius = minDim * if (index == 0) 0.014f else 0.010f,
                center = Offset(x, y),
            )
        }
    }
}
