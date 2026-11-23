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
import androidx.compose.runtime.remember
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

/** Soft Ambient Zen Stone Ripples — a quiet raked garden with a seamless ripple loop. */
@Composable
internal fun ZenStoneRipplesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grooveCount = qualityCount(quality, low = 14, medium = 22, high = 32)
    val grooveOffsets = remember(grooveCount) { List(grooveCount) { it / (grooveCount - 1f) } }
    val transition = rememberInfiniteTransition(label = "zenstoneripples")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zenstoneripples_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val tau = 2f * PI.toFloat()
        val ebb = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 11)
        val sand = TonalPalette.brightness(Color(0xFFD8E0D8), brightness * dim)
        val sandShade = TonalPalette.brightness(Color(0xFFB7C6BB), brightness * dim)
        val stone = TonalPalette.brightness(Color(0xFF8AA090), brightness * dim)
        val deepStone = TonalPalette.brightness(Color(0xFF2E3A32), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2E3A32), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )

        drawRect(brush = Brush.verticalGradient(listOf(sand, sandShade, sand)))
        // A subtle quieting vignette keeps the garden centered without a hard border.
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(deepStone, 0.18f * dim),
                center = Offset(w * 0.5f, h * 0.52f),
                radius = minDim * 0.78f,
            ),
        )

        // Fine raked grooves, gently bending around three stones.
        grooveOffsets.forEachIndexed { index, fraction ->
            val y = h * (0.08f + fraction * 0.84f)
            val wave = sin(fraction * tau * 2f + time * tau) * minDim * 0.004f * (0.85f + 0.15f * ebb)
            val path = Path().apply {
                moveTo(-w * 0.04f, y)
                cubicTo(w * 0.26f, y + wave, w * 0.70f, y - wave, w * 1.04f, y)
            }
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(if (index % 3 == 0) Color.White else deepStone, if (index % 3 == 0) 0.18f * dim else 0.12f * dim),
                style = Stroke(width = minDim * 0.0018f),
            )
        }

        val stones = listOf(
            Triple(0.33f, 0.42f, 0.19f),
            Triple(0.65f, 0.59f, 0.15f),
            Triple(0.43f, 0.72f, 0.105f),
        )
        stones.forEachIndexed { index, (x, y, radius) ->
            val center = Offset(w * x, h * y)
            val rx = minDim * radius
            val ry = rx * (0.54f + index * 0.025f)
            val pulse = 0.5f + 0.5f * sin(time * tau + index * 1.7f)

            // Soft cast shadow and smooth river stone.
            drawOval(
                color = TonalPalette.withAlpha(deepStone, 0.19f * dim),
                topLeft = Offset(center.x - rx * 0.92f, center.y - ry * 0.32f),
                size = Size(rx * 1.9f, ry * 1.35f),
            )
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(TonalPalette.mix(stone, Color.White, 0.24f), stone, deepStone.copy(alpha = 0.72f)),
                    start = Offset(center.x - rx, center.y - ry),
                    end = Offset(center.x + rx, center.y + ry),
                ),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
            )
            drawOval(
                color = TonalPalette.withAlpha(Color.White, 0.32f * dim),
                topLeft = Offset(center.x - rx * 0.62f, center.y - ry * 0.65f),
                size = Size(rx * 0.84f, ry * 0.32f),
            )

            // Concentric rings flow around each stone and expand gently in place.
            val ringCount = 4
            for (ring in 0 until ringCount) {
                val ringPhase = (time + ring.toFloat() / ringCount + index * 0.17f) % 1f
                val radiusScale = 1.12f + ringPhase * 0.75f
                val alpha = (1f - ringPhase) * (0.18f + 0.08f * pulse) * dim
                drawOval(
                    color = TonalPalette.withAlpha(accent, alpha),
                    topLeft = Offset(center.x - rx * radiusScale, center.y - ry * radiusScale),
                    size = Size(rx * 2f * radiusScale, ry * 2f * radiusScale),
                    style = Stroke(width = minDim * 0.0022f),
                )
            }
        }
    }
}
