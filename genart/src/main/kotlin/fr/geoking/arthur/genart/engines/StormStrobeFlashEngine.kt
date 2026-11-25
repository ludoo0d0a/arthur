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
import kotlin.math.exp

/** Controlled storm strobe (phone-only): a single brief sheet-lightning flash behind clouds. */
@Composable
internal fun StormStrobeFlashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val transition = rememberInfiniteTransition(label = "stormstrobeflash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobeflash_t",
    )
    val dim = if (isActive) 1f else 0.45f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val cloudMotion = loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = 7)
        val flashPhase = (time + 0.37f) % 1f
        val flash = exp(-((flashPhase - 0.13f) * (flashPhase - 0.13f)) / 0.0016f)
        val flashAmt = (flash * dim).coerceIn(0f, 1f)
        val lightningTint = TonalPalette.mix(Color(0xFFE8F0FF), TonalPalette.pick(paletteColors, 0), 0.12f)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF080C16), brightness * dim),
                0.58f to TonalPalette.brightness(Color(0xFF111A28), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF202938), brightness * dim * (0.9f + 0.1f * cloudMotion)),
            ),
        )

        // One broad, diffuse sheet-lightning glow per cycle, kept behind the cloud bank.
        val glowCenter = Offset(w * (0.49f + 0.06f * (cloudMotion - 0.5f)), h * 0.61f)
        val glowRadius = minDim * (0.74f + 0.04f * flashAmt)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(lightningTint, 0.55f * flashAmt * brightness),
                0.4f to TonalPalette.withAlpha(Color(0xFFB8C8E0), 0.28f * flashAmt * brightness),
                1f to Color.Transparent,
                center = glowCenter,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = glowCenter,
        )

        // Soft low cloud bank with clear horizontal drift + brief sheet flash behind it.
        val cloudColor = TonalPalette.brightness(Color(0xFF111925), brightness * dim)
        repeat(cloudCount) { index ->
            val xRaw = (index + 0.35f) / cloudCount + (cloudMotion - 0.5f) * 0.22f + time * 0.35f
            val x = ((xRaw % 1f) + 1f) % 1f
            val y = 0.48f + (index % 3) * 0.07f
            val rx = minDim * (0.26f + (index % 3) * 0.05f)
            val ry = minDim * (0.14f + (index % 2) * 0.04f)
            val center = Offset(x * w, y * h)
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(cloudColor, 0.94f),
                    0.72f to TonalPalette.withAlpha(cloudColor, 0.78f),
                    1f to Color.Transparent,
                    center = center,
                    radius = rx,
                ),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
            )
        }

        // Keep just a restrained ambient response to the flash, with no full-frame whiteout.
        if (flashAmt > 0.015f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to TonalPalette.withAlpha(Color(0xFFDCE8FA), 0.32f * flashAmt * brightness),
                    1f to Color.Transparent,
                ),
            )
        }
    }
}
