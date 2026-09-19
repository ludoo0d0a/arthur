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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * A large cratered full moon filling the frame — soft terminator, pale glow, sparse stars.
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
    val starCount = qualityCount(quality, low = 20, medium = 32, high = 48)
    val stars = remember(starCount) {
        List(starCount) { i ->
            Offset(seededUnit(i * 13 + 5), seededUnit(i * 19 + 9)) to
                seededRange(i * 31 + 11, 0.1f, 0.4f)
        }
    }

    val transition = rememberInfiniteTransition(label = "moon")
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moon_glow",
    )

    val dim = if (isActive) 1f else 0.55f
    val pulse = 0.9f + 0.1f * sin01(phase01(glowT) * 2f * PI.toFloat())
    val moonTint = TonalPalette.brightness(
        TonalPalette.mix(Color(0xFFE8ECF4), TonalPalette.pick(paletteColors, 0), 0.15f),
        brightness,
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF04060E), Color(0xFF010208)),
                ),
            )
            stars.forEach { (pos, alpha) ->
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.7f * dim),
                    radius = 1.2f,
                    center = Offset(pos.x * w, pos.y * h),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val moonR = minDim * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(moonTint, 0.4f * dim * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = moonR * 1.55f,
                ),
                radius = moonR * 1.55f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val moonR = minDim * 0.36f

            drawCircle(color = Color(0xFFD8DDE8), radius = moonR, center = Offset(cx, cy))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.9f * dim),
                        TonalPalette.withAlpha(moonTint, 0.7f * dim),
                    ),
                    center = Offset(cx - moonR * 0.35f, cy - moonR * 0.35f),
                    radius = moonR * 1.3f,
                ),
                radius = moonR,
                center = Offset(cx, cy),
            )
            // Terminator / night side
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF1A2030).copy(alpha = 0.65f * dim)),
                    center = Offset(cx + moonR * 0.45f, cy + moonR * 0.35f),
                    radius = moonR * 1.35f,
                ),
                radius = moonR,
                center = Offset(cx, cy),
            )

            craters.forEach { crater ->
                val dx = crater.xFrac * moonR
                val dy = crater.yFrac * moonR
                if (dx * dx + dy * dy > moonR * moonR * 0.85f) return@forEach
                val r = crater.radiusFrac * moonR
                val px = cx + dx
                val py = cy + dy
                drawCircle(
                    color = Color(0xFF9AA3B4).copy(alpha = crater.depth * dim),
                    radius = r,
                    center = Offset(px, py),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f * dim),
                    radius = r * 0.85f,
                    center = Offset(px - r * 0.15f, py - r * 0.15f),
                )
                drawCircle(
                    color = Color(0xFF2A3344).copy(alpha = 0.35f * crater.depth * dim),
                    radius = r * 0.7f,
                    center = Offset(px + r * 0.12f, py + r * 0.12f),
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
