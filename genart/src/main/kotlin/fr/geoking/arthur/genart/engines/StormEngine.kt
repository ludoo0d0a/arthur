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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Soft storm: blurred cumulonimbus silhouettes, rain intensity that "breathes" in slow waves,
 * and a diffuse glow pulse standing in for lightning — never a hard flash (car-safe).
 */
@Composable
internal fun StormEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val clouds = remember(cloudCount) {
        List(cloudCount) { i ->
            StormCloudSeed(
                x0 = seededUnit(i * 17 + 3),
                yFrac = seededRange(i * 29 + 7, 0.05f, 0.4f),
                speedMul = seededRange(i * 41 + 11, 0.03f, 0.09f),
                scale = seededRange(i * 53 + 13, 0.7f, 1.3f),
                colorIndex = i,
            )
        }
    }
    val dropCount = qualityCount(quality, low = 46, medium = 76, high = 116)
    val drops = remember(dropCount) {
        List(dropCount) { i ->
            StormDropSeed(
                x0 = seededUnit(i * 61 + 5),
                y0 = seededUnit(i * 73 + 9),
                fallSpeed = seededRange(i * 83 + 13, 0.7f, 1.6f),
                lengthFrac = seededRange(i * 97 + 17, 0.02f, 0.06f),
                thickness = seededRange(i * 103 + 19, 1f, 2.2f),
                alphaBase = seededRange(i * 107 + 23, 0.2f, 0.6f),
                densityGate = seededUnit(i * 113 + 29),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "storm")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "storm_drift",
    )
    val breathT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "storm_breath",
    )
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "storm_glow",
    )
    val gustT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((13000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "storm_gust",
    )

    val dim = if (isActive) 1f else 0.55f
    // Rain "breathes": intensity slowly rises and falls, never fully off.
    val intensity = 0.5f + 0.5f * sin01(phase01(breathT) * 2f * PI.toFloat())
    // Distant heat-glow: slow fade in/out, capped well below a flash, no hard edges (blurred layer).
    val glowPulse = sin01(phase01(glowT) * 2f * PI.toFloat())
    // Shared wind-gust scalar drives every rain streak's slant together, not per-drop independent phase.
    val gust = 0.08f + 0.22f * sin01(phase01(gustT) * 2f * PI.toFloat())

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF11151D), Color(0xFF04070B)),
                ),
            )
        }
        // Blurred layer: distant glow + cumulonimbus silhouettes. Real gaussian blur (no-ops below API 31).
        Canvas(modifier = Modifier.fillMaxSize().blur(30.dp)) {
            val w = size.width
            val h = size.height
            val glowAlpha = (0.08f + glowPulse * 0.22f) * dim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color(0xFFAEB9CC), glowAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.5f, h * 0.22f),
                    radius = w.coerceAtLeast(h) * 0.55f,
                ),
                radius = w.coerceAtLeast(h) * 0.55f,
                center = Offset(w * 0.5f, h * 0.22f),
            )
            val drift = phase01(driftT)
            clouds.forEach { cloud ->
                val x = phase01(cloud.x0 + drift * cloud.speedMul) * w
                val y = cloud.yFrac * h
                val base = TonalPalette.brightness(TonalPalette.pick(paletteColors, cloud.colorIndex), brightness * 0.35f)
                val radius = cloud.scale * w.coerceAtMost(h) * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color(0xFF1B2028), 0.85f * dim),
                            TonalPalette.withAlpha(base, 0.35f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
        // Crisp layer: rain streaks on top, kept sharp, density modulated by the breathing intensity.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val time = phase01(driftT)
            drops.forEach { drop ->
                if (drop.densityGate > intensity) return@forEach
                val fall = phase01(drop.y0 + drop.fallSpeed * time)
                val y = fall * h
                val x = phase01(drop.x0 + gust * time * 0.5f) * w
                val len = drop.lengthFrac * h
                val start = Offset(x, y)
                val end = Offset(x + gust * len, y + len)
                val base = TonalPalette.mix(Color(0xFFAEBED8), TonalPalette.pick(paletteColors, drop.colorIndex), 0.2f)
                val color = TonalPalette.brightness(base, brightness)
                drawLine(
                    color = TonalPalette.withAlpha(color, drop.alphaBase * dim),
                    start = start,
                    end = end,
                    strokeWidth = drop.thickness,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private data class StormCloudSeed(
    val x0: Float,
    val yFrac: Float,
    val speedMul: Float,
    val scale: Float,
    val colorIndex: Int,
)

private data class StormDropSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val lengthFrac: Float,
    val thickness: Float,
    val alphaBase: Float,
    val densityGate: Float,
    val colorIndex: Int,
)
