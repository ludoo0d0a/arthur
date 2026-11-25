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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private data class StormCloudBank(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val phase: Float,
)

/** Controlled storm strobe (phone-only) — brief flashes, not continuous flicker. */
@Composable
internal fun StormStrobeSheetEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 5)
    val clouds = remember(count) {
        List(count) { i ->
            StormCloudBank(
                x = seededRange(i * 19 + 5, -0.18f, 0.92f),
                y = seededRange(i * 31 + 9, 0.22f, 0.72f),
                width = seededRange(i * 43 + 13, 0.38f, 0.72f),
                height = seededRange(i * 53 + 17, 0.10f, 0.24f),
                phase = seededRange(i * 61 + 21, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "stormstrobesheet")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobesheet_t",
    )
    val dim = if (isActive) 1f else 0.45f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val cloud = loopedFbm(time, radius = 1.4f, octaves = 3, seedOffset = 7)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim),
                    TonalPalette.brightness(Color(0xFF1A2230), brightness * dim * (0.9f + 0.1f * cloud)),
                ),
            ),
        )
        // A brief, diffuse horizontal sheet flash behind the cloud banks.
        val flashPhase = (time + 0.37f) % 1f
        val flash = exp(-((flashPhase - 0.12f) * (flashPhase - 0.12f)) / 0.0018f)
        val flash2 = exp(-((flashPhase - 0.62f) * (flashPhase - 0.62f)) / 0.0012f)
        val flashAmt = (flash + 0.45f * flash2).coerceIn(0f, 1f) * dim
        if (flashAmt > 0.025f) {
            val bandY = h * (0.36f + 0.035f * (cloud - 0.5f))
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.30f to TonalPalette.withAlpha(Color(0xFFE8F0FF), 0.12f * flashAmt * brightness),
                    0.50f to TonalPalette.withAlpha(Color(0xFFE8F0FF), 0.48f * flashAmt * brightness),
                    0.70f to TonalPalette.withAlpha(Color(0xFFD3E2FA), 0.18f * flashAmt * brightness),
                    1f to Color.Transparent,
                    startY = bandY - h * 0.25f,
                    endY = bandY + h * 0.25f,
                ),
            )
        }

        clouds.forEachIndexed { index, bank ->
            val drift = (loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = index * 37 + 19) - 0.5f) * w * 0.18f + time * w * 0.28f
            val bob = sin(time * 2f * PI.toFloat() + bank.phase) * h * 0.018f
            val span = w + bank.width * w
            val cx = ((bank.x * w + drift) % span + span) % span - bank.width * w * 0.25f
            val cy = bank.y * h + bob
            val cloudAlpha = (0.55f + flashAmt * 0.22f) * dim
            val cloudColor = TonalPalette.brightness(Color(0xFF1A2230), brightness * (0.92f + 0.08f * cloud))
            // Broad, soft cloud body with a faint illuminated upper edge during flashes.
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(cloudColor, cloudAlpha),
                    0.72f to TonalPalette.withAlpha(Color(0xFF101827), cloudAlpha * 0.82f),
                    1f to Color.Transparent,
                    center = Offset(cx + bank.width * w * 0.5f, cy + bank.height * h * 0.55f),
                    radius = bank.width * w * 0.58f,
                ),
                topLeft = Offset(cx, cy),
                size = androidx.compose.ui.geometry.Size(bank.width * w, bank.height * h),
            )
            if (flashAmt > 0.04f) {
                drawLine(
                    color = TonalPalette.withAlpha(Color(0xFFE8F0FF), 0.13f * flashAmt * brightness),
                    start = Offset(cx + bank.width * w * 0.18f, cy + bank.height * h * 0.16f),
                    end = Offset(cx + bank.width * w * 0.82f, cy + bank.height * h * 0.16f),
                    strokeWidth = minOf(w, h) * 0.006f,
                )
            }
        }
    }
}
