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
import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Avion Sunrise Band — seamless 68-second cloud drift. */
@Composable
internal fun AvionSunriseBandEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "avionsunriseband")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avionsunriseband_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = sin(time * 2f * PI.toFloat())
        val horizonY = h * (0.57f + 0.012f * drift)
        val paletteCoral = TonalPalette.mix(Color(0xFFE08060), TonalPalette.pick(paletteColors, 0), 0.22f)
        val paletteGold = TonalPalette.mix(Color(0xFFFFE0A0), TonalPalette.pick(paletteColors, 1), 0.18f)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF2A1840), brightness * dim * (0.94f + 0.06f * wash)),
                0.38f to TonalPalette.brightness(Color(0xFF665071), brightness * dim),
                0.53f to TonalPalette.brightness(paletteCoral, brightness * dim),
                0.60f to TonalPalette.brightness(paletteGold, brightness * dim),
                0.70f to TonalPalette.brightness(Color(0xFF423044), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF080A0C), brightness * dim),
            ),
        )

        // Broad sunrise bloom, softened by the cloud layer at the horizon.
        val glowCenter = Offset(w * 0.53f, horizonY + h * 0.025f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(paletteGold, 0.48f * dim),
                0.38f to TonalPalette.withAlpha(paletteCoral, 0.20f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = minDim * 0.58f,
            ),
            radius = minDim * 0.58f,
            center = glowCenter,
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(paletteGold, 0.24f * dim),
                1f to Color.Transparent,
                startY = horizonY - h * 0.07f,
                endY = horizonY + h * 0.08f,
            ),
            topLeft = Offset(0f, horizonY - h * 0.07f),
            size = Size(w, h * 0.15f),
        )

        // Receding cloud shelves below and across the sunrise band.
        for (i in 0 until cloudCount) {
            val fi = i.toFloat()
            val n = loopedFbm(time, radius = 1.2f + fi * 0.06f, octaves = 2, seedOffset = 47 + i * 29)
            val baseX = ((fi * 0.31f + n * 0.19f + drift * 0.025f + 1f) % 1f) * w
            val y = horizonY + h * (0.045f + (i % 4) * 0.075f) + h * 0.009f * sin(time * 2f * PI.toFloat() + fi)
            val cloudW = w * (0.20f + 0.25f * n)
            val cloudH = h * (0.035f + 0.035f * n)
            val cloudColor = if (i % 3 == 0) paletteCoral else Color(0xFF171522)
            val alpha = (if (y < horizonY + h * 0.12f) 0.27f else 0.72f) * dim
            drawOval(
                color = TonalPalette.withAlpha(TonalPalette.brightness(cloudColor, brightness * dim), alpha),
                topLeft = Offset(baseX - cloudW * 0.5f, y),
                size = Size(cloudW, cloudH),
            )
            drawOval(
                color = TonalPalette.withAlpha(TonalPalette.brightness(cloudColor, brightness * dim), alpha * 0.72f),
                topLeft = Offset(baseX - cloudW * 0.2f, y - cloudH * 0.38f),
                size = Size(cloudW * 0.58f, cloudH * 0.72f),
            )
            drawOval(
                color = TonalPalette.withAlpha(paletteGold, 0.16f * n * dim),
                topLeft = Offset(baseX - cloudW * 0.45f, y + cloudH * 0.08f),
                size = Size(cloudW * 0.78f, cloudH * 0.22f),
            )
        }

        // A faint airplane window rim frames the sky without obscuring it.
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF080A0C), 0.28f * dim),
            topLeft = Offset(w * 0.025f, h * 0.018f),
            size = Size(w * 0.95f, h * 0.964f),
            cornerRadius = CornerRadius(minDim * 0.065f),
            style = Stroke(width = minDim * 0.018f),
        )
    }
}
