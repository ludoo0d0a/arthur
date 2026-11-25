package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.genartSoftBlur
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * A large cratered full moon — dark maria, soft terminator, light-edge craters, granulation,
 * slow drift plus a dark cloud band sweeping across. Deep night-blue sky, gentle star twinkle,
 * softly pulsing halo.
 * Distinct from [MoonlightRipplesEngine] (small moon over water).
 */
@Composable
internal fun MoonEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val craterCount = qualityCount(quality, low = 6, medium = 10, high = 16)
    val craters = remember(craterCount) {
        List(craterCount) { i ->
            MoonCraterSeed(
                xFrac = seededRange(i * 17 + 3, -0.7f, 0.7f),
                yFrac = seededRange(i * 29 + 7, -0.7f, 0.7f),
                radiusFrac = seededRange(i * 41 + 11, 0.04f, 0.14f),
                depth = seededRange(i * 53 + 13, 0.15f, 0.4f),
            )
        }
    }
    val seaCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val seas = remember(seaCount) {
        List(seaCount) { i ->
            MoonSeaSeed(
                xFrac = seededRange(i * 11 + 5, -0.6f, 0.6f),
                yFrac = seededRange(i * 23 + 9, -0.6f, 0.6f),
                radiusFrac = seededRange(i * 37 + 13, 0.07f, 0.16f),
            )
        }
    }
    val grainCount = qualityCount(quality, low = 10, medium = 16, high = 24)
    val grains = remember(grainCount) {
        List(grainCount) { i ->
            MoonGrainSeed(
                xFrac = seededRange(i * 7 + 3, -0.65f, 0.65f),
                yFrac = seededRange(i * 11 + 5, -0.65f, 0.65f),
                radiusFrac = seededRange(i * 13 + 7, 0.006f, 0.02f),
                alpha = seededRange(i * 17 + 9, 0.05f, 0.14f),
            )
        }
    }
    val starCount = qualityCount(quality, low = 18, medium = 30, high = 44)
    val stars = remember(starCount) {
        List(starCount) { i ->
            MoonStarSeed(
                xFrac = seededUnit(i * 13 + 5),
                yFrac = seededUnit(i * 19 + 9),
                alpha = seededRange(i * 31 + 11, 0.12f, 0.34f),
                radius = 0.8f + seededRange(i * 37 + 13, 0f, 0.9f),
                phase = seededUnit(i * 41 + 15) * 2f * PI.toFloat(),
                twinkleSpeed = seededRange(i * 43 + 17, 1.8f, 3.4f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "moon")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moon_t",
    )
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((3600 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moon_glow",
    )

    val dim = if (isActive) 1f else 0.55f
    val haloPulse = 0.88f + 0.12f * sin01(phase01(glowT) * 2f * PI.toFloat())
    val moonTint = TonalPalette.brightness(
        TonalPalette.mix(Color(0xFFE8ECF4), TonalPalette.pick(paletteColors, 0), 0.15f),
        brightness,
    )
    val nightSide = TonalPalette.brightness(moonTint, 0.42f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1228), Color(0xFF020409)),
                ),
            )
            stars.forEach { star ->
                val twinkle = sin01(phase01(glowT) * 2f * PI.toFloat() * star.twinkleSpeed + star.phase)
                val alpha = (star.alpha * (0.5f + 0.5f * twinkle) * 0.7f * dim).coerceIn(0f, 1f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.radius,
                    center = Offset(star.xFrac * w, star.yFrac * h),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().genartSoftBlur(quality, 18.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val moonR = minDim * 0.30f
            val drift = sin01(phase01(t) * 2f * PI.toFloat()) * minDim * 0.02f
            val cx = w * 0.64f + drift
            val cy = h * 0.5f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(moonTint, 0.38f * dim * haloPulse),
                    0.45f to TonalPalette.withAlpha(moonTint, 0.16f * dim * haloPulse),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = moonR * 1.7f,
                ),
                radius = moonR * 1.7f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val moonR = minDim * 0.30f
            val drift = sin01(phase01(t) * 2f * PI.toFloat()) * minDim * 0.02f
            val cx = w * 0.64f + drift
            val cy = h * 0.5f
            val bandOffset = (phase01(t) - 0.5f) * 2f * moonR * 1.3f

            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFFFF6E0), 0.95f * dim * haloPulse),
                    0.42f to TonalPalette.withAlpha(moonTint, 0.8f * dim * haloPulse),
                    1f to TonalPalette.withAlpha(nightSide, dim * haloPulse),
                    center = Offset(cx - moonR * 0.3f, cy - moonR * 0.3f),
                    radius = moonR * 1.25f,
                ),
                radius = moonR,
                center = Offset(cx, cy),
            )

            seas.forEach { sea ->
                val dx = sea.xFrac * moonR
                val dy = sea.yFrac * moonR
                if (dx * dx + dy * dy > moonR * moonR * 0.9f) return@forEach
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFF35415C), 0.55f * dim * haloPulse),
                    radius = sea.radiusFrac * moonR,
                    center = Offset(cx + dx, cy + dy),
                )
            }

            grains.forEach { grain ->
                val dx = grain.xFrac * moonR
                val dy = grain.yFrac * moonR
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFFF2F5FA), grain.alpha * dim * haloPulse),
                    radius = grain.radiusFrac * moonR,
                    center = Offset(cx + dx, cy + dy),
                )
            }

            val bandPath = Path().apply {
                moveTo(cx - moonR * 1.7f - bandOffset, cy - moonR * 0.5f)
                quadraticBezierTo(
                    cx - bandOffset,
                    cy - moonR * 1.25f,
                    cx + moonR * 1.7f - bandOffset,
                    cy - moonR * 0.5f,
                )
            }
            drawPath(
                path = bandPath,
                color = TonalPalette.withAlpha(Color(0xFF3E4A66), 0.36f * dim * haloPulse),
                style = Stroke(width = moonR * 0.22f, cap = StrokeCap.Round),
            )

            craters.forEach { crater ->
                val dx = crater.xFrac * moonR
                val dy = crater.yFrac * moonR
                if (dx * dx + dy * dy > moonR * moonR * 0.85f) return@forEach
                val r = crater.radiusFrac * moonR
                val px = cx + dx
                val py = cy + dy
                drawCircle(
                    color = Color(0xFF141B2C).copy(alpha = (0.5f + 0.5f * crater.depth) * dim * haloPulse),
                    radius = r,
                    center = Offset(px, py),
                )
                drawCircle(
                    color = Color(0xFFF4F7FC).copy(alpha = (0.3f + 0.45f * crater.depth) * dim * haloPulse),
                    radius = r * 0.86f,
                    center = Offset(px - r * 0.18f, py - r * 0.18f),
                )
                drawCircle(
                    color = Color(0xFF0B101B).copy(alpha = crater.depth * dim * haloPulse),
                    radius = r * 0.68f,
                    center = Offset(px + r * 0.14f, py + r * 0.14f),
                )
            }
        }
    }
}

private data class MoonCraterSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val depth: Float,
)

private data class MoonSeaSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
)

private data class MoonGrainSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val alpha: Float,
)

private data class MoonStarSeed(
    val xFrac: Float,
    val yFrac: Float,
    val alpha: Float,
    val radius: Float,
    val phase: Float,
    val twinkleSpeed: Float,
)
