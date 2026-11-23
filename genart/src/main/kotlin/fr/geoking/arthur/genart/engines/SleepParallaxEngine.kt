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
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class SleepParallaxMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient Sleep Parallax — layered night ridges and lucid lights in a seamless loop. */
@Composable
internal fun SleepParallaxEngine(
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
            SleepParallaxMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.04f, 0.14f),
                driftX = seededRange(i * 53 + 13, 0.02f, 0.06f),
                driftY = seededRange(i * 67 + 19, 0.015f, 0.05f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "sleepparallax")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sleepparallax_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val minDim = minOf(w, h)
        val driftFar = loopedFbm(time, radius = 0.95f, octaves = 2, seedOffset = 61) - 0.5f
        val driftNear = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 89) - 0.5f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF0E1020), brightness * dim * (0.94f + 0.06f * wash)),
                    TonalPalette.brightness(Color(0xFF1A1830), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFA090C8), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // Distant moon-like orb and a broad halo above the layered night ridges.
        val cx = w * (0.5f + 0.12f * driftFar)
        val cy = h * (0.28f + 0.025f * (loopedFbm(time, radius = 1.4f, seedOffset = 47) - 0.5f))
        val orbRadius = minDim * 0.075f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.15f * dim),
                0.45f to TonalPalette.withAlpha(accent, 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = minDim * 0.43f,
            ),
            radius = minDim * 0.43f,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFE0D8FF), 0.46f * dim),
                0.62f to TonalPalette.withAlpha(accent, 0.20f * dim),
                1f to TonalPalette.withAlpha(accent, 0.02f * dim),
                center = Offset(cx, cy),
                radius = orbRadius,
            ),
            radius = orbRadius,
            center = Offset(cx, cy),
        )

        // Three hazy ridge planes move at different rates to create gentle parallax.
        fun ridgePath(baseY: Float, amplitude: Float, offset: Float): Path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, baseY)
            for (step in 0..12) {
                val x = w * step / 12f
                val envelope = sin((step / 12f + offset + 1f) * PI.toFloat())
                val ripple = loopedFbm(time, radius = 1.05f + step * 0.025f, octaves = 2, seedOffset = 120 + step * 13) - 0.5f
                lineTo(x, baseY + envelope * amplitude * 0.38f + ripple * amplitude * 0.32f)
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = ridgePath(h * 0.64f, h * 0.075f, driftFar * 0.18f),
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF3B3D68), 0.42f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF22233F), 0.70f * dim),
                startY = h * 0.58f,
                endY = h,
            ),
        )
        drawPath(
            path = ridgePath(h * 0.74f, h * 0.09f, driftNear * 0.3f + 0.36f),
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF292B4C), 0.82f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF17182B), 0.96f * dim),
                startY = h * 0.68f,
                endY = h,
            ),
        )
        drawPath(
            path = ridgePath(h * 0.86f, h * 0.045f, driftNear * 0.52f + 0.71f),
            color = TonalPalette.withAlpha(Color(0xFF101222), 0.96f * dim),
        )
        // A fine soft horizon glow separates the far plane without a hard light source.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(accent, 0.08f * dim),
                1f to Color.Transparent,
                startY = h * 0.58f,
                endY = h * 0.78f,
            ),
        )

        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.25f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + 1f) % 1f * w
            val depth = 0.35f + (mote.colorIndex % 4) * 0.18f
            val y = (mote.y0 * 0.58f + 0.18f + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.35f + 1f) % 1f * h
            val r = minDim * (0.006f + 0.009f * depth) * (0.8f + 0.35f * n)
            val col = TonalPalette.withAlpha(
                TonalPalette.brightness(TonalPalette.mix(accent, TonalPalette.pick(paletteColors, mote.colorIndex), 0.25f), brightness * dim),
                (0.22f + 0.22f * n) * dim,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(col, 0.8f),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = r * 3.5f,
                ),
                radius = r * 3.5f,
                center = Offset(x, y),
            )
            drawCircle(color = col, radius = r, center = Offset(x, y))
        }
    }
}
