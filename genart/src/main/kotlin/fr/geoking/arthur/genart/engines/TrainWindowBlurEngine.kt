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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange

private data class WindowLight(
    val y: Float,
    val height: Float,
    val width: Float,
    val speed: Float,
    val offset: Float,
    val colorIndex: Int,
    val alpha: Float,
)

/** Soft Ambient POV Train Window Blur — passing countryside lights through glass. */
@Composable
internal fun TrainWindowBlurEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val lights = androidx.compose.runtime.remember(count) {
        List(count) { i ->
            WindowLight(
                y = seededRange(i * 17 + 3, 0.12f, 0.82f),
                height = seededRange(i * 29 + 7, 0.012f, 0.055f),
                width = seededRange(i * 41 + 11, 0.16f, 0.48f),
                speed = seededRange(i * 53 + 13, 0.75f, 1.55f),
                offset = seededRange(i * 67 + 19, 0f, 1f),
                colorIndex = i,
                alpha = seededRange(i * 71 + 23, 0.12f, 0.34f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "trainwindowblur")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trainwindowblur_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val cool = TonalPalette.brightness(Color(0xFF1A1E28), brightness * dim)
        val deep = TonalPalette.brightness(Color(0xFF0E1018), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF80A0C0), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )

        // Dark interior and a soft horizon beyond the window.
        drawRect(brush = Brush.verticalGradient(listOf(cool, deep, Color(0xFF080A0C))))
        val horizon = h * (0.53f + (wash - 0.5f) * 0.035f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.48f to TonalPalette.withAlpha(accent, 0.12f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.18f,
                endY = horizon + h * 0.18f,
            ),
        )

        // Broad horizontal bands read as blurred fields, hedgerows and distant lights.
        val bands = 8
        for (i in 0 until bands) {
            val n = loopedFbm(time, radius = 1.15f + i * 0.08f, octaves = 2, seedOffset = 73 + i * 31)
            val y = h * (0.17f + i * 0.096f) + (n - 0.5f) * h * 0.035f
            val bandH = h * (0.014f + 0.024f * n)
            val tint = if (i % 3 == 0) Color(0xFF657080) else Color(0xFF343A45)
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to TonalPalette.withAlpha(tint, 0.09f * dim),
                    1f to Color.Transparent,
                    startY = y - bandH,
                    endY = y + bandH,
                ),
                topLeft = Offset(0f, y - bandH),
                size = Size(w, bandH * 2f),
            )
        }

        // Passing lights move laterally and wrap cleanly at the loop boundary.
        lights.forEach { light ->
            val travel = (time * light.speed + light.offset) % 1f
            val x = travel * (w + light.width * w) - light.width * w
            val y = light.y * h + (loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 151 + light.colorIndex * 23) - 0.5f) * h * 0.025f
            val color = TonalPalette.brightness(
                if (light.colorIndex % 3 == 0) Color(0xFFB5A28B) else TonalPalette.pick(paletteColors, light.colorIndex),
                brightness * dim,
            )
            val glow = light.width * w
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(color, light.alpha * dim),
                    0.55f to TonalPalette.withAlpha(color, light.alpha * 0.32f * dim),
                    1f to Color.Transparent,
                    center = Offset(x + glow * 0.5f, y),
                    radius = (glow * 0.7f).coerceAtLeast(1f),
                ),
                topLeft = Offset(x, y - light.height * h * 2f),
                size = Size(glow, light.height * h * 4f),
            )
        }

        // Soft pane reflections and a barely visible rounded frame.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.42f * dim),
                0.035f to Color.Transparent,
                0.965f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.42f * dim),
            ),
        )
        drawRect(
            color = TonalPalette.withAlpha(Color(0xFF080A0C), 0.28f * dim),
            topLeft = Offset(w * 0.035f, h * 0.025f),
            size = Size(w * 0.93f, h * 0.95f),
            style = Stroke(width = minDim * 0.018f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.35f * dim),
                startY = h * 0.78f,
                endY = h,
            ),
        )
    }
}
