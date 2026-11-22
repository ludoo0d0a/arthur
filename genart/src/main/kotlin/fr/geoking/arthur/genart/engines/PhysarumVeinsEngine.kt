package fr.geoking.arthur.genart.engines

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.ShaderBrush
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class PhysarumVeinsNode(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)


@Composable
internal fun PhysarumVeinsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val nodes = remember(count) {
        List(count) { i ->
            PhysarumVeinsNode(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.01f, 0.035f),
                phaseOffset = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumveins")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumveins_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7CFFB2) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PHYSARUM_SOFT_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            drawRect(color = TonalPalette.brightness(Color(0xFF05080A), brightness * dim))
            // filament links between drifting nodes
            for (i in nodes.indices) {
                val a = nodes[i]
                val b = nodes[(i + 3) % nodes.size]
                val na = loopedFbm(time, radius = 1.3f, seedOffset = a.colorIndex * 17)
                val nb = loopedFbm(time, radius = 1.25f, seedOffset = b.colorIndex * 19)
                val ax = (a.x0 + 0.04f * (na * 2f - 1f) + 1f) % 1f * w
                val ay = (a.y0 + 0.04f * sin(time * 2f * PI.toFloat() + a.phaseOffset) + 1f) % 1f * h
                val bx = (b.x0 + 0.04f * (nb * 2f - 1f) + 1f) % 1f * w
                val by = (b.y0 + 0.04f * cos(time * 2f * PI.toFloat() + b.phaseOffset) + 1f) % 1f * h
                drawLine(
                    color = TonalPalette.withAlpha(c0, 0.18f * dim),
                    start = Offset(ax, ay),
                    end = Offset(bx, by),
                    strokeWidth = minOf(w, h) * 0.004f,
                )
                drawCircle(color = TonalPalette.withAlpha(c0, 0.35f * dim), radius = minOf(w, h) * a.radius, center = Offset(ax, ay))
            }
        }
    }
}
