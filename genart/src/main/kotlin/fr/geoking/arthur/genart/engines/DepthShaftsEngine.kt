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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dark underwater column with god rays from above, soft caustic streaks, and
 * drifting plankton motes — more cinematic than Sunbeams alone.
 */
@Composable
internal fun DepthShaftsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val shaftCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val streakCount = qualityCount(quality, low = 6, medium = 10, high = 16)
    val moteCount = qualityCount(quality, low = 30, medium = 55, high = 85)

    val shafts = remember(shaftCount) {
        List(shaftCount) { i ->
            DepthShaftBeamSeed(
                angle = seededRange(i * 17 + 3, -0.45f, 0.45f),
                width = seededRange(i * 29 + 7, 0.035f, 0.09f),
                swayAmp = seededRange(i * 41 + 11, 0.015f, 0.045f),
                swayFreq = seededRange(i * 53 + 13, 0.12f, 0.4f),
                alphaBase = seededRange(i * 67 + 19, 0.07f, 0.2f),
                colorIndex = i,
            )
        }
    }
    val streaks = remember(streakCount) {
        List(streakCount) { i ->
            DepthShaftStreakSeed(
                xFrac = seededUnit(i * 17 + 3),
                yFrac = seededRange(i * 29 + 7, 0.25f, 0.85f),
                lengthFrac = seededRange(i * 37 + 11, 0.1f, 0.24f),
                curveFrac = seededRange(i * 43 + 13, 0.025f, 0.08f),
                angle = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                thicknessFrac = seededRange(i * 61 + 19, 0.0035f, 0.009f),
                driftFreq1 = seededRange(i * 71 + 23, 0.35f, 0.85f),
                driftFreq2 = seededRange(i * 79 + 29, 0.85f, 1.6f),
                driftPhase1 = seededRange(i * 97 + 37, 0f, 2f * PI.toFloat()),
                driftPhase2 = seededRange(i * 103 + 41, 0f, 2f * PI.toFloat()),
                shimmerFreq = seededRange(i * 113 + 47, 0.25f, 0.7f),
                shimmerPhase = seededRange(i * 127 + 53, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val motes = remember(moteCount) {
        List(moteCount) { j ->
            DepthShaftMoteSeed(
                x0 = seededUnit(j * 13 + 5),
                y0 = seededUnit(j * 19 + 7),
                sizeFrac = seededRange(j * 31 + 13, 0.0012f, 0.0045f),
                driftX = seededRange(j * 37 + 17, -0.04f, 0.04f),
                driftY = seededRange(j * 41 + 19, -0.06f, -0.01f),
                twinklePhase = seededRange(j * 43 + 23, 0f, 2f * PI.toFloat()),
                twinkleFreq = seededRange(j * 47 + 29, 0.5f, 1.8f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "depth_shafts")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "depth_shafts_t",
    )

    val dim = if (isActive) 1f else 0.5f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val deepTop = TonalPalette.brightness(Color(0xFF04263A), brightness)
            val deepBottom = TonalPalette.brightness(Color(0xFF01121F), brightness)
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(deepTop, deepBottom)),
            )

            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t) * 2f * PI.toFloat()
            val origin = Offset(w * 0.5f, -h * 0.04f)
            val reach = h * 1.4f

            shafts.forEach { beam ->
                val sway = beam.swayAmp * sin(time * beam.swayFreq)
                val a = beam.angle + sway
                val left = Offset(
                    origin.x + sin(a - beam.width) * reach,
                    origin.y + cos(a - beam.width) * reach,
                )
                val right = Offset(
                    origin.x + sin(a + beam.width) * reach,
                    origin.y + cos(a + beam.width) * reach,
                )
                val path = Path().apply {
                    moveTo(origin.x, origin.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                }
                val base = TonalPalette.mix(
                    Color(0xFF9EE8FF),
                    TonalPalette.pick(paletteColors, beam.colorIndex),
                    0.28f,
                )
                val tint = TonalPalette.brightness(base, brightness)
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, beam.alphaBase * dim),
                            TonalPalette.withAlpha(tint, beam.alphaBase * 0.35f * dim),
                            Color.Transparent,
                        ),
                        start = origin,
                        end = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f),
                    ),
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color(0xFFC8F0FF), 0.3f * dim * brightness.coerceAtMost(1.2f)),
                        Color.Transparent,
                    ),
                    center = origin,
                    radius = minDim * 0.32f,
                ),
                radius = minDim * 0.32f,
                center = origin,
            )

            motes.forEach { mote ->
                val x = phase01(mote.x0 + t * mote.driftX * dim) * w
                val y = phase01(mote.y0 + t * mote.driftY * dim) * h
                val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(time * mote.twinkleFreq + mote.twinklePhase))
                val r = mote.sizeFrac * minDim * (0.7f + 0.5f * twinkle)
                val depthFade = 0.35f + 0.65f * (1f - y / h)
                drawCircle(
                    color = TonalPalette.withAlpha(
                        Color(0xFFB8E8F8),
                        0.45f * twinkle * depthFade * dim,
                    ),
                    radius = r.coerceAtLeast(0.5f),
                    center = Offset(x, y),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(5.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val timeAngle = phase01(t) * 2f * PI.toFloat()

            streaks.forEach { streak ->
                val driftX = sin(timeAngle * streak.driftFreq1 + streak.driftPhase1) * 0.018f +
                    sin(timeAngle * streak.driftFreq2 + streak.driftPhase2) * 0.01f
                val driftY = sin(timeAngle * streak.driftFreq2 + streak.driftPhase1) * 0.014f
                val cx = phase01(streak.xFrac + driftX * dim) * w
                val cy = phase01(streak.yFrac + driftY * dim) * h
                val len = streak.lengthFrac * minDim
                val curve = streak.curveFrac * minDim
                val dirX = sin(streak.angle)
                val dirY = sin(streak.angle + PI.toFloat() / 2f)
                val normX = -dirY
                val normY = dirX
                val path = Path().apply {
                    moveTo(cx - dirX * len / 2f, cy - dirY * len / 2f)
                    quadraticTo(
                        cx + normX * curve,
                        cy + normY * curve,
                        cx + dirX * len / 2f,
                        cy + dirY * len / 2f,
                    )
                }
                val shimmer = sin01(timeAngle * streak.shimmerFreq + streak.shimmerPhase)
                val baseColor = TonalPalette.mix(
                    Color(0xFF7FE9FF),
                    TonalPalette.pick(paletteColors, streak.colorIndex),
                    0.25f,
                )
                val glowColor = TonalPalette.brightness(baseColor, brightness)
                val alpha = (0.14f + 0.2f * shimmer) * dim
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(glowColor, alpha),
                    style = Stroke(width = streak.thicknessFrac * minDim),
                )
            }
        }
    }
}

private data class DepthShaftBeamSeed(
    val angle: Float,
    val width: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)

private data class DepthShaftStreakSeed(
    val xFrac: Float,
    val yFrac: Float,
    val lengthFrac: Float,
    val curveFrac: Float,
    val angle: Float,
    val thicknessFrac: Float,
    val driftFreq1: Float,
    val driftFreq2: Float,
    val driftPhase1: Float,
    val driftPhase2: Float,
    val shimmerFreq: Float,
    val shimmerPhase: Float,
    val colorIndex: Int,
)

private data class DepthShaftMoteSeed(
    val x0: Float,
    val y0: Float,
    val sizeFrac: Float,
    val driftX: Float,
    val driftY: Float,
    val twinklePhase: Float,
    val twinkleFreq: Float,
)
