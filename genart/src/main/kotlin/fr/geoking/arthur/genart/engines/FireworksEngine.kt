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
 * Soft sci-fi sky bursts — staggered fibrous radial explosions in cyan/magenta/gold.
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
    val burstCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val sparkPerBurst = qualityCount(quality, low = 16, medium = 24, high = 34)
    val bursts = remember(burstCount) {
        List(burstCount) { i ->
            FireworksBurstSeed(
                cx = seededRange(i * 17 + 3, 0.16f, 0.84f),
                cy = seededRange(i * 29 + 7, 0.12f, 0.58f),
                phaseOffset = seededUnit(i * 41 + 11),
                duration = seededRange(i * 53 + 13, 0.2f, 0.36f),
                maxRadiusFrac = seededRange(i * 67 + 19, 0.14f, 0.32f),
                colorIndex = i,
                hueBias = seededUnit(i * 83 + 29),
            )
        }
    }
    val sparks = remember(burstCount, sparkPerBurst) {
        List(burstCount) { b ->
            List(sparkPerBurst) { s ->
                val seed = b * 901 + s * 47 + 2001
                FireworksSparkSeed(
                    angle = seededRange(seed, 0f, 2f * PI.toFloat()),
                    lengthMul = seededRange(seed + 11, 0.45f, 1.25f),
                    thickness = seededRange(seed + 23, 1.0f, 2.8f),
                    fiber = seededRange(seed + 31, 0f, 1f),
                )
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "fireworks")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
                colors = listOf(Color(0xFF060414), Color(0xFF03020A), Color(0xFF010104)),
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val cyan = Color(0xFF5CFFF0)
        val magenta = Color(0xFFFF3DCA)
        val gold = Color(0xFFFFC14A)
        val ice = Color(0xFFF5FBFF)

        bursts.forEachIndexed { bi, burst ->
            val local = phase01(time + burst.phaseOffset)
            if (local > burst.duration) return@forEachIndexed
            val life = local / burst.duration
            val expand = (life / 0.32f).coerceIn(0f, 1f)
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.1f).coerceIn(0f, 1f)
            val cx = burst.cx * w
            val cy = burst.cy * h
            val radius = burst.maxRadiusFrac * minDim * expand
            val accent = when {
                burst.hueBias > 0.66f -> gold
                burst.hueBias > 0.33f -> magenta
                else -> cyan
            }
            val tint = TonalPalette.brightness(
                TonalPalette.mix(accent, TonalPalette.pick(paletteColors, burst.colorIndex), 0.25f),
                brightness,
            )
            val core = TonalPalette.mix(tint, ice, 0.45f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(core, 0.55f * fade * dim),
                        TonalPalette.withAlpha(tint, 0.28f * fade * dim),
                        TonalPalette.withAlpha(accent, 0.1f * fade * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = (radius * 0.62f).coerceAtLeast(1f),
                ),
                radius = (radius * 0.62f).coerceAtLeast(1f),
                center = Offset(cx, cy),
            )

            sparks[bi].forEach { spark ->
                val bend = (spark.fiber - 0.5f) * 0.18f
                val len = radius * spark.lengthMul
                val start = Offset(cx, cy)
                val mid = Offset(
                    cx + cos(spark.angle + bend * 0.5f) * len * 0.55f,
                    cy + sin(spark.angle + bend * 0.5f) * len * 0.55f,
                )
                val end = Offset(
                    cx + cos(spark.angle + bend) * len,
                    cy + sin(spark.angle + bend) * len,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(core, 0.78f * fade * dim),
                            TonalPalette.withAlpha(tint, 0.35f * fade * dim),
                            Color.Transparent,
                        ),
                        start = start,
                        end = end,
                    ),
                    start = start,
                    end = mid,
                    strokeWidth = spark.thickness,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, 0.4f * fade * dim),
                            Color.Transparent,
                        ),
                        start = mid,
                        end = end,
                    ),
                    start = mid,
                    end = end,
                    strokeWidth = spark.thickness * 0.7f,
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
    val hueBias: Float,
)

private data class FireworksSparkSeed(
    val angle: Float,
    val lengthMul: Float,
    val thickness: Float,
    val fiber: Float,
)
