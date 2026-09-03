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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun TonalGeometryEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val shapes = remember(count) {
        List(count) { i ->
            ShapeSeed(
                kind = i % 3,
                x = seededUnit(i * 13 + 1),
                y = seededUnit(i * 17 + 3),
                size = seededRange(i * 23 + 5, 0.08f, 0.22f),
                rot = seededUnit(i * 29 + 7) * 2f * PI.toFloat(),
                colorIndex = i,
                morph = seededUnit(i * 37 + 11),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "tonalgeometry")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tonalgeometry_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF020617)),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        val time = phase01(t)
        val reshuffle = (time * 4f).toInt()
        shapes.forEachIndexed { index, seed ->
            val phaseSeed = reshuffle * 97 + index * 13
            val x = lerp(seed.x, seededUnit(phaseSeed + 1), 0.35f + 0.2f * sin01(time * 2f * PI.toFloat() + seed.morph))
            val y = lerp(seed.y, seededUnit(phaseSeed + 2), 0.35f + 0.2f * sin01(time * 2f * PI.toFloat() + seed.rot))
            val scale = seed.size * (0.75f + 0.35f * sin01(time * 2f * PI.toFloat() + seed.morph * 4f))
            val cx = x * w
            val cy = y * h
            val dim = scale * minOf(w, h)
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, seed.colorIndex), brightness)
            val alpha = (if (isActive) 0.55f else 0.35f) * brightness.coerceAtMost(1.2f)
            when (seed.kind) {
                0 -> drawRect(
                    color = TonalPalette.withAlpha(color, alpha),
                    topLeft = Offset(cx - dim * 0.5f, cy - dim * 0.4f),
                    size = Size(dim, dim * 0.8f),
                )
                1 -> {
                    val path = Path().apply {
                        val rot = seed.rot + time * 2f * PI.toFloat() * 0.25f
                        val r = dim * 0.55f
                        moveTo(cx + cos(rot) * r, cy + sin(rot) * r)
                        lineTo(cx + cos(rot + 2.2f) * r, cy + sin(rot + 2.2f) * r)
                        lineTo(cx + cos(rot + 4.2f) * r, cy + sin(rot + 4.2f) * r)
                        close()
                    }
                    drawPath(path = path, color = TonalPalette.withAlpha(color, alpha))
                }
                else -> drawCircle(
                    color = TonalPalette.withAlpha(color, alpha),
                    radius = dim * 0.45f,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}

private data class ShapeSeed(
    val kind: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val rot: Float,
    val colorIndex: Int,
    val morph: Float,
)
