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

/** A brief cloud-internal lightning bloom that swells and diffuses without a hard flash. */
@Composable
internal fun StormStrobeCloudEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val transition = rememberInfiniteTransition(label = "stormstrobecloud")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobecloud_t",
    )
    val dim = if (isActive) 1f else 0.45f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 17)
        val drift2 = loopedFbm(time, radius = 1.65f, octaves = 2, seedOffset = 43)

        // One short, soft swell per cycle, followed by a longer cloud-diffused afterglow.
        val localPhase = (time + 0.36f) % 1f
        val burst = exp(-((localPhase - 0.20f) * (localPhase - 0.20f)) / 0.0032f)
        val afterglow = exp(-((localPhase - 0.245f) * (localPhase - 0.245f)) / 0.018f) * 0.24f
        val light = (burst * 0.52f + afterglow) * dim
        val base = TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim)
        val cloudBase = TonalPalette.brightness(Color(0xFF1A2230), brightness * dim)
        val stormLight = TonalPalette.mix(Color(0xFFE8F0FF), TonalPalette.pick(paletteColors, 0), 0.08f)

        drawRect(
            brush = Brush.verticalGradient(
                0f to base,
                0.52f to TonalPalette.mix(base, cloudBase, 0.72f),
                1f to TonalPalette.mix(base, cloudBase, 0.92f),
            ),
        )

        // Distant cloud strata establish depth before the internal glow appears.
        for (layer in 0..2) {
            val y = h * (0.40f + layer * 0.105f)
            val layerAlpha = (0.40f + layer * 0.12f) * dim
            drawOval(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.2f to TonalPalette.withAlpha(cloudBase, layerAlpha),
                    0.52f to TonalPalette.withAlpha(TonalPalette.mix(cloudBase, Color(0xFF263144), 0.35f), layerAlpha),
                    0.82f to TonalPalette.withAlpha(cloudBase, layerAlpha),
                    1f to Color.Transparent,
                    startX = -w * 0.08f,
                    endX = w * 1.08f,
                ),
                topLeft = Offset(-w * 0.08f, y),
                size = Size(w * 1.16f, h * 0.30f),
            )
        }

        // The burst is concentrated inside the cloud bank; radial falloff keeps the edges calm.
        val glowCenter = Offset(w * (0.52f + (drift - 0.5f) * 0.10f), h * (0.58f + (drift2 - 0.5f) * 0.035f))
        val glowRadius = minDim * (0.34f + 0.10f * burst)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(stormLight, 0.55f * light * brightness),
                0.38f to TonalPalette.withAlpha(Color(0xFFBFCDE0), 0.36f * light * brightness),
                0.72f to TonalPalette.withAlpha(Color(0xFF8999B3), 0.18f * light * brightness),
                1f to Color.Transparent,
                center = glowCenter,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = glowCenter,
        )

        // Quality-scaled, overlapping foreground cloud lobes soften the light into the storm.
        repeat(cloudCount) { index ->
            val stagger = (index + 0.5f) / cloudCount
            val cloudNoise = loopedFbm(time, radius = 1.1f + stagger, octaves = 2, seedOffset = index * 29 + 7)
            val xRaw = stagger + (drift - 0.5f) * 0.18f + time * 0.30f
            val x = w * (((xRaw % 1f) + 1f) % 1f)
            val y = h * (0.46f + (index % 3) * 0.075f + (cloudNoise - 0.5f) * 0.04f)
            val rx = minDim * (0.24f + (index % 3) * 0.06f)
            val ry = minDim * (0.13f + (index % 2) * 0.045f)
            val distance = ((x - glowCenter.x) / (minDim * 0.68f)).let { it * it }
            val lit = (light * (0.24f + 0.46f * exp(-distance))).coerceIn(0f, 0.58f)
            val cloudColor = TonalPalette.mix(cloudBase, Color(0xFF39465B), lit)
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(cloudColor, (0.86f + 0.08f * cloudNoise) * dim),
                    0.74f to TonalPalette.withAlpha(cloudBase, 0.76f * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = rx,
                ),
                topLeft = Offset(x - rx, y - ry),
                size = Size(rx * 2f, ry * 2f),
            )
        }

        // A faint veil lets the glow breathe through cloud gaps without a full-frame whiteout.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.62f to TonalPalette.withAlpha(stormLight, 0.055f * light * brightness),
                1f to Color.Transparent,
            ),
        )
    }
}
