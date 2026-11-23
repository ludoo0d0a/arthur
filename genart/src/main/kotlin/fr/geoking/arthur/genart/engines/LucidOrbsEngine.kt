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
import kotlin.math.cos
import kotlin.math.sin

private data class LucidOrbsMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
    val depth: Float,
)

/** Soft Ambient Lucid Orbs — seamless loop via loopedFbm. */
@Composable
internal fun LucidOrbsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = remember(count) {
        List(count) { i ->
            LucidOrbsMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.04f, 0.14f),
                driftX = seededRange(i * 53 + 13, 0.02f, 0.06f),
                driftY = seededRange(i * 67 + 19, 0.015f, 0.05f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
                depth = seededRange(i * 83 + 31, 0.55f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "lucidorbs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lucidorbs_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF0B1020), brightness * dim),
                0.55f to TonalPalette.brightness(Color(0xFF121828), brightness * dim * (0.92f + 0.08f * wash)),
                1f to TonalPalette.brightness(Color(0xFF1C2840), brightness * dim),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF80D0FF), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )
        val violet = TonalPalette.brightness(Color(0xFF9F8CFF), brightness * dim)
        val focalX = w * (0.35f + 0.3f * loopedFbm(time, radius = 1.2f, seedOffset = 29))
        val focalY = h * (0.3f + 0.25f * loopedFbm(time, radius = 1.4f, seedOffset = 47))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.22f * dim),
                0.5f to TonalPalette.withAlpha(violet, 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(focalX, focalY),
                radius = minDim * 0.7f,
            ),
            radius = minDim * 0.7f,
            center = Offset(focalX, focalY),
        )
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.25f + mote.radius, octaves = 3, seedOffset = mote.colorIndex * 97 + 5)
            val phase = time * 2f * PI.toFloat() + mote.phaseOffset
            val x = (mote.x0 + mote.driftX * sin(phase) + (n - 0.5f) * 0.025f) * w
            val y = (mote.y0 + mote.driftY * cos(phase * 0.72f) + (n - 0.5f) * 0.02f) * h
            val r = minDim * mote.radius * (0.76f + 0.42f * n)
            val center = Offset(x, y)
            val orbColor = if (mote.colorIndex % 2 == 0) accent else violet
            val glowRadius = r * (2.5f + 0.8f * n)

            // Wide bloom keeps the spheres suspended in the dark atmosphere.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(orbColor, 0.16f * dim * mote.depth),
                    0.45f to TonalPalette.withAlpha(orbColor, 0.06f * dim * mote.depth),
                    1f to Color.Transparent,
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )
            // Translucent orb body with a brighter upper-left rim and shaded lower edge.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFE5F5FF), (0.52f + 0.12f * n) * dim),
                    0.22f to TonalPalette.withAlpha(orbColor, 0.32f * dim),
                    0.72f to TonalPalette.withAlpha(orbColor, 0.13f * dim),
                    1f to TonalPalette.withAlpha(orbColor, 0.38f * dim),
                    center = Offset(x - r * 0.32f, y - r * 0.35f),
                    radius = r * 1.7f,
                ),
                radius = r,
                center = center,
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFDFF7FF), (0.36f + 0.2f * n) * dim),
                radius = r * 0.11f,
                center = Offset(x - r * 0.32f, y - r * 0.36f),
            )
        }
        // Fine bokeh pinpricks strengthen the sense of depth without competing with the orbs.
        for (i in 0 until count) {
            val bx = (seededUnit(i * 137 + 41) + 0.02f * sin(time * 2f * PI.toFloat() + i)) * w
            val by = (seededUnit(i * 149 + 43) + 0.015f * cos(time * 2f * PI.toFloat() + i)) * h
            val bokehAlpha = (0.12f + 0.1f * loopedFbm(time, radius = 1.1f, seedOffset = i * 31 + 7)) * dim
            drawCircle(TonalPalette.withAlpha(accent, bokehAlpha), radius = minDim * 0.0025f, center = Offset(bx, by))
        }
    }
}
