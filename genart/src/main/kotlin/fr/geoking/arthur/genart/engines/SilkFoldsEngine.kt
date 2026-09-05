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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Stacked horizontal undulating bands with soft fills — silk-fold look. */
@Composable
internal fun SilkFoldsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 10)
    val folds = remember(count) {
        List(count) { i ->
            SilkFold(
                baseY = seededRange(i * 17 + 3, 0.08f, 0.9f),
                amplitude = seededRange(i * 29 + 7, 0.02f, 0.07f),
                cycles = seededRange(i * 41 + 11, 1.0f, 2.4f),
                phase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                speedMul = seededRange(i * 67 + 19, 0.4f, 1.1f),
                thickness = seededRange(i * 79 + 23, 0.06f, 0.14f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "silk_folds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((38000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "silk_folds_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0810), Color(0xFF030208)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val segments = 36
        folds.sortedBy { it.baseY }.forEach { fold ->
            val path = Path()
            val amp = fold.amplitude * h * dim
            val baseY = fold.baseY * h
            val thick = fold.thickness * h
            val freq = fold.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + amp * sin(x * freq + time * fold.speedMul + fold.phase)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (k in segments downTo 0) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + thick + amp * 0.7f * sin(x * freq + time * fold.speedMul + fold.phase + 0.4f)
                path.lineTo(x, y)
            }
            path.close()
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, fold.colorIndex),
                brightness,
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, 0.28f * dim),
            )
        }
    }
}

private data class SilkFold(
    val baseY: Float,
    val amplitude: Float,
    val cycles: Float,
    val phase: Float,
    val speedMul: Float,
    val thickness: Float,
    val colorIndex: Int,
)
