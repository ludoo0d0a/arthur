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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.exp

/** Controlled storm strobe (phone-only): distant cloud flashes, not full-frame flicker. */
@Composable
internal fun StormStrobePulseEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 5)
    val transition = rememberInfiniteTransition(label = "stormstrobepulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobepulse_t",
    )
    val dim = if (isActive) 1f else 0.45f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val cloud = loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = 7)
        val cloudWarp = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 19)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim),
                    TonalPalette.brightness(Color(0xFF1A2230), brightness * dim * (0.9f + 0.1f * cloud)),
                ),
            ),
        )
        // Keep the storm low on the horizon, with broken cloud shelves.
        val cloudY = h * (0.42f + 0.05f * cloudWarp)
        val shelfColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF111A28), TonalPalette.pick(paletteColors, 0), 0.12f),
            brightness * dim,
        )
        drawOval(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(shelfColor, 0.18f),
                1f to TonalPalette.withAlpha(Color(0xFF080C14), 0.88f * dim),
                startY = cloudY - h * 0.11f,
                endY = cloudY + h * 0.24f,
            ),
            topLeft = Offset(-w * 0.08f, cloudY - h * 0.10f),
            size = Size(w * 1.16f, h * 0.38f),
        )
        for (i in 0 until count) {
            val x = (i + 0.5f) / count * w + (loopedFbm(time, radius = 1.25f, seedOffset = 37 + i * 11) - 0.5f) * w * 0.16f
            val y = cloudY + h * (0.01f + 0.035f * (i % 2))
            val cloudWidth = w * (0.25f + 0.12f * loopedFbm(time, seedOffset = 71 + i * 7))
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFF263346), 0.34f * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = cloudWidth * 0.62f,
                ),
                topLeft = Offset(x - cloudWidth / 2f, y - h * 0.12f),
                size = Size(cloudWidth, h * 0.25f),
            )
        }

        // Rare, spatially diffuse flash envelope (narrow peaks).
        val flashPhase = (time + 0.37f) % 1f
        val flash = exp(-((flashPhase - 0.12f) * (flashPhase - 0.12f)) / 0.0018f)
        val flash2 = exp(-((flashPhase - 0.62f) * (flashPhase - 0.62f)) / 0.0012f)
        val flashAmt = (flash + 0.55f * flash2) * dim
        if (flashAmt > 0.05f) {
            val flashX = w * (0.30f + 0.40f * loopedFbm(time, radius = 1.2f, seedOffset = 91))
            val flashY = cloudY + h * 0.03f
            val glowRadius = minOf(w, h) * 0.42f
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFE8F0FF), 0.34f * flashAmt * brightness),
                    0.5f to TonalPalette.withAlpha(Color(0xFFBACBE2), 0.15f * flashAmt * brightness),
                    1f to Color.Transparent,
                    center = Offset(flashX, flashY),
                    radius = glowRadius,
                ),
                topLeft = Offset(flashX - glowRadius, flashY - glowRadius * 0.48f),
                size = Size(glowRadius * 2f, glowRadius * 0.96f),
            )
        }
        // Low cloud underbellies catch just a trace of the brief light.
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFBFCDE0), 0.18f * flashAmt * brightness),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, cloudY + h * 0.13f),
                radius = w * 0.46f,
            ),
            topLeft = Offset(w * 0.04f, cloudY + h * 0.01f),
            size = Size(w * 0.92f, h * 0.26f),
        )
    }
}
