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
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Condensation droplets on glass over a softly blurred backdrop.
 * Each composition rolls a rain intensity (light → heavy) that scales bead count,
 * how often drops run, and how far the rivulets travel — calm, car-safe.
 */
@Composable
internal fun RainOnGlassEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    // 0 = light drizzle on glass, 1 = heavy sheet of running drops.
    val intensity = remember { Random.nextFloat() }
    val baseCount = qualityCount(quality, low = 6, medium = 10, high = 16)
    val count = (baseCount * lerp(0.4f, 1.85f, intensity)).toInt().coerceAtLeast(3)
    val streakCount = (qualityCount(quality, low = 4, medium = 8, high = 14) * intensity)
        .toInt()
        .coerceAtLeast(if (intensity > 0.55f) 2 else 0)

    val droplets = remember(count, intensity) {
        List(count) { i ->
            RainOnGlassDropletSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                baseRadiusFrac = seededRange(
                    i * 41 + 11,
                    lerp(0.022f, 0.014f, intensity),
                    lerp(0.04f, 0.038f, intensity),
                ),
                growAmpFrac = seededRange(i * 53 + 13, 0.006f, 0.018f),
                growFreq = seededRange(i * 67 + 19, 0.05f, 0.16f),
                growPhase = seededUnit(i * 71 + 41),
                runFreq = seededRange(
                    i * 79 + 23,
                    lerp(0.02f, 0.06f, intensity),
                    lerp(0.06f, 0.16f, intensity),
                ),
                runPhase = seededUnit(i * 83 + 47),
                runDistFrac = seededRange(
                    i * 89 + 29,
                    lerp(0.03f, 0.1f, intensity),
                    lerp(0.08f, 0.28f, intensity),
                ),
                alphaBase = seededRange(i * 97 + 31, 0.32f, 0.6f),
                highlightAngle = seededRange(i * 101 + 53, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val streaks = remember(streakCount, intensity) {
        List(streakCount) { i ->
            RainOnGlassStreakSeed(
                x0 = seededUnit(i * 23 + 5),
                y0 = seededUnit(i * 37 + 11),
                fallSpeed = seededRange(i * 47 + 13, lerp(0.35f, 0.7f, intensity), lerp(0.7f, 1.4f, intensity)),
                lengthFrac = seededRange(i * 59 + 17, 0.04f, 0.12f),
                thickness = seededRange(i * 73 + 19, 0.8f, 2.2f),
                alphaBase = seededRange(i * 89 + 29, 0.08f, 0.22f),
                colorIndex = i,
            )
        }
    }

    val cycleMs = (lerp(70000f, 38000f, intensity) / speed.coerceAtLeast(0.2f)).toInt()
    val transition = rememberInfiniteTransition(label = "rain_on_glass")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain_on_glass_t",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141C28), Color(0xFF080C12)),
                ),
            )
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.5f
            paletteColors.indices.take(3).forEach { i ->
                val cx = phase01(seededUnit(i * 13 + 5) + time * 0.03f) * w
                val cy = seededRange(i * 19 + 9, 0.2f, 0.8f) * h
                val base = TonalPalette.mix(Color(0xFF8FB4D8), TonalPalette.pick(paletteColors, i), 0.35f)
                val tint = TonalPalette.brightness(base, brightness * 0.85f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, 0.25f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.35f,
                    ),
                    radius = w * 0.35f,
                    center = Offset(cx, cy),
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.6f

            streaks.forEach { streak ->
                val fall = phase01(streak.y0 + streak.fallSpeed * time)
                val y = fall * h
                val x = streak.x0 * w
                val len = streak.lengthFrac * h
                val base = TonalPalette.mix(Color(0xFFDCEBFA), TonalPalette.pick(paletteColors, streak.colorIndex), 0.15f)
                val tint = TonalPalette.brightness(base, brightness)
                drawLine(
                    color = TonalPalette.withAlpha(tint, streak.alphaBase * dim * intensity),
                    start = Offset(x, y),
                    end = Offset(x, y + len),
                    strokeWidth = streak.thickness,
                    cap = StrokeCap.Round,
                )
            }

            droplets.forEach { drop ->
                val grow = sin01(time * 2f * PI.toFloat() * drop.growFreq + drop.growPhase * 2f * PI.toFloat())
                val radius = (drop.baseRadiusFrac + drop.growAmpFrac * grow) * w

                val runCycle = phase01(time * drop.runFreq + drop.runPhase)
                val runSlide = (sin01(runCycle * 2f * PI.toFloat()) * 2f - 1f).coerceAtLeast(0f)
                val slideDist = runSlide * drop.runDistFrac * h

                val x = drop.x0 * w
                val y = drop.y0 * h + slideDist

                if (slideDist > radius * 0.4f) {
                    val trailAlpha = drop.alphaBase * 0.35f * dim * (slideDist / (drop.runDistFrac * h))
                    val base = TonalPalette.mix(Color(0xFFDCEBFA), TonalPalette.pick(paletteColors, drop.colorIndex), 0.2f)
                    val tint = TonalPalette.brightness(base, brightness)
                    drawLine(
                        color = TonalPalette.withAlpha(tint, trailAlpha),
                        start = Offset(x, y - slideDist),
                        end = Offset(x, y - radius * 0.3f),
                        strokeWidth = radius * 0.5f,
                        cap = StrokeCap.Round,
                    )
                }

                val base = TonalPalette.mix(Color(0xFFDCEBFA), TonalPalette.pick(paletteColors, drop.colorIndex), 0.2f)
                val tint = TonalPalette.brightness(base, brightness)
                val alpha = drop.alphaBase * dim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            TonalPalette.withAlpha(tint, alpha * 0.35f),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
                val highlightOffset = Offset(
                    x + sin(drop.highlightAngle) * radius * 0.35f,
                    y - sin(drop.highlightAngle + PI.toFloat() / 2f) * radius * 0.35f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color.White, alpha * 0.8f),
                            Color.Transparent,
                        ),
                        center = highlightOffset,
                        radius = radius * 0.4f,
                    ),
                    radius = radius * 0.4f,
                    center = highlightOffset,
                )
            }
        }
    }
}

private data class RainOnGlassDropletSeed(
    val x0: Float,
    val y0: Float,
    val baseRadiusFrac: Float,
    val growAmpFrac: Float,
    val growFreq: Float,
    val growPhase: Float,
    val runFreq: Float,
    val runPhase: Float,
    val runDistFrac: Float,
    val alphaBase: Float,
    val highlightAngle: Float,
    val colorIndex: Int,
)

private data class RainOnGlassStreakSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val lengthFrac: Float,
    val thickness: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
