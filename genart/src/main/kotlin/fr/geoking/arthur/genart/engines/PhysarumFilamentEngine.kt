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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class Filament(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val bend: Float,
    val phase: Float,
    val width: Float,
    val colorIndex: Int,
)

@Composable
internal fun PhysarumFilamentEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val filaments = remember(count) {
        List(count) { i ->
            val side = if (i % 2 == 0) -1f else 1f
            Filament(
                startX = seededRange(i * 17 + 3, 0.12f, 0.88f),
                startY = seededRange(i * 29 + 7, 0.12f, 0.88f),
                endX = seededRange(i * 41 + 11, 0.12f, 0.88f),
                endY = seededRange(i * 53 + 13, 0.12f, 0.88f),
                bend = side * seededRange(i * 67 + 19, 0.08f, 0.22f),
                phase = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                width = seededRange(i * 79 + 31, 0.0015f, 0.0032f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumfilament")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumfilament_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val teal = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF66E8BE) }, brightness * dim)
        val coral = TonalPalette.brightness(paletteColors.getOrElse(2) { Color(0xFFFF7780) }, brightness * dim)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF04090B), Color(0xFF071411), Color(0xFF030809)),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(teal, 0.12f * dim),
                0.48f to TonalPalette.withAlpha(Color(0xFF12352B), 0.10f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.48f, h * 0.53f),
                radius = minDim * 0.62f,
            ),
            radius = minDim * 0.62f,
            center = Offset(w * 0.48f, h * 0.53f),
        )

        filaments.forEachIndexed { index, filament ->
            val noise = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = index * 37 + 5)
            val drift = sin(time * 2f * PI.toFloat() + filament.phase) * 0.025f
            val sx = (filament.startX + drift) * w
            val sy = (filament.startY + (noise - 0.5f) * 0.035f) * h
            val ex = (filament.endX - drift * 0.7f) * w
            val ey = (filament.endY + (0.5f - noise) * 0.035f) * h
            val dx = ex - sx
            val dy = ey - sy
            val bend = (filament.bend + (noise - 0.5f) * 0.08f) * minDim
            val path = Path().apply {
                moveTo(sx, sy)
                cubicTo(
                    sx + dx * 0.30f - bend,
                    sy + dy * 0.20f + bend,
                    sx + dx * 0.68f + bend,
                    sy + dy * 0.76f - bend,
                    ex,
                    ey,
                )
            }
            val color = if (index % 5 == 0) coral else teal
            val stroke = minDim * filament.width
            drawPath(path, TonalPalette.withAlpha(color, 0.13f * dim), style = Stroke(strokeWidth = stroke * 6f, cap = StrokeCap.Round))
            drawPath(path, TonalPalette.withAlpha(color, 0.30f * dim), style = Stroke(strokeWidth = stroke * 2.7f, cap = StrokeCap.Round))
            drawPath(path, TonalPalette.withAlpha(color, 0.78f * dim), style = Stroke(strokeWidth = stroke, cap = StrokeCap.Round))

            // Fine side branches grow from the main filament, tapering into the dark substrate.
            for (branch in 0..1) {
                val fraction = if (branch == 0) 0.32f else 0.69f
                val bx = sx + dx * fraction
                val by = sy + dy * fraction + bend * (0.35f - fraction)
                val branchSide = if ((index + branch) % 2 == 0) -1f else 1f
                val reach = minDim * (0.07f + 0.025f * noise)
                val branchPath = Path().apply {
                    moveTo(bx, by)
                    cubicTo(
                        bx + dx * 0.07f + reach * branchSide * 0.35f,
                        by + reach * branchSide * 0.25f,
                        bx + dx * 0.12f + reach * branchSide,
                        by + reach * branchSide,
                        bx + dx * 0.16f + reach * branchSide * 1.12f,
                        by + reach * branchSide * 1.18f,
                    )
                }
                drawPath(branchPath, TonalPalette.withAlpha(color, 0.12f * dim), style = Stroke(strokeWidth = stroke * 3.5f, cap = StrokeCap.Round))
                drawPath(branchPath, TonalPalette.withAlpha(color, 0.60f * dim), style = Stroke(strokeWidth = stroke * 0.68f, cap = StrokeCap.Round))
            }

            val beadRadius = minDim * (0.0035f + 0.002f * noise)
            drawCircle(TonalPalette.withAlpha(color, 0.26f * dim), beadRadius * 3.2f, Offset(ex, ey))
            drawCircle(TonalPalette.withAlpha(color, 0.72f * dim), beadRadius, Offset(ex, ey))
        }
    }
}
