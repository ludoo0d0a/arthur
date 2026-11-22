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
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private data class StormStrobeCloudBolt(
    val x0: Float,
    val y0: Float,
    val length: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Controlled storm strobe (phone-only) — brief flashes, not continuous flicker. */
@Composable
internal fun StormStrobeCloudEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 5)
    val bolts = remember(count) {
        List(count) { i ->
            StormStrobeCloudBolt(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededRange(i * 31 + 9, 0.05f, 0.35f),
                length = seededRange(i * 43 + 13, 0.35f, 0.7f),
                phaseOffset = seededUnit(i * 53 + 17),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "stormstrobecloud")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobecloud_t",
    )
    val dim = if (isActive) 1f else 0.45f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val cloud = loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = 7)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim),
                    TonalPalette.brightness(Color(0xFF1A2230), brightness * dim * (0.9f + 0.1f * cloud)),
                ),
            ),
        )
        // Rare controlled flash envelope (narrow peaks)
        val flashPhase = (time + 0.37f) % 1f
        val flash = exp(-((flashPhase - 0.12f) * (flashPhase - 0.12f)) / 0.0018f)
        val flash2 = exp(-((flashPhase - 0.62f) * (flashPhase - 0.62f)) / 0.0012f)
        val flashAmt = (flash + 0.55f * flash2) * dim
        if (flashAmt > 0.05f) {
            drawRect(color = TonalPalette.withAlpha(Color(0xFFE8F0FF), 0.55f * flashAmt * brightness))
        }
        bolts.forEach { bolt ->
            val local = ((time + bolt.phaseOffset) % 1f)
            val boltFlash = exp(-((local - 0.15f) * (local - 0.15f)) / 0.0009f) * dim
            if (boltFlash < 0.08f) return@forEach
            val x = bolt.x0 * w
            val y0 = bolt.y0 * h
            val y1 = y0 + bolt.length * h
            val col = TonalPalette.withAlpha(
                TonalPalette.brightness(TonalPalette.pick(paletteColors, bolt.colorIndex), brightness),
                0.75f * boltFlash,
            )
            drawLine(color = col, start = Offset(x, y0), end = Offset(x + (loopedFbm(time, seedOffset = bolt.colorIndex * 13) - 0.5f) * w * 0.08f, y1), strokeWidth = minOf(w, h) * 0.008f)
        }
    }
}
