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

@Composable
internal fun CloudsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val puffBudget = qualityCount(quality, low = 4, medium = 5, high = 7)

    val clouds = remember(cloudCount, puffBudget) {
        List(cloudCount) { i ->
            val puffCount = (puffBudget - 1 + (seededUnit(i * 101 + 3) * 2f).toInt()).coerceIn(3, 8)
            CloudSeed(
                x0 = seededUnit(i * 17 + 3),
                yFrac = seededRange(i * 29 + 7, 0.12f, 0.58f),
                speedMul = seededRange(i * 41 + 11, 0.08f, 0.22f),
                bobAmpFrac = seededRange(i * 53 + 13, 0.004f, 0.018f),
                bobFreq = seededRange(i * 67 + 19, 0.3f, 1.1f),
                bobPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                scale = seededRange(i * 89 + 29, 0.55f, 1.15f),
                colorIndex = i,
                puffs = List(puffCount) { p ->
                    PuffSeed(
                        dx = seededRange(i * 200 + p * 13 + 5, -0.55f, 0.55f),
                        dy = seededRange(i * 200 + p * 17 + 7, -0.28f, 0.22f),
                        radiusFrac = seededRange(i * 200 + p * 19 + 11, 0.22f, 0.48f),
                    )
                },
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "clouds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((50000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "clouds_t",
    )

    val driftScale = if (isActive) 1f else 0.35f
    val alphaScale = if (isActive) 1f else 0.65f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.45f)
            val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.28f)
            val skyBot = Color(0xFF0A1018)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop, skyMid, skyBot),
                ),
            )
        }
        // Real gaussian blur on the puffs only (no-ops below API 31, keeping the prior look there).
        Canvas(modifier = Modifier.fillMaxSize().blur(16.dp)) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            val minDim = w.coerceAtMost(h)

            clouds.forEach { cloud ->
                val x = phase01(cloud.x0 + time * cloud.speedMul * driftScale) * w
                // Seamlessly-looping fbm instead of a pure sine — organic, non-repeating bob.
                val bobNoise = loopedFbm(time, radius = 1.2f + cloud.bobFreq, seedOffset = cloud.colorIndex * 977 + 13) * 2f - 1f
                val bob = bobNoise * cloud.bobAmpFrac * h * driftScale
                val y = cloud.yFrac * h + bob
                val base = TonalPalette.pick(paletteColors, cloud.colorIndex)
                val tint = TonalPalette.brightness(base, brightness * 0.85f)
                val cloudWhite = Color(0xFFE8EEF5)

                cloud.puffs.forEach { puff ->
                    val cx = x + puff.dx * cloud.scale * minDim * 0.55f
                    val cy = y + puff.dy * cloud.scale * minDim * 0.35f
                    val radius = puff.radiusFrac * cloud.scale * minDim * 0.22f
                    val haloAlpha = 0.10f * alphaScale * brightness.coerceAtMost(1.1f)
                    val coreAlpha = 0.22f * alphaScale * brightness.coerceAtMost(1.1f)

                    drawCircle(
                        color = TonalPalette.withAlpha(tint, haloAlpha),
                        radius = radius * 1.45f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = TonalPalette.withAlpha(cloudWhite, coreAlpha),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }
            }
        }
    }
}

private data class CloudSeed(
    val x0: Float,
    val yFrac: Float,
    val speedMul: Float,
    val bobAmpFrac: Float,
    val bobFreq: Float,
    val bobPhase: Float,
    val scale: Float,
    val colorIndex: Int,
    val puffs: List<PuffSeed>,
)

private data class PuffSeed(
    val dx: Float,
    val dy: Float,
    val radiusFrac: Float,
)
