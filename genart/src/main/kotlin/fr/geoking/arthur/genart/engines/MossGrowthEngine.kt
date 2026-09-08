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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Fixed-position mossy patches that slowly breathe, growing then receding, each on its own staggered cycle. */
@Composable
internal fun MossGrowthEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val mossGreen = Color(0xFF4C6B3A)
    val count = qualityCount(quality, low = 6, medium = 10, high = 16)
    val patches = remember(count) {
        List(count) { i ->
            MossPatchSeed(
                x = seededUnit(i * 31 + 5),
                y = seededUnit(i * 43 + 9),
                radiusBaseFrac = seededRange(i * 59 + 13, 0.05f, 0.09f),
                radiusGrowFrac = seededRange(i * 71 + 17, 0.06f, 0.14f),
                growPhase = seededRange(i * 83 + 21, 0f, 2f * PI.toFloat()),
                colorMixT = seededRange(i * 97 + 25, 0.35f, 0.75f),
                alphaBase = seededRange(i * 109 + 29, 0.22f, 0.4f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "moss_growth")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moss_growth_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2B2620), Color(0xFF171410)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.85f,
            ),
        )
        val dim = if (isActive) 1f else 0.55f
        patches.forEach { patch ->
            val x = patch.x * w
            val y = patch.y * h
            val pulse = sin01(t * 2f * PI.toFloat() + patch.growPhase)
            val radius = (patch.radiusBaseFrac + patch.radiusGrowFrac * pulse) * minDim
            val base = TonalPalette.pick(paletteColors, patch.colorIndex)
            val tint = TonalPalette.brightness(TonalPalette.mix(base, mossGreen, patch.colorMixT), brightness)
            val alpha = patch.alphaBase * (0.6f + 0.4f * pulse) * dim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, alpha),
                        TonalPalette.withAlpha(tint, alpha * 0.35f),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class MossPatchSeed(
    val x: Float,
    val y: Float,
    val radiusBaseFrac: Float,
    val radiusGrowFrac: Float,
    val growPhase: Float,
    val colorMixT: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
