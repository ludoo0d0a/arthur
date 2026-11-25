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
import androidx.compose.ui.geometry.CornerRadius
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
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min

/**
 * A large soft moon hanging over a dark water surface, its light breaking into vertical bands
 * that shimmer, joined by slowly expanding concentric ripples on the water.
 */
@Composable
internal fun MoonlightRipplesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ringCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val fragmentCount = qualityCount(quality, low = 2, medium = 4, high = 6)
    val craterCount = qualityCount(quality, low = 2, medium = 5, high = 8)

    val origins = remember(2) {
        List(2) { o ->
            Offset(
                x = if (o == 0) 0.5f else seededRange(o * 131 + 5, 0.30f, 0.70f),
                y = if (o == 0) 0.6f else seededRange(o * 173 + 11, 0.6f, 0.88f),
            )
        }
    }
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            MoonRippleSeed(
                originIndex = i % 2,
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 1.9f, 3.0f),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.13f, 0.26f),
                baseAlpha = seededRange(i * 97 + 17, 0.25f, 0.42f),
                colorIndex = i,
            )
        }
    }
    val fragments = remember(fragmentCount) {
        List(fragmentCount) { i ->
            MoonFragmentSeed(
                phaseOffset = seededUnit(i * 89 + 7),
                speed = 0.7f + seededRange(i * 101 + 11, 0.5f, 1.1f),
                offsetFrac = seededUnit(i * 113 + 13) * 0.8f,
                widthFrac = 0.012f + seededUnit(i * 127 + 17) * 0.02f,
                alpha = 0.35f + seededRange(i * 131 + 19, 0.3f, 0.6f),
            )
        }
    }
    val craters = remember(craterCount) {
        List(craterCount) { i ->
            RippleCraterSeed(
                xFrac = seededRange(i * 211 + 3, -0.55f, 0.55f),
                yFrac = seededRange(i * 223 + 7, -0.55f, 0.55f),
                radiusFrac = seededRange(i * 227 + 11, 0.05f, 0.13f),
                alpha = 0.25f + seededRange(i * 229 + 17, 0.12f, 0.32f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "moonlight_ripples")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((11500 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moonlight_ripples_t",
    )
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((4300 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moonlight_ripples_glow",
    )

    val dim = if (isActive) 1f else 0.55f
    val moonColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)

            val waterTop = h * 0.56f
            val moonCx = w * 0.5f
            val moonCy = h * 0.24f
            val moonRadius = minDim * 0.22f
            val moonBottom = moonCy + moonRadius * 0.98f

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF02060C), Color(0xFF050D16), Color(0xFF010A12)),
                    startY = 0f,
                    endY = waterTop,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF050E18), Color(0xFF02060E), Color(0xFF01030A)),
                    startY = waterTop,
                    endY = h,
                ),
            )

            val haloPulse = 0.85f + 0.15f * sin01(phase01(glowT) * 2f * PI.toFloat())

            // Soft moon halo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(moonColor, 0.30f * dim * haloPulse), Color.Transparent),
                    center = Offset(moonCx, moonCy),
                    radius = moonRadius * 3.2f,
                ),
                radius = moonRadius * 3.2f,
                center = Offset(moonCx, moonCy),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(moonColor, 0.09f * dim * haloPulse), Color.Transparent),
                    center = Offset(moonCx, moonCy),
                    radius = moonRadius * 4.8f,
                ),
                radius = moonRadius * 4.8f,
                center = Offset(moonCx, moonCy),
            )

            val moonTint = TonalPalette.mix(Color(0xFFEFF4FB), moonColor, 0.18f)
            drawCircle(color = TonalPalette.brightness(moonTint, brightness), radius = moonRadius, center = Offset(moonCx, moonCy))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(Color.White, 0.92f * dim), TonalPalette.withAlpha(moonTint, 0.8f * dim)),
                    center = Offset(moonCx - moonRadius * 0.35f, moonCy - moonRadius * 0.35f),
                    radius = moonRadius * 1.3f,
                ),
                radius = moonRadius,
                center = Offset(moonCx, moonCy),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color(0xFF131B2A), 0.55f * dim)),
                    center = Offset(moonCx + moonRadius * 0.45f, moonCy + moonRadius * 0.45f),
                    radius = moonRadius * 1.35f,
                ),
                radius = moonRadius,
                center = Offset(moonCx, moonCy),
            )

            craters.forEach { crater ->
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFF93A2B8), crater.alpha * dim),
                    radius = crater.radiusFrac * moonRadius,
                    center = Offset(moonCx + crater.xFrac * moonRadius, moonCy + crater.yFrac * moonRadius),
                )
            }

            // Soft haze resting on the water surface.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(moonColor, 0.10f * dim * haloPulse), Color.Transparent),
                    center = Offset(moonCx, waterTop),
                    radius = minDim * 0.9f,
                ),
                radius = minDim * 0.9f,
                center = Offset(moonCx, waterTop),
            )

            // Concentric ripples in perspective (flattened ellipses) born on the water.
            rings.forEach { ring ->
                val life = phase01(t * ring.cyclesPerLoop + ring.phaseOffset)
                val origin = origins[ring.originIndex]
                val radius = life * ring.maxRadiusFrac * minDim
                if (radius > 0.5f) {
                    val alpha = (1f - life) * ring.baseAlpha * brightness.coerceAtMost(1.2f) * dim
                    val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIndex), brightness)
                    drawOval(
                        color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(origin.x * w - radius, origin.y * h - radius * 0.3f),
                        size = Size(radius * 2f, radius * 0.6f),
                        style = Stroke(width = (2.2f - life * 1.6f).coerceAtLeast(0.7f)),
                    )
                }
            }

            // Moon reflection: vertical light bands that break and shimmer over the water.
            val waterColumn = (waterTop - moonBottom).coerceAtLeast(0.5f)
            fragments.forEach { frag ->
                val shimmer = sin01(2f * PI.toFloat() * phase01(glowT * frag.speed + frag.phaseOffset))
                val length = waterColumn * (0.3f + 0.55f * shimmer)
                val alpha = (0.35f + 0.65f * shimmer) * frag.alpha * dim
                val x = moonCx + frag.offsetFrac * moonRadius * 0.9f
                val width = moonRadius * frag.widthFrac
                if (width > 0.5f) {
                    drawRoundRect(
                        color = TonalPalette.withAlpha(moonColor, alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(x - width * 0.5f, moonBottom),
                        size = Size(width, length),
                        cornerRadius = CornerRadius(width * 0.5f),
                    )
                }
            }
        }
    }
}

private data class MoonRippleSeed(
    val originIndex: Int,
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxRadiusFrac: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)

private data class MoonFragmentSeed(
    val phaseOffset: Float,
    val speed: Float,
    val offsetFrac: Float,
    val widthFrac: Float,
    val alpha: Float,
)

private data class RippleCraterSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val alpha: Float,
)
