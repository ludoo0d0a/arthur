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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft night-sky fireworks — staggered radial bursts that bloom and fade gently.
 * No hard flashes; peak alpha stays car-safe.
 */
@Composable
internal fun FireworksEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val burstCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val sparkPerBurst = qualityCount(quality, low = 10, medium = 14, high = 18)
    val bursts = remember(burstCount) {
        List(burstCount) { i ->
            FireworksBurstSeed(
                cx = seededRange(i * 17 + 3, 0.18f, 0.82f),
                cy = seededRange(i * 29 + 7, 0.12f, 0.55f),
                phaseOffset = seededUnit(i * 41 + 11),
                duration = seededRange(i * 53 + 13, 0.18f, 0.32f),
                maxRadiusFrac = seededRange(i * 67 + 19, 0.12f, 0.28f),
                colorIndex = i,
            )
        }
    }
    val sparks = remember(burstCount, sparkPerBurst) {
        List(burstCount) { b ->
            List(sparkPerBurst) { s ->
                val seed = b * 901 + s * 47 + 2001
                FireworksSparkSeed(
                    angle = seededRange(seed, 0f, 2f * PI.toFloat()),
                    lengthMul = seededRange(seed + 11, 0.55f, 1.15f),
                    thickness = seededRange(seed + 23, 1.2f, 2.6f),
                )
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "fireworks")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fireworks_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF050510), Color(0xFF020208), Color(0xFF010104)),
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        bursts.forEachIndexed { bi, burst ->
            val local = phase01(time + burst.phaseOffset)
            if (local > burst.duration) return@forEachIndexed
            val life = local / burst.duration
            val expand = (life / 0.35f).coerceIn(0f, 1f)
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.12f).coerceIn(0f, 1f)
            val cx = burst.cx * w
            val cy = burst.cy * h
            val radius = burst.maxRadiusFrac * minDim * expand
            val tint = TonalPalette.brightness(TonalPalette.pick(paletteColors, burst.colorIndex), brightness)
            val core = TonalPalette.mix(tint, Color.White, 0.35f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(core, 0.45f * fade * dim),
                        TonalPalette.withAlpha(tint, 0.18f * fade * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = (radius * 0.55f).coerceAtLeast(1f),
                ),
                radius = (radius * 0.55f).coerceAtLeast(1f),
                center = Offset(cx, cy),
            )

            sparks[bi].forEach { spark ->
                val len = radius * spark.lengthMul
                val start = Offset(cx, cy)
                val end = Offset(cx + cos(spark.angle) * len, cy + sin(spark.angle) * len)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(core, 0.7f * fade * dim),
                            TonalPalette.withAlpha(tint, 0.15f * fade * dim),
                            Color.Transparent,
                        ),
                        start = start,
                        end = end,
                    ),
                    start = start,
                    end = end,
                    strokeWidth = spark.thickness,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private data class FireworksBurstSeed(
    val cx: Float,
    val cy: Float,
    val phaseOffset: Float,
    val duration: Float,
    val maxRadiusFrac: Float,
    val colorIndex: Int,
)

private data class FireworksSparkSeed(
    val angle: Float,
    val lengthMul: Float,
    val thickness: Float,
)
