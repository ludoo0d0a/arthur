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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI

// Signature cosmic blue and pink tones for high-quality space nebula art
private val CosmicBluePinkColors = listOf(
    Color(0xFF0F172A), // Dark slate blue
    Color(0xFF1E3A8A), // Deep royal blue
    Color(0xFF2563EB), // Vivid blue
    Color(0xFF0284C7), // Bright cyan-blue
    Color(0xFF3B82F6), // Electric blue
    Color(0xFF701A75), // Deep magenta
    Color(0xFFBE185D), // Rich pink
    Color(0xFFDB2777), // Hot pink
    Color(0xFFEC4899), // Vibrant pink
    Color(0xFFF472B6), // Soft glowing pink
    Color(0xFFE0F2FE), // Ice blue highlight
    Color(0xFFFCE7F3), // Ice pink highlight
)

/** Dense, high-quality soft nebula clouds drifting slowly across a deep sky for TV and phone. */
@Composable
internal fun NebulaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    // Increased cloud density to fill 80%+ screen space in 4K
    val count = qualityCount(quality, low = 12, medium = 18, high = 26)
    val clouds = remember(count) {
        List(count) { i ->
            NebulaCloud(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                speedX = seededRange(i * 41 + 11, 0.015f, 0.05f),
                speedY = seededRange(i * 53 + 13, 0.01f, 0.04f),
                radiusFrac = seededRange(i * 67 + 19, 0.28f, 0.55f),
                pulseFreq = seededRange(i * 79 + 23, 0.15f, 0.45f),
                pulsePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 97 + 31, 0.12f, 0.32f),
                colorIndex = i,
            )
        }
    }
    // High-density multi-tiered starfield
    val starCount = qualityCount(quality, low = 80, medium = 140, high = 200)
    val stars = remember(starCount) {
        List(starCount) { j ->
            StarParticle(
                x = seededUnit(j * 11 + 5),
                y = seededUnit(j * 23 + 9),
                alpha = seededRange(j * 37 + 13, 0.15f, 0.85f),
                size = seededRange(j * 43 + 17, 0.8f, 2.5f),
                isPink = j % 4 == 0,
                isCyan = j % 3 == 0,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "nebula")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nebula_t",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Deep cosmic base background
            drawRect(color = Color(0xFF03020A))

            // Draw starfield
            stars.forEach { star ->
                val starColor = when {
                    star.isPink -> Color(0xFFF472B6)
                    star.isCyan -> Color(0xFF38BDF8)
                    else -> Color.White
                }
                drawCircle(
                    color = starColor.copy(alpha = star.alpha * brightness.coerceIn(0.5f, 1.5f)),
                    radius = star.size,
                    center = Offset(star.x * w, star.y * h),
                )
            }
        }
        // Real gaussian blur on the gas clouds only — keeps the star pinpoints crisp above.
        Canvas(modifier = Modifier.fillMaxSize().blur(24.dp)) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.60f
            clouds.forEach { cloud ->
                val x = phase01(cloud.x0 + time * cloud.speedX) * w
                val y = phase01(cloud.y0 + time * cloud.speedY) * h
                // Seamlessly-looping fbm instead of a pure sine — organic, non-repeating pulse.
                val pulse = 0.85f + 0.15f * (loopedFbm(time, radius = 1.2f + cloud.pulseFreq, seedOffset = cloud.colorIndex * 619 + 71) * 2f - 1f)
                val radius = cloud.radiusFrac * minDim * pulse

                // Blend cosmic blue/pink with palette colors
                val cosmicTone = CosmicBluePinkColors[cloud.colorIndex % CosmicBluePinkColors.size]
                val paletteTone = TonalPalette.pick(paletteColors, cloud.colorIndex)
                val base = if (cloud.colorIndex % 3 == 0) paletteTone else cosmicTone
                val tint = TonalPalette.brightness(base, brightness)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, cloud.alphaBase * dim),
                            TonalPalette.withAlpha(tint, cloud.alphaBase * 0.40f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private data class NebulaCloud(
    val x0: Float,
    val y0: Float,
    val speedX: Float,
    val speedY: Float,
    val radiusFrac: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)

private data class StarParticle(
    val x: Float,
    val y: Float,
    val alpha: Float,
    val size: Float,
    val isPink: Boolean,
    val isCyan: Boolean,
)
