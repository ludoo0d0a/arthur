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
import kotlin.math.sin

private data class DustGrain(
    val x: Float,
    val y: Float,
    val size: Float,
    val depth: Float,
    val drift: Float,
    val phase: Float,
    val colorIndex: Int,
)

/** Fine suspended particles floating through a dim, softly lit atmosphere. */
@Composable
internal fun MicroDustEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val qualityScale = qualityCount(quality, low = 4, medium = 7, high = 11)
    val grains = remember(qualityScale) {
        List(qualityScale * 9) { i ->
            DustGrain(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                size = seededRange(i * 41 + 11, 0.0008f, 0.0032f),
                depth = seededRange(i * 53 + 13, 0.25f, 1f),
                drift = seededRange(i * 67 + 19, 0.004f, 0.022f),
                phase = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "microdust")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "microdust_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val haze = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFD0C8B0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF18161A), brightness * dim * (0.92f + 0.08f * haze)),
                0.52f to TonalPalette.brightness(Color(0xFF111014), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF0C0A10), brightness * dim),
            ),
        )

        // Diffuse side light catches only a small region of the suspended dust.
        val glowCenter = Offset(
            w * (0.22f + 0.08f * loopedFbm(time, radius = 1.1f, seedOffset = 29)),
            h * (0.28f + 0.08f * loopedFbm(time, radius = 1.3f, seedOffset = 47)),
        )
        val glowRadius = minDim * 0.68f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.12f * dim),
                0.45f to TonalPalette.withAlpha(Color(0xFF8C8068), 0.045f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = glowCenter,
        )

        grains.forEach { grain ->
            val noise = loopedFbm(time, radius = 1.15f + grain.depth, octaves = 2, seedOffset = grain.colorIndex * 97 + 5)
            val angle = time * (2f * PI.toFloat()) + grain.phase
            val x = (grain.x + sin(angle) * grain.drift + (noise - 0.5f) * grain.drift * 0.35f + 1f) % 1f * w
            val y = (grain.y + sin(angle * 0.73f) * grain.drift * 0.7f + 1f) % 1f * h
            val radius = minDim * grain.size * (0.8f + 0.35f * noise) * grain.depth
            val color = TonalPalette.mix(accent, TonalPalette.pick(paletteColors, grain.colorIndex + 1), 0.3f)
            val alpha = (0.08f + 0.16f * noise) * grain.depth * dim
            if (grain.depth > 0.62f) {
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha * 0.28f),
                    radius = radius * 3.2f,
                    center = Offset(x, y),
                )
            }
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = radius.coerceAtLeast(0.45f),
                center = Offset(x, y),
            )
        }
    }
}
