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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Ochre dust settling and re-stirring in the bottom third, as if after a slow landslide — low contrast, car-safe. */
@Composable
internal fun LandslideDustEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 9)
    val clouds = remember(count) {
        List(count) { i ->
            DustCloud(
                x0 = seededUnit(i * 17 + 5),
                yFrac = seededRange(i * 29 + 11, 0.7f, 0.97f),
                speedMul = seededRange(i * 41 + 13, 0.015f, 0.05f),
                widthFrac = seededRange(i * 53 + 17, 0.4f, 0.85f),
                heightFrac = seededRange(i * 67 + 19, 0.1f, 0.22f),
                bobAmp = seededRange(i * 79 + 23, 0.003f, 0.012f),
                bobFreq = seededRange(i * 89 + 29, 0.15f, 0.5f),
                alphaBase = seededRange(i * 97 + 31, 0.1f, 0.24f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "landslide_dust")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((95000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "landslide_dust_t",
    )
    val breathT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "landslide_dust_breath",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A140E), Color(0xFF241A10)),
                ),
            )
        }
        // Real gaussian blur on the dust clouds — softer, confined to the bottom third of the frame.
        Canvas(modifier = Modifier.fillMaxSize().blur(24.dp)) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.55f
            val breathe = 0.75f + 0.25f * sin01(breathT * 2f * PI.toFloat())
            clouds.forEach { cloud ->
                val x = phase01(cloud.x0 + time * cloud.speedMul) * w
                val bob = sin(time * cloud.bobFreq * 2f * PI.toFloat()) * cloud.bobAmp * h
                val y = (cloud.yFrac * h + bob).coerceIn(h * 0.62f, h)
                val rw = cloud.widthFrac * w
                val rh = cloud.heightFrac * h
                val base = TonalPalette.mix(Color(0xFFC9A16A), TonalPalette.pick(paletteColors, cloud.colorIndex), 0.25f)
                val tint = TonalPalette.brightness(base, brightness * 0.85f)
                val alpha = cloud.alphaBase * dim * breathe
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = rw.coerceAtLeast(rh) * 0.6f,
                    ),
                    radius = rw.coerceAtLeast(rh) * 0.6f,
                    center = Offset(x, y),
                )
                drawOval(
                    color = TonalPalette.withAlpha(tint, alpha * 0.65f),
                    topLeft = Offset(x - rw * 0.5f, y - rh * 0.5f),
                    size = Size(rw, rh),
                )
            }
        }
    }
}

private data class DustCloud(
    val x0: Float,
    val yFrac: Float,
    val speedMul: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val bobAmp: Float,
    val bobFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
