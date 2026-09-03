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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun SnowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 40, medium = 70, high = 110)
    val flakes = remember(count) {
        List(count) { i ->
            SnowSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.4f, 1.2f),
                radius = seededRange(i * 53 + 13, 1.5f, 5.5f),
                swayAmp = seededRange(i * 67 + 19, 0.01f, 0.05f),
                swayFreq = seededRange(i * 79 + 23, 0.4f, 2.2f),
                swayPhase = seededUnit(i * 83 + 29) * 2f * PI.toFloat(),
                alphaBase = seededRange(i * 97 + 31, 0.35f, 1f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "snow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "snow_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1220), Color(0xFF020617)),
                startY = 0f,
                endY = h,
            ),
        )
        val loopT = phase01(t)
        val glowLayers = if (isActive) 3 else 1
        flakes.forEach { f ->
            val fallPhase = phase01(f.y0 + f.fallSpeed * loopT)
            val y = fallPhase * h
            val swayTime = loopT * 2f * PI.toFloat() * f.swayFreq + f.swayPhase
            val x = phase01(f.x0 + sin(swayTime) * f.swayAmp) * w
            val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, f.colorIndex), 0.12f)
            val color = TonalPalette.brightness(base, brightness)
            val depth = ((f.radius - 1.5f) / 4f).coerceIn(0f, 1f)
            val alpha = (f.alphaBase * (0.5f + 0.5f * depth) * (if (isActive) 1f else 0.75f)).coerceIn(0f, 1f)
            if (depth > 0.55f) {
                for (s in glowLayers downTo 1) {
                    val spread = s / glowLayers.toFloat()
                    drawCircle(
                        color = TonalPalette.withAlpha(color, alpha * 0.18f * (1f - spread * 0.6f)),
                        radius = f.radius * (2.2f + spread * 1.3f),
                        center = Offset(x, y),
                    )
                }
            }
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = f.radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class SnowSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val radius: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val swayPhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
