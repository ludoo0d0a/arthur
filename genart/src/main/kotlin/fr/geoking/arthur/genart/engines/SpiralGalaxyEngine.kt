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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val ARM_COUNT = 3
private const val MAX_THETA = 4f * PI.toFloat()
private const val SPIRAL_B = 0.3f
private const val MAX_RADIUS_FRAC = 0.46f

/** A galaxy of stars scattered along logarithmic spiral arms, drifting in a slow whole-field rotation. */
@Composable
internal fun SpiralGalaxyEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val starCount = qualityCount(quality, low = 60, medium = 110, high = 170)
    val stars = remember(starCount) {
        List(starCount) { i ->
            val seed = i * 101 + 7
            val armIndex = i % ARM_COUNT
            val u = seededUnit(seed + 3)
            val theta = u * u * MAX_THETA
            SpiralStar(
                armOffset = armIndex * (2f * PI.toFloat() / ARM_COUNT),
                theta = theta,
                radialJitter = seededRange(seed + 13, -0.025f, 0.025f),
                angleJitter = seededRange(seed + 19, -0.06f, 0.06f),
                sizeUnit = seededUnit(seed + 29),
                alphaUnit = seededRange(seed + 37, 0.35f, 1f),
                colorIndex = i,
            )
        }
    }

    val effectiveSpeed = (if (isActive) speed else speed * 0.5f).coerceAtLeast(0.15f)

    val transition = rememberInfiniteTransition(label = "spiralGalaxy")
    val rotationT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((240000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spiralGalaxy_rotation",
    )
    val pulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spiralGalaxy_pulse",
    )

    val dim = if (isActive) 1f else 0.6f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF03020A))
            val cx = w / 2f
            val cy = h / 2f
            val minDim = minOf(w, h)
            val fieldRotationDeg = phase01(rotationT) * 360f
            rotate(degrees = fieldRotationDeg, pivot = Offset(cx, cy)) {
                stars.forEach { star ->
                    val angle = star.theta + star.armOffset + star.angleJitter
                    val rNorm = exp(SPIRAL_B * star.theta) / exp(SPIRAL_B * MAX_THETA)
                    val r = (rNorm + star.radialJitter).coerceAtLeast(0f) * MAX_RADIUS_FRAC * minDim
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    val radius = 0.6f + star.sizeUnit * 1.6f
                    val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, star.colorIndex), 0.2f)
                    val tint = TonalPalette.brightness(base, brightness)
                    val alpha = (star.alphaUnit * dim).coerceIn(0f, 1f)
                    drawCircle(
                        color = TonalPalette.withAlpha(tint, alpha),
                        radius = radius,
                        center = Offset(x, y),
                    )
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val pulse = 0.9f + 0.1f * sin(phase01(pulseT) * 2f * PI.toFloat())
            val coreRadius = minDim * 0.3f * pulse
            val core = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.5f * dim),
                        TonalPalette.withAlpha(core, 0.35f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = coreRadius,
                ),
                radius = coreRadius,
                center = Offset(w / 2f, h / 2f),
            )
        }
    }
}

private data class SpiralStar(
    val armOffset: Float,
    val theta: Float,
    val radialJitter: Float,
    val angleJitter: Float,
    val sizeUnit: Float,
    val alphaUnit: Float,
    val colorIndex: Int,
)
