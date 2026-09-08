package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Synthwave grid floor: a perspective horizon of slowly cross-fading cool-toned lines — car-safe, no strobe. */
@Composable
internal fun DataHorizonEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lineCount = qualityCount(quality, low = 10, medium = 16, high = 24)
    val fanCount = qualityCount(quality, low = 5, medium = 7, high = 9)

    val transition = rememberInfiniteTransition(label = "data_horizon")
    val tintT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "data_horizon_tint",
    )

    val dim = if (isActive) 1f else 0.55f
    val cyan = TonalPalette.mix(Color(0xFF33F0FF), TonalPalette.pick(paletteColors, 0), 0.25f)
    val violet = TonalPalette.mix(Color(0xFFA855F7), TonalPalette.pick(paletteColors, 1), 0.25f)
    val blue = TonalPalette.mix(Color(0xFF3B82F6), TonalPalette.pick(paletteColors, 2), 0.25f)
    val cycle = phase01(tintT) * 3f
    val tint = when {
        cycle < 1f -> TonalPalette.mix(cyan, violet, sin01(cycle * PI.toFloat() / 2f))
        cycle < 2f -> TonalPalette.mix(violet, blue, sin01((cycle - 1f) * PI.toFloat() / 2f))
        else -> TonalPalette.mix(blue, cyan, sin01((cycle - 2f) * PI.toFloat() / 2f))
    }
    val gridColor = TonalPalette.brightness(tint, brightness * dim)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF05060C), Color(0xFF000000)),
                ),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(6.dp)) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.42f
            val vanishingX = w * 0.5f
            val spacingFactor = 6f

            for (i in 0 until lineCount) {
                val t = i / lineCount.toFloat()
                val y = horizonY + (h - horizonY) * (1f - 1f / (1f + t * spacingFactor))
                val alpha = (0.5f - t * 0.35f).coerceIn(0.08f, 0.5f) * dim
                drawLine(
                    color = TonalPalette.withAlpha(gridColor, alpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f,
                )
            }

            for (i in 0 until fanCount) {
                val t = i / (fanCount - 1).coerceAtLeast(1).toFloat()
                val bottomX = t * w
                val alpha = 0.28f * dim
                drawLine(
                    color = TonalPalette.withAlpha(gridColor, alpha),
                    start = Offset(vanishingX, horizonY),
                    end = Offset(bottomX, h),
                    strokeWidth = 1.5f,
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(gridColor, 0.35f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(vanishingX, horizonY),
                    radius = w * 0.35f,
                ),
                radius = w * 0.35f,
                center = Offset(vanishingX, horizonY),
            )
        }
    }
}
