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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Abstract soft ribbons drifting — calmer and thinner than aurora sheets. */
@Composable
internal fun SoftRibbonsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 3, medium = 5, high = 7)
    val ribbons = remember(count) {
        List(count) { i ->
            SoftRibbonSeed(
                baseY = seededRange(i * 17 + 3, 0.2f, 0.8f),
                amplitude = seededRange(i * 29 + 7, 0.04f, 0.12f),
                cycles = seededRange(i * 41 + 11, 1.2f, 2.8f),
                phase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                thickness = seededRange(i * 67 + 19, 1.5f, 4f),
                speedMul = seededRange(i * 79 + 23, 0.5f, 1.2f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "soft_ribbons")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((35000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "soft_ribbons_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0C0A14), Color(0xFF040308)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.75f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val segments = 40
        ribbons.forEach { ribbon ->
            val path = Path()
            val amp = ribbon.amplitude * h * dim
            val baseY = ribbon.baseY * h
            val freq = ribbon.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + amp * sin(x * freq + time * ribbon.speedMul + ribbon.phase)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val base = TonalPalette.pick(paletteColors, ribbon.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(color, 0.12f * dim),
                style = Stroke(width = ribbon.thickness * 4f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(color, 0.45f * dim),
                style = Stroke(width = ribbon.thickness, cap = StrokeCap.Round),
            )
        }
    }
}

private data class SoftRibbonSeed(
    val baseY: Float,
    val amplitude: Float,
    val cycles: Float,
    val phase: Float,
    val thickness: Float,
    val speedMul: Float,
    val colorIndex: Int,
)
