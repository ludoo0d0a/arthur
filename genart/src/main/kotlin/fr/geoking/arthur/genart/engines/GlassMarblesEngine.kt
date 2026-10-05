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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Scattered translucent glass marbles on a dark surface — colored bodies, rim light,
 * specular glints, soft elliptical floor reflections, and a slow roll.
 */
@Composable
internal fun GlassMarblesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 7, medium = 11, high = 16)
    val marbles = remember(count) {
        List(count) { i ->
            GlassMarbleSeed(
                x0 = seededRange(i * 17 + 3, 0.12f, 0.88f),
                y0 = seededRange(i * 29 + 7, 0.38f, 0.82f),
                radiusFrac = seededRange(i * 41 + 11, 0.055f, 0.12f),
                rollAmp = seededRange(i * 53 + 13, 0.008f, 0.028f),
                rollFreq = seededRange(i * 67 + 19, 0.15f, 0.45f),
                rollPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                swirlAngle = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                swirlSpin = seededRange(i * 97 + 31, 0.2f, 0.7f),
                highlightAngle = seededRange(i * 103 + 37, -0.9f, -0.35f),
                alpha = seededRange(i * 109 + 41, 0.45f, 0.75f),
                colorIndex = i,
            )
        }.sortedBy { it.y0 }
    }
    val transition = rememberInfiniteTransition(label = "glass_marbles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((38000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glass_marbles_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A1620), Color(0xFF0C0A10), Color(0xFF050408)),
            ),
        )
        // Soft table plane
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2434).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.78f),
                radius = minDim * 0.7f,
            ),
            radius = minDim * 0.7f,
            center = Offset(w * 0.5f, h * 0.78f),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f

        marbles.forEach { m ->
            val roll = m.rollAmp * sin(time * m.rollFreq + m.rollPhase)
            val cx = (m.x0 + roll) * w
            val cy = m.y0 * h
            val r = m.radiusFrac * minDim
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    TonalPalette.pick(paletteColors, m.colorIndex),
                    Color(0xFFE8F0FF),
                    0.18f,
                ),
                brightness,
            )
            val a = m.alpha * dim

            // Floor reflection (flattened ellipse)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, a * 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy + r * 0.85f),
                    radius = r * 1.1f,
                ),
                topLeft = Offset(cx - r * 0.95f, cy + r * 0.55f),
                size = Size(r * 1.9f, r * 0.55f),
            )

            // Glass body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, a * 0.55f),
                        TonalPalette.withAlpha(tint, a * 0.7f),
                        TonalPalette.withAlpha(tint, a * 0.25f),
                        TonalPalette.withAlpha(Color(0xFF101018), a * 0.55f),
                    ),
                    center = Offset(cx - r * 0.28f, cy - r * 0.32f),
                    radius = r * 1.15f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )

            // Internal swirl (fake refraction)
            val swirl = m.swirlAngle + time * m.swirlSpin
            val sx = cx + cos(swirl) * r * 0.22f
            val sy = cy + sin(swirl) * r * 0.18f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, a * 0.55f),
                        Color.Transparent,
                    ),
                    center = Offset(sx, sy),
                    radius = r * 0.55f,
                ),
                radius = r * 0.55f,
                center = Offset(sx, sy),
            )

            // Rim
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, a * 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = (r * 0.04f).coerceAtLeast(0.8f)),
            )

            // Specular highlight
            val hx = cx + cos(m.highlightAngle) * r * 0.38f
            val hy = cy + sin(m.highlightAngle) * r * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, a * 0.95f),
                        Color.Transparent,
                    ),
                    center = Offset(hx, hy),
                    radius = r * 0.28f,
                ),
                radius = r * 0.28f,
                center = Offset(hx, hy),
            )
            // Secondary glint
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, a * 0.45f),
                radius = r * 0.06f,
                center = Offset(cx + r * 0.42f, cy - r * 0.15f),
            )
        }
    }
}

private data class GlassMarbleSeed(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val rollAmp: Float,
    val rollFreq: Float,
    val rollPhase: Float,
    val swirlAngle: Float,
    val swirlSpin: Float,
    val highlightAngle: Float,
    val alpha: Float,
    val colorIndex: Int,
)
