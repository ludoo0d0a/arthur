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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Sparse soft meteor streaks — no strobing, slow fade trails. */
@Composable
internal fun MeteorsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val starCount = qualityCount(quality, low = 18, medium = 28, high = 40)
    val stars = remember(starCount) {
        List(starCount) { i ->
            Offset(seededUnit(i * 13 + 5), seededUnit(i * 19 + 9)) to
                seededRange(i * 31 + 11, 0.08f, 0.35f)
        }
    }
    val meteorCount = qualityCount(quality, low = 3, medium = 4, high = 6)
    val meteors = remember(meteorCount) {
        List(meteorCount) { i ->
            MeteorSeed(
                startX = seededUnit(i * 17 + 3),
                startY = seededRange(i * 29 + 7, 0f, 0.45f),
                lengthFrac = seededRange(i * 41 + 11, 0.12f, 0.28f),
                angle = seededRange(i * 53 + 13, 0.35f, 0.85f),
                phaseOffset = seededUnit(i * 67 + 19),
                duration = seededRange(i * 79 + 23, 0.12f, 0.28f),
                thickness = seededRange(i * 89 + 29, 1.2f, 2.8f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "meteors")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "meteors_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF060510), Color(0xFF020208)),
            ),
        )
        stars.forEach { (pos, alpha) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = 1.2f,
                center = Offset(pos.x * w, pos.y * h),
            )
        }
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.5f
        meteors.forEach { m ->
            val local = phase01(time + m.phaseOffset)
            if (local > m.duration) return@forEach
            val life = local / m.duration
            val travel = life * m.lengthFrac * 2.2f
            val x = (m.startX + cos(m.angle) * travel) * w
            val y = (m.startY + sin(m.angle) * travel) * h
            val trail = m.lengthFrac * minOf(w, h)
            val start = Offset(x - cos(m.angle) * trail, y - sin(m.angle) * trail)
            val end = Offset(x, y)
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.15f).coerceIn(0f, 1f)
            val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, m.colorIndex), 0.35f)
            val color = TonalPalette.brightness(base, brightness)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(color, 0.55f * fade * dim),
                    ),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = m.thickness,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, 0.7f * fade * dim),
                radius = m.thickness * 1.2f,
                center = end,
            )
        }
    }
}

private data class MeteorSeed(
    val startX: Float,
    val startY: Float,
    val lengthFrac: Float,
    val angle: Float,
    val phaseOffset: Float,
    val duration: Float,
    val thickness: Float,
    val colorIndex: Int,
)
