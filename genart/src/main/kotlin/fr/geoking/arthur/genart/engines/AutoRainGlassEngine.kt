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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class RainGlassTrail(
    val x: Float,
    val y: Float,
    val length: Float,
    val sway: Float,
    val width: Float,
    val speed: Float,
    val phase: Float,
)

private data class RainGlassLight(val x: Float, val y: Float, val radius: Float, val warm: Boolean)

/** Night drive seen through rain-streaked glass; slow motion loops without simulating traffic. */
@Composable
internal fun AutoRainGlassEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val trails = remember(count) {
        List(count * 2) { i ->
            RainGlassTrail(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                length = seededRange(i * 41 + 11, 0.10f, 0.34f),
                sway = seededRange(i * 53 + 13, 0.006f, 0.026f),
                width = seededRange(i * 67 + 19, 0.7f, 2.2f),
                speed = seededRange(i * 79 + 23, 0.45f, 0.95f),
                phase = seededUnit(i * 83 + 31),
            )
        }
    }
    val lights = remember {
        List(9) { i ->
            RainGlassLight(
                x = seededRange(i * 23 + 5, 0.06f, 0.94f),
                y = seededRange(i * 37 + 11, 0.24f, 0.62f),
                radius = seededRange(i * 47 + 13, 0.018f, 0.05f),
                warm = i % 3 != 0,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "autorainonglass")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autorainonglass_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val time = phase01(t)
        val atmosphere = loopedFbm(time, radius = 1.4f, octaves = 3, seedOffset = 11)
        val glass = TonalPalette.brightness(Color(0xFF0C1014), brightness * dim)
        val lowerNight = TonalPalette.brightness(Color(0xFF182028), brightness * dim)
        drawRect(brush = Brush.verticalGradient(listOf(glass, Color(0xFF111820), lowerNight)))

        // Distant city lights, kept soft to read as a view outside the windshield.
        lights.forEachIndexed { i, light ->
            val shimmer = loopedFbm(time, radius = 1.2f + light.radius, octaves = 2, seedOffset = i * 61 + 47)
            val color = if (light.warm) Color(0xFFFFC98A) else Color(0xFFA0C8E0)
            val center = Offset(light.x * w, light.y * h)
            val r = m * light.radius * (0.9f + shimmer * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(TonalPalette.brightness(color, brightness * dim), 0.38f * dim),
                    0.35f to TonalPalette.withAlpha(color, 0.20f * dim),
                    1f to Color.Transparent,
                    center = center,
                    radius = r * 3.2f,
                ),
                radius = r * 3.2f,
                center = center,
            )
            drawCircle(color = TonalPalette.withAlpha(color, (0.20f + shimmer * 0.12f) * dim), radius = r, center = center)
        }

        // Low, diffuse horizon wash; no distinct moving cars or traffic simulation.
        val horizon = h * (0.54f + (atmosphere - 0.5f) * 0.025f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFFA0C8E0), 0.10f * dim),
                startY = horizon - h * 0.09f,
                endY = horizon + h * 0.09f,
            ),
            topLeft = Offset(0f, horizon - h * 0.09f),
            size = Size(w, h * 0.18f),
        )

        // Fine rain rivulets slide down the glass on a wrapped phase.
        trails.forEachIndexed { i, trail ->
            val local = phase01(time * trail.speed + trail.phase)
            val y0 = (trail.y + local * (1f + trail.length)) % 1f * h
            val x0 = trail.x * w + sin(time * 2f * PI.toFloat() + trail.phase * 2f * PI.toFloat()) * trail.sway * w
            val len = trail.length * h
            val drift = sin(time * 2f * PI.toFloat() + trail.phase * 2f * PI.toFloat()) * trail.sway * w
            val alpha = (0.11f + 0.09f * loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = i * 19 + 7)) * dim
            val rainColor = TonalPalette.brightness(Color(0xFFA0C8E0), brightness * dim)
            val path = Path().apply {
                moveTo(x0, y0)
                cubicTo(x0 + drift * 0.4f, y0 + len * 0.32f, x0 - drift * 0.2f, y0 + len * 0.68f, x0 + drift * 0.25f, y0 + len)
            }
            drawPath(path, TonalPalette.withAlpha(rainColor, alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(width = trail.width, cap = StrokeCap.Round))
            // Tiny bead at the leading end catches the outside light.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFDDF2FF), 0.32f * dim),
                    1f to Color.Transparent,
                    center = Offset(x0 + drift * 0.25f, y0 + len),
                    radius = m * 0.009f,
                ),
                radius = m * 0.009f,
                center = Offset(x0 + drift * 0.25f, y0 + len),
            )
        }

        // Soft lower dashboard silhouette and cool reflected glow on glass.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.88f * dim),
                startY = h * 0.78f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.22f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFA0C8E0), 0.11f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, h * 0.86f),
                radius = m * 0.42f,
            ),
            radius = m * 0.42f,
            center = Offset(w * 0.5f, h * 0.86f),
        )
    }
}
