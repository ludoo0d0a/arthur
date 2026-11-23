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
import kotlin.math.cos
import kotlin.math.sin

private data class CityLight(
    val x: Float,
    val y: Float,
    val size: Float,
    val warmth: Float,
    val phase: Float,
)

/** Quiet aerial view above a sleeping city, with a seamless drifting night haze. */
@Composable
internal fun AvionNightCityEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 42, medium = 76, high = 112)
    val lights = remember(count) {
        List(count) { i ->
            val row = seededUnit(i * 31 + 13)
            CityLight(
                x = seededUnit(i * 17 + 3),
                y = row,
                size = seededRange(i * 41 + 11, 0.0015f, 0.005f) * (0.45f + row),
                warmth = seededRange(i * 53 + 19, 0.35f, 1f),
                phase = seededRange(i * 67 + 23, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "avionNightCity")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avionNightCity_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 29)
        val breathing = loopedFbm(time, radius = 1.6f, octaves = 2, seedOffset = 53)
        val horizonY = h * (0.40f + 0.018f * (drift - 0.5f))

        val skyTop = TonalPalette.brightness(Color(0xFF060810), brightness * dim)
        val cityBase = TonalPalette.brightness(Color(0xFF101828), brightness * dim)
        drawRect(brush = Brush.verticalGradient(0f to skyTop, 0.44f to cityBase, 1f to Color(0xFF080A0C)))

        // Distant urban haze at the horizon.
        val haze = TonalPalette.mix(Color(0xFFFFE080), TonalPalette.pick(paletteColors, 0), 0.16f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.42f to TonalPalette.withAlpha(haze, 0.11f * dim),
                1f to Color.Transparent,
                startY = horizonY - h * 0.12f,
                endY = horizonY + h * 0.24f,
            ),
        )

        // Far skyline silhouettes, kept low and soft against the haze.
        val skylineY = horizonY + h * 0.035f
        for (i in 0..20) {
            val x0 = w * (i / 21f)
            val buildingW = w * (0.012f + seededUnit(i * 37 + 1) * 0.026f)
            val buildingH = h * (0.018f + seededUnit(i * 47 + 9) * 0.085f)
            drawRect(
                color = TonalPalette.withAlpha(Color(0xFF090E18), 0.52f * dim),
                topLeft = Offset(x0, skylineY - buildingH),
                size = androidx.compose.ui.geometry.Size(buildingW, buildingH),
            )
        }

        // Receding avenue lines form a broad aerial grid rather than a moving traffic scene.
        val gridTop = horizonY + h * 0.025f
        val gridBottom = h * 1.06f
        for (i in -7..7) {
            val bottomX = w * (0.5f + i * 0.115f)
            val topX = w * (0.5f + i * 0.018f)
            val path = Path().apply {
                moveTo(w * 0.5f + (topX - w * 0.5f) * 0.28f, gridTop)
                lineTo(bottomX, gridBottom)
            }
            val alpha = (0.035f + 0.035f * (1f - kotlin.math.abs(i) / 8f)) * dim
            drawPath(path, Color(0xFFFFC86A).copy(alpha = alpha))
        }
        for (row in 0..10) {
            val u = row / 10f
            val y = gridTop + (gridBottom - gridTop) * u * u
            val alpha = (0.028f + 0.025f * u) * dim
            drawLine(
                color = Color(0xFFFFD98C).copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = minDim * (0.001f + 0.0014f * u),
            )
        }

        lights.forEachIndexed { index, light ->
            val n = loopedFbm(time, radius = 1.35f + light.size, octaves = 2, seedOffset = index * 97 + 5)
            val sway = sin(time * 2f * PI.toFloat() + light.phase) * 0.0022f
            val x = (light.x + sway + 1f) % 1f * w
            val y = horizonY + h * (0.035f + light.y * 0.94f)
            val perspective = (0.3f + light.y * 0.9f)
            val radius = (minDim * light.size * perspective * (0.8f + 0.28f * n)).coerceAtLeast(0.8f)
            val warm = TonalPalette.mix(Color(0xFFFFE080), Color(0xFFFFA94D), 1f - light.warmth)
            val glowColor = TonalPalette.withAlpha(warm, (0.20f + 0.18f * n) * dim)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to glowColor,
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = radius * 5.5f,
                ),
                radius = radius * 5.5f,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(warm, (0.24f + 0.48f * n) * dim),
                radius = radius,
                center = Offset(x, y),
            )
        }

        // A few broad luminous districts add depth without suggesting moving vehicles.
        for (i in 0..3) {
            val cx = w * (0.16f + i * 0.22f) + (drift - 0.5f) * w * 0.015f
            val cy = horizonY + h * (0.24f + (i % 2) * 0.18f)
            val glow = (0.045f + 0.025f * breathing) * dim
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFFFC66A).copy(alpha = glow),
                    0.4f to Color(0xFFFFC66A).copy(alpha = glow * 0.42f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = minDim * (0.14f + 0.02f * breathing),
                ),
                radius = minDim * (0.14f + 0.02f * breathing),
                center = Offset(cx, cy),
            )
        }

        // Subtle interior window rim at the lower edge.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color(0xFF080A0C).copy(alpha = 0.45f * dim),
                startY = h * 0.91f,
                endY = h,
            ),
        )
    }
}
