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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/**
 * Three depth layers of point-stars (far = many/small/sharp/slow, near = few/large/soft-blurred/
 * fast), each star twinkling on its own independent phase, plus one slow straight-line
 * "satellite" crossing the frame — a parallax star field rather than a single flat scatter.
 */
@Composable
internal fun StarFieldEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val farCount = qualityCount(quality, low = 46, medium = 74, high = 110)
    val midCount = qualityCount(quality, low = 16, medium = 26, high = 40)
    val nearCount = qualityCount(quality, low = 6, medium = 9, high = 14)

    val farStars = remember(farCount) { starLayer(farCount, seedBase = 1) }
    val midStars = remember(midCount) { starLayer(midCount, seedBase = 2) }
    val nearStars = remember(nearCount) { starLayer(nearCount, seedBase = 3) }
    val satelliteYFrac = remember { seededRange(9001, 0.08f, 0.5f) }

    val transition = rememberInfiniteTransition(label = "starfield")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((110000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starfield_drift",
    )
    val twinkleT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starfield_twinkle",
    )
    val satelliteT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starfield_satellite",
    )

    val dim = if (isActive) 1f else 0.6f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF02030A), Color(0xFF000000)),
                ),
            )
        }
        // Far layer: crisp, slow drift, many small dim stars.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStarLayer(farStars, driftT, twinkleT, driftScale = 0.015f, sizeScale = 1f, alphaScale = 1f, paletteColors, brightness, dim)
        }
        // Mid layer: light blur, medium drift, medium size.
        Canvas(modifier = Modifier.fillMaxSize().blur(3.dp)) {
            drawStarLayer(midStars, driftT, twinkleT, driftScale = 0.045f, sizeScale = 1.7f, alphaScale = 1.15f, paletteColors, brightness, dim)
        }
        // Near layer: soft bokeh blur, fastest drift, largest/brightest — shallow depth-of-field look.
        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp)) {
            drawStarLayer(nearStars, driftT, twinkleT, driftScale = 0.09f, sizeScale = 2.6f, alphaScale = 1.3f, paletteColors, brightness, dim)
        }
        // One slow satellite point crossing the frame in a straight line, crisp, fading at both ends.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = phase01(satelliteT)
            val x = t * w
            val y = satelliteYFrac * h + (t - 0.5f) * 0.06f * h
            val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val alpha = 0.55f * dim * edgeFade
            if (alpha > 0.01f) {
                val trailLen = w * 0.02f
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(x - trailLen, y),
                    end = Offset(x, y),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = Color.White.copy(alpha = alpha), radius = 1.6f, center = Offset(x, y))
            }
        }
    }
}

private fun starLayer(count: Int, seedBase: Int): List<StarSeed> = List(count) { i ->
    val s = i * 97 + seedBase * 100_003
    StarSeed(
        xFrac = seededUnit(s + 11),
        yFrac = seededUnit(s + 23),
        sizeUnit = seededUnit(s + 37),
        alphaUnit = seededRange(s + 47, 0.35f, 1f),
        twinkleFreq = seededRange(s + 59, 0.5f, 1.6f),
        twinklePhase = seededRange(s + 71, 0f, 2f * PI.toFloat()),
        colorIndex = i,
    )
}

private fun DrawScope.drawStarLayer(
    stars: List<StarSeed>,
    driftT: Float,
    twinkleT: Float,
    driftScale: Float,
    sizeScale: Float,
    alphaScale: Float,
    paletteColors: List<Color>,
    brightness: Float,
    dim: Float,
) {
    val w = size.width
    val h = size.height
    val drift = phase01(driftT)
    val twinkleAngle = phase01(twinkleT) * 2f * PI.toFloat()
    stars.forEach { star ->
        val x = phase01(star.xFrac + drift * driftScale) * w
        val y = star.yFrac * h
        val twinkle = 0.5f + 0.5f * sin01(twinkleAngle * star.twinkleFreq + star.twinklePhase)
        val radius = (0.7f + star.sizeUnit * 1.6f) * sizeScale
        val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, star.colorIndex), 0.18f)
        val tint = TonalPalette.brightness(base, brightness)
        val alpha = (star.alphaUnit * (0.4f + 0.6f * twinkle) * alphaScale * dim).coerceIn(0f, 1f)
        drawCircle(
            color = TonalPalette.withAlpha(tint, alpha),
            radius = radius,
            center = Offset(x, y),
        )
    }
}

private data class StarSeed(
    val xFrac: Float,
    val yFrac: Float,
    val sizeUnit: Float,
    val alphaUnit: Float,
    val twinkleFreq: Float,
    val twinklePhase: Float,
    val colorIndex: Int,
)
