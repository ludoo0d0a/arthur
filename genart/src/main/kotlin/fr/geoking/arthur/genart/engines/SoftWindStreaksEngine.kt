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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Soft daytime wind: translucent curved streaks whose speed and curvature ride one shared gust. */
@Composable
internal fun SoftWindStreaksEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val streakCount = qualityCount(quality, low = 5, medium = 9, high = 14)
    val streaks = remember(streakCount) {
        List(streakCount) { i ->
            WindStreakSeed(
                x0 = seededUnit(i * 19 + 5),
                yFrac = seededRange(i * 31 + 7, 0.08f, 0.92f),
                lengthFrac = seededRange(i * 43 + 11, 0.14f, 0.3f),
                thicknessFrac = seededRange(i * 59 + 13, 0.006f, 0.016f),
                curveFrac = seededRange(i * 71 + 17, 0.02f, 0.06f),
                speedMul = seededRange(i * 83 + 19, 0.05f, 0.15f),
                alphaBase = seededRange(i * 97 + 23, 0.16f, 0.4f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "wind")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((34000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wind_drift",
    )
    val gustT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wind_gust",
    )

    val dim = if (isActive) 1f else 0.55f
    // Shared gust scalar: every streak speeds up and curves more together, then relaxes together.
    val gust = 0.35f + 0.65f * sin01(phase01(gustT) * 2f * PI.toFloat())

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F1F8), Color(0xFFCBDCE8)),
                ),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(6.dp)) {
            val w = size.width
            val h = size.height
            val time = phase01(driftT)
            streaks.forEach { streak ->
                val x = phase01(streak.x0 + time * streak.speedMul * gust) * w
                val y = streak.yFrac * h
                val len = streak.lengthFrac * w
                val amp = streak.curveFrac * h * gust
                val startX = x - len * 0.5f
                val endX = x + len * 0.5f
                val path = Path().apply {
                    moveTo(startX, y)
                    quadraticBezierTo(x, y - amp, endX, y)
                }
                val base = TonalPalette.mix(Color(0xFFF5F8FC), TonalPalette.pick(paletteColors, streak.colorIndex), 0.25f)
                val color = TonalPalette.brightness(base, brightness)
                val alpha = streak.alphaBase * dim
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TonalPalette.withAlpha(color, alpha),
                            TonalPalette.withAlpha(color, alpha),
                            Color.Transparent,
                        ),
                        startX = startX,
                        endX = endX,
                    ),
                    style = Stroke(
                        width = (streak.thicknessFrac * h).coerceAtLeast(1f),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
    }
}

private data class WindStreakSeed(
    val x0: Float,
    val yFrac: Float,
    val lengthFrac: Float,
    val thicknessFrac: Float,
    val curveFrac: Float,
    val speedMul: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
