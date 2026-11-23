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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV US Retrowave Horizon — a softly glowing synthwave road and grid. */
@Composable
internal fun UsRetrowaveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "usretrowave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "usretrowave_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val horizon = h * (0.53f + (wash - 0.5f) * 0.025f)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFF60C0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )
        val orange = TonalPalette.brightness(Color(0xFFFF985E), brightness * dim)
        val dark = TonalPalette.brightness(Color(0xFF180828), brightness * dim)
        val deep = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF080A14),
                0.38f to dark,
                0.72f to Color(0xFF2A1040),
                1f to deep,
            ),
        )

        // Atmospheric sunset glow at the vanishing point.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(orange, 0.30f * dim),
                0.42f to TonalPalette.withAlpha(accent, 0.18f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizon),
                radius = (minDim * 0.62f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.62f).coerceAtLeast(1f),
            center = Offset(w * 0.5f, horizon),
        )

        // Striped sun, kept soft so it reads as a horizon glow rather than a hard lamp.
        val sunRadius = minDim * 0.16f
        val sunCenter = Offset(w * 0.5f, horizon - minDim * 0.045f)
        drawCircle(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFBD78), 0.76f * dim),
                0.58f to TonalPalette.withAlpha(accent, 0.68f * dim),
                1f to TonalPalette.withAlpha(Color(0xFFFF4CA8), 0.08f * dim),
                startY = sunCenter.y - sunRadius,
                endY = sunCenter.y + sunRadius,
            ),
            radius = sunRadius,
            center = sunCenter,
        )
        for (i in 0 until 5) {
            val stripeY = sunCenter.y + sunRadius * (0.16f + i * 0.14f)
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFF281038), (0.30f + i * 0.035f) * dim),
                start = Offset(w * 0.5f - sunRadius, stripeY),
                end = Offset(w * 0.5f + sunRadius, stripeY),
                strokeWidth = minDim * (0.009f + i * 0.001f),
            )
        }

        // Distant mountain silhouettes around the sunset.
        val ridge = Path().apply {
            moveTo(0f, horizon + minDim * 0.035f)
            lineTo(w * 0.13f, horizon - minDim * 0.01f)
            lineTo(w * 0.25f, horizon + minDim * 0.04f)
            lineTo(w * 0.39f, horizon - minDim * 0.025f)
            lineTo(w * 0.48f, horizon + minDim * 0.02f)
            lineTo(w * 0.62f, horizon - minDim * 0.03f)
            lineTo(w * 0.76f, horizon + minDim * 0.035f)
            lineTo(w * 0.88f, horizon - minDim * 0.015f)
            lineTo(w, horizon + minDim * 0.04f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = ridge, color = TonalPalette.withAlpha(Color(0xFF130B25), 0.88f * dim))

        // Neon road grid. Lines wrap from the horizon to the foreground once per cycle,
        // fading at both ends so the wrap stays soft.
        val gridTop = horizon + minDim * 0.055f
        val gridHeight = h - gridTop
        val rows = when (quality) {
            GenartQuality.Low -> 9
            GenartQuality.Medium -> 13
            GenartQuality.High -> 17
        }
        for (i in 0 until rows) {
            val u = ((i + 1f) / rows + time) % 1f
            val y = gridTop + gridHeight * u * u
            val half = w * (0.06f + 0.48f * u)
            drawLine(
                color = TonalPalette.withAlpha(accent, (0.48f * sin(u * PI.toFloat())).coerceAtLeast(0f) * dim),
                start = Offset(w * 0.5f - half, y),
                end = Offset(w * 0.5f + half, y),
                strokeWidth = minDim * (0.0015f + 0.0025f * u),
            )
        }
        for (i in -6..6) {
            val bottomX = w * 0.5f + i * w * 0.105f
            drawLine(
                color = TonalPalette.withAlpha(accent, (0.13f + 0.12f * (1f - kotlin.math.abs(i) / 6f)) * dim),
                start = Offset(w * 0.5f + i * minDim * 0.003f, gridTop),
                end = Offset(bottomX, h),
                strokeWidth = minDim * 0.0018f,
            )
        }

        // Low, diffuse horizon bloom and a faint scanline texture.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to TonalPalette.withAlpha(accent, 0.14f * dim),
                1f to Color.Transparent,
                startY = horizon - minDim * 0.07f,
                endY = horizon + minDim * 0.09f,
            ),
            topLeft = Offset(0f, horizon - minDim * 0.07f),
            size = androidx.compose.ui.geometry.Size(w, minDim * 0.16f),
        )
        val scanCount = when (quality) {
            GenartQuality.Low -> 8
            GenartQuality.Medium -> 14
            GenartQuality.High -> 20
        }
        for (i in 0 until scanCount) {
            val y = h * (i + 1f) / (scanCount + 1f)
            val shimmer = 0.035f + 0.018f * sin(time * 2f * PI.toFloat() + i)
            drawLine(
                color = TonalPalette.withAlpha(accent, shimmer * dim),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = minDim * 0.0008f,
            )
        }
    }
}
