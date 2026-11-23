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
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class PollenGrain(val x: Float, val y: Float, val radius: Float)

/** Macro pollen grains suspended in warm botanical backlight. */
@Composable
internal fun MicroPollenCloseEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val grains = remember(count) {
        List(count) { i ->
            PollenGrain(
                x = seededUnit(i * 31 + 9),
                y = seededUnit(i * 47 + 3),
                radius = seededRange(i * 59 + 5, 0.012f, 0.035f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "micropollenclose")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "micropollenclose_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val breeze = loopedFbm(time, radius = 1.2f, octaves = 3, seedOffset = 43)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFE080), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * dim,
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF2A2210), brightness * dim),
                0.58f to TonalPalette.brightness(Color(0xFF18140A), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF0C100A), brightness * dim),
            ),
        )

        // Broad defocused leaf forms frame a shallow macro depth of field.
        val leaf = Path().apply {
            moveTo(-w * 0.08f, h * 0.82f)
            cubicTo(w * 0.16f, h * 0.56f, w * 0.36f, h * 0.63f, w * 0.54f, h * 1.08f)
            cubicTo(w * 0.30f, h * 0.93f, w * 0.08f, h * 0.91f, -w * 0.08f, h * 0.82f)
            close()
        }
        drawPath(leaf, TonalPalette.withAlpha(Color(0xFF35451C), 0.5f * dim))
        drawPath(
            path = leaf,
            color = TonalPalette.withAlpha(Color(0xFF819344), 0.28f * dim),
            style = Stroke(width = unit * 0.004f),
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF9DAD54), 0.26f * dim),
            start = Offset(w * 0.04f, h * 0.84f),
            end = Offset(w * 0.43f, h * 0.96f),
            strokeWidth = unit * 0.003f,
        )

        val glowCenter = Offset(w * 0.47f, h * (0.38f + 0.025f * sin(time * 2f * PI.toFloat())))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.48f * dim),
                0.38f to TonalPalette.withAlpha(Color(0xFFB87828), 0.18f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = unit * 0.72f,
            ),
            radius = unit * 0.72f,
            center = glowCenter,
        )

        // A few oversized pollen grains sit close to the lens.
        for (i in 0 until 3) {
            val x = w * (0.27f + i * 0.24f) + unit * 0.012f * sin(time * 2f * PI.toFloat() + i)
            val y = h * (0.40f + (i % 2) * 0.12f) + unit * 0.008f * sin(time * 2f * PI.toFloat() + i * 1.7f)
            val r = unit * (0.047f + i * 0.009f) * (0.96f + 0.04f * breeze)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFFFF2B0), 0.96f * dim),
                    0.3f to TonalPalette.withAlpha(Color(0xFFFFD45A), 0.94f * dim),
                    0.78f to TonalPalette.withAlpha(Color(0xFFB56A1C), 0.86f * dim),
                    1f to TonalPalette.withAlpha(Color(0xFF58310F), 0.7f * dim),
                    center = Offset(x - r * 0.22f, y - r * 0.28f),
                    radius = r * 1.45f,
                ),
                radius = r,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, 0.78f * dim),
                radius = r * 0.12f,
                center = Offset(x - r * 0.25f, y - r * 0.31f),
            )
        }

        grains.forEachIndexed { i, grain ->
            val drift = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = i * 97 + 17)
            val x = ((grain.x + 0.018f * (drift - 0.5f) + 1f) % 1f) * w
            val y = ((grain.y - time * 0.035f + 1f) % 1f) * h
            val r = unit * grain.radius * (0.85f + 0.2f * drift)
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFFFE894), (0.85f * dim)),
                    0.7f to TonalPalette.withAlpha(Color(0xFFE3A63B), (0.72f * dim)),
                    1f to Color.Transparent,
                    center = Offset(x - r * 0.18f, y - r * 0.2f),
                    radius = r,
                ),
                topLeft = Offset(x - r, y - r * 0.72f),
                size = Size(r * 2f, r * 1.44f),
            )
        }
    }
}
