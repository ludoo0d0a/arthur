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
import kotlin.math.sin

/** Soft, slow-drifting caustic streaks over a cool underwater gradient. */
@Composable
internal fun SoftCausticsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val streakCount = qualityCount(quality, low = 8, medium = 14, high = 22)
    val streaks = remember(streakCount) {
        List(streakCount) { i ->
            CausticStreakSeed(
                xFrac = seededUnit(i * 17 + 3),
                yFrac = seededUnit(i * 29 + 7),
                lengthFrac = seededRange(i * 37 + 11, 0.12f, 0.28f),
                curveFrac = seededRange(i * 43 + 13, 0.03f, 0.09f),
                angle = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                thicknessFrac = seededRange(i * 61 + 19, 0.004f, 0.01f),
                driftFreq1 = seededRange(i * 71 + 23, 0.4f, 0.9f),
                driftFreq2 = seededRange(i * 79 + 29, 0.9f, 1.7f),
                driftFreq3 = seededRange(i * 89 + 31, 1.7f, 2.6f),
                driftPhase1 = seededRange(i * 97 + 37, 0f, 2f * PI.toFloat()),
                driftPhase2 = seededRange(i * 103 + 41, 0f, 2f * PI.toFloat()),
                driftPhase3 = seededRange(i * 109 + 43, 0f, 2f * PI.toFloat()),
                shimmerFreq = seededRange(i * 113 + 47, 0.3f, 0.8f),
                shimmerPhase = seededRange(i * 127 + 53, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "soft_caustics")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "soft_caustics_t",
    )

    val activeFactor = if (isActive) 1f else 0.45f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val deepTop = TonalPalette.brightness(Color(0xFF04263A), brightness)
            val deepBottom = TonalPalette.brightness(Color(0xFF01121F), brightness)
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(deepTop, deepBottom)),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(6.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val timeAngle = phase01(t) * 2f * PI.toFloat()

            streaks.forEach { streak ->
                val driftX = sin(timeAngle * streak.driftFreq1 + streak.driftPhase1) * 0.02f +
                    sin(timeAngle * streak.driftFreq2 + streak.driftPhase2) * 0.012f +
                    sin(timeAngle * streak.driftFreq3 + streak.driftPhase3) * 0.006f
                val driftY = sin(timeAngle * streak.driftFreq2 + streak.driftPhase3) * 0.018f +
                    sin(timeAngle * streak.driftFreq3 + streak.driftPhase1) * 0.01f
                val cx = (phase01(streak.xFrac + driftX * activeFactor)) * w
                val cy = (phase01(streak.yFrac + driftY * activeFactor)) * h

                val len = streak.lengthFrac * minDim
                val curve = streak.curveFrac * minDim
                val dirX = sin(streak.angle)
                val dirY = sin(streak.angle + PI.toFloat() / 2f)
                val normX = -dirY
                val normY = dirX

                val startX = cx - dirX * len / 2f
                val startY = cy - dirY * len / 2f
                val endX = cx + dirX * len / 2f
                val endY = cy + dirY * len / 2f
                val ctrlX = cx + normX * curve
                val ctrlY = cy + normY * curve

                val path = Path().apply {
                    moveTo(startX, startY)
                    quadraticTo(ctrlX, ctrlY, endX, endY)
                }

                val shimmer = sin01(timeAngle * streak.shimmerFreq + streak.shimmerPhase)
                val baseColor = TonalPalette.mix(
                    Color(0xFF7FE9FF),
                    TonalPalette.pick(paletteColors, streak.colorIndex),
                    0.25f,
                )
                val glowColor = TonalPalette.brightness(baseColor, brightness)
                val alpha = (0.18f + 0.22f * shimmer) * activeFactor

                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(glowColor, alpha),
                    style = Stroke(width = streak.thicknessFrac * minDim),
                )
            }
        }
    }
}

private data class CausticStreakSeed(
    val xFrac: Float,
    val yFrac: Float,
    val lengthFrac: Float,
    val curveFrac: Float,
    val angle: Float,
    val thicknessFrac: Float,
    val driftFreq1: Float,
    val driftFreq2: Float,
    val driftFreq3: Float,
    val driftPhase1: Float,
    val driftPhase2: Float,
    val driftPhase3: Float,
    val shimmerFreq: Float,
    val shimmerPhase: Float,
    val colorIndex: Int,
)
