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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * N-fold rotational mandala of Archimedean / log-spiral arms with breathing stroke taper
 * and dual phase offset — dense jewel geometry, analytic only.
 */
@Composable
internal fun SpiralMandalaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val foldCount = remember { 5 + (seededUnit(41) * 4f).toInt().coerceIn(0, 3) } // 5–8
    val segmentCount = qualityCount(quality, low = 80, medium = 140, high = 220)
    val dualOffset = remember { seededRange(53, 0.12f, 0.28f) }
    val spiralB = remember { seededRange(67, 0.18f, 0.32f) }

    val arms = remember(foldCount) {
        List(foldCount) { i ->
            SpiralMandalaArmSeed(
                foldIndex = i,
                colorIndex = i,
                widthScale = seededRange(i * 19 + 3, 0.7f, 1.2f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "spiral_mandala")
    val rotT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spiral_mandala_rot",
    )
    val breathT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spiral_mandala_breath",
    )

    val dim = if (isActive) 1f else 0.55f
    val rot = phase01(rotT) * 360f
    val breath = 0.82f + 0.18f * loopedFbm(phase01(breathT), radius = 1.4f, seedOffset = 11)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF06040F))
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val glow = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFC9A24A), TonalPalette.pick(paletteColors, 0), 0.35f),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(glow, 0.42f * dim * breath),
                        TonalPalette.withAlpha(glow, 0.12f * dim * breath),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = minDim * 0.38f,
                ),
                radius = minDim * 0.38f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val maxR = minDim * 0.46f
            val foldAngle = 2f * PI.toFloat() / foldCount

            rotate(degrees = rot, pivot = Offset(cx, cy)) {
                arms.forEach { arm ->
                    val baseAngle = arm.foldIndex * foldAngle
                    val ink = TonalPalette.brightness(
                        TonalPalette.mix(
                            TonalPalette.pick(paletteColors, arm.colorIndex),
                            Color(0xFFE8D5A3),
                            0.25f,
                        ),
                        brightness,
                    )
                    // Primary spiral + dual phase twin
                    for (phase in 0..1) {
                        val phaseShift = if (phase == 0) 0f else dualOffset * 2f * PI.toFloat()
                        val alpha = (if (phase == 0) 0.72f else 0.38f) * dim
                        var prevX = 0f
                        var prevY = 0f
                        for (s in 0 until segmentCount) {
                            val u = s / (segmentCount - 1).toFloat()
                            val theta = u * u * 5.2f * PI.toFloat()
                            val r = maxR * (1f - exp(-spiralB * theta))
                            val a = baseAngle + theta + phaseShift
                            val x = cx + cos(a) * r
                            val y = cy + sin(a) * r
                            if (s > 0) {
                                val taper = (1f - u * 0.55f).coerceIn(0.35f, 1f)
                                val strokeW = (1.4f + 3.8f * arm.widthScale * breath * taper) *
                                    (minDim / 420f).coerceIn(0.6f, 1.8f)
                                drawLine(
                                    color = TonalPalette.withAlpha(ink, alpha),
                                    start = Offset(prevX, prevY),
                                    end = Offset(x, y),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round,
                                )
                            }
                            prevX = x
                            prevY = y
                        }
                    }
                }

                // Inner jewel ring
                val ring = TonalPalette.withAlpha(
                    TonalPalette.brightness(Color(0xFFF0E0B0), brightness),
                    0.35f * dim * breath,
                )
                drawCircle(
                    color = ring,
                    radius = maxR * 0.06f,
                    center = Offset(cx, cy),
                    style = Stroke(width = maxR * 0.012f),
                )
            }
        }
    }
}

private data class SpiralMandalaArmSeed(
    val foldIndex: Int,
    val colorIndex: Int,
    val widthScale: Float,
)
