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
import kotlin.math.PI
import kotlin.math.sin

/** Soft fog banks drifting horizontally — low contrast, car-safe. */
@Composable
internal fun FogEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 10)
    val banks = remember(count) {
        List(count) { i ->
            FogBank(
                x0 = seededUnit(i * 17 + 3),
                yFrac = seededRange(i * 29 + 7, 0.15f, 0.85f),
                speedMul = seededRange(i * 41 + 11, 0.04f, 0.14f),
                widthFrac = seededRange(i * 53 + 13, 0.35f, 0.7f),
                heightFrac = seededRange(i * 67 + 19, 0.08f, 0.22f),
                bobAmp = seededRange(i * 79 + 23, 0.004f, 0.02f),
                bobFreq = seededRange(i * 89 + 29, 0.2f, 0.8f),
                alphaBase = seededRange(i * 97 + 31, 0.08f, 0.22f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fog")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((55000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fog_t",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF101820), Color(0xFF080C12)),
                ),
            )
        }
        // Real gaussian blur on the fog banks — softer and less "ring-shaped" than gradient-only fog.
        Canvas(modifier = Modifier.fillMaxSize().blur(22.dp)) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.6f
            banks.forEach { bank ->
                val x = phase01(bank.x0 + time * bank.speedMul) * w
                val bob = sin(time * bank.bobFreq * 2f * PI.toFloat()) * bank.bobAmp * h
                val y = bank.yFrac * h + bob
                val rw = bank.widthFrac * w
                val rh = bank.heightFrac * h
                val base = TonalPalette.mix(Color(0xFFD8E4F0), TonalPalette.pick(paletteColors, bank.colorIndex), 0.2f)
                val tint = TonalPalette.brightness(base, brightness * 0.9f)
                val alpha = bank.alphaBase * dim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = rw.coerceAtLeast(rh) * 0.65f,
                    ),
                    radius = rw.coerceAtLeast(rh) * 0.65f,
                    center = Offset(x, y),
                )
                drawOval(
                    color = TonalPalette.withAlpha(tint, alpha * 0.7f),
                    topLeft = Offset(x - rw * 0.5f, y - rh * 0.5f),
                    size = Size(rw, rh),
                )
            }
        }
    }
}

private data class FogBank(
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
