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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** A close-up woven fiber field with a slow, seamless breathing wave. */
@Composable
internal fun MicroFiberEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val strandCount = qualityCount(quality, low = 20, medium = 34, high = 50)
    val transition = rememberInfiniteTransition(label = "microfiber")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "microfiber_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val breathe = loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = 196)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE0C0A0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )
        val base = TonalPalette.brightness(Color(0xFF201818), brightness * dim)
        val shadow = TonalPalette.brightness(Color(0xFF101010), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(base, shadow)))

        // Diffuse grazing light reveals the fine weave without a hard source.
        val glowCenter = Offset(w * (0.46f + 0.08f * (breathe - 0.5f)), h * 0.47f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.22f * dim),
                0.52f to TonalPalette.withAlpha(Color(0xFF806454), 0.08f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = (minDim * 0.76f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.76f).coerceAtLeast(1f),
            center = glowCenter,
        )

        val verticalStep = w / strandCount
        val horizontalStep = h / strandCount
        val segments = 12
        for (i in 0..strandCount) {
            val u = i.toFloat() / strandCount
            val centerLight = (1f - kotlin.math.abs(u - 0.5f) * 1.5f).coerceIn(0.12f, 1f)
            val verticalPath = Path()
            val horizontalPath = Path()
            for (segment in 0..segments) {
                val p = segment.toFloat() / segments
                val waveV = sin((p * 2f * PI.toFloat()) + i * 0.71f + time * 2f * PI.toFloat())
                val waveH = sin((p * 2f * PI.toFloat()) + i * 0.57f - time * 2f * PI.toFloat())
                val warpV = (waveV * 0.0028f + (breathe - 0.5f) * 0.002f) * minDim
                val warpH = (waveH * 0.0028f + (breathe - 0.5f) * 0.002f) * minDim
                val vx = i * verticalStep + warpV
                val hy = i * horizontalStep + warpH
                val vy = p * h
                val hx = p * w
                if (segment == 0) {
                    verticalPath.moveTo(vx, vy)
                    horizontalPath.moveTo(hx, hy)
                } else {
                    verticalPath.lineTo(vx, vy)
                    horizontalPath.lineTo(hx, hy)
                }
            }

            // Alternating strand brightness creates a fine over/under weave.
            val verticalAlpha = (if (i % 2 == 0) 0.22f else 0.11f) * centerLight * dim
            val horizontalAlpha = (if (i % 2 == 0) 0.12f else 0.24f) * centerLight * dim
            drawPath(
                path = verticalPath,
                color = TonalPalette.withAlpha(if (i % 2 == 0) accent else Color(0xFF6A5145), verticalAlpha),
                style = Stroke(width = (minDim * 0.0035f).coerceAtLeast(0.7f), cap = StrokeCap.Round),
            )
            drawPath(
                path = horizontalPath,
                color = TonalPalette.withAlpha(if (i % 2 == 0) Color(0xFF9B806D) else accent, horizontalAlpha),
                style = Stroke(width = (minDim * 0.003f).coerceAtLeast(0.65f), cap = StrokeCap.Round),
            )
        }

        // A fine, low-contrast diagonal grain gives the fabric a macro texture.
        val grainAlpha = (0.08f + 0.04f * breathe) * dim
        for (i in -strandCount..strandCount) {
            val x = i * verticalStep
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFFE8D4C0), grainAlpha),
                start = Offset(x, 0f),
                end = Offset(x + h * 0.26f, h),
                strokeWidth = (minDim * 0.001f).coerceAtLeast(0.45f),
            )
        }
    }
}
