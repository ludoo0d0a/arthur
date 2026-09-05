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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.min

/** Truchet-style quarter-arc tile grid with slow rotation / phase offset. */
@Composable
internal fun ArcMosaicEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cols = qualityCount(quality, low = 5, medium = 7, high = 9)
    val tiles = remember(cols) {
        List(cols * cols) { i ->
            ArcTile(
                rot0 = seededUnit(i * 17 + 3) * 4f, // 0..4 quarter turns
                speedMul = seededRange(i * 41 + 11, 0.15f, 0.45f),
                colorIndex = i,
                strokeFrac = seededRange(i * 53 + 13, 0.06f, 0.12f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "arc_mosaic")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "arc_mosaic_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF08070E))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val cell = min(w, h) / cols
        val originX = (w - cell * cols) * 0.5f
        val originY = (h - cell * cols) * 0.5f
        tiles.forEachIndexed { index, tile ->
            val col = index % cols
            val row = index / cols
            val cx = originX + (col + 0.5f) * cell
            val cy = originY + (row + 0.5f) * cell
            val rotDeg = (tile.rot0 + time * tile.speedMul * 4f) * 90f
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, tile.colorIndex),
                brightness,
            )
            val stroke = cell * tile.strokeFrac
            val arcSize = cell * 0.92f
            rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
                // Two opposing quarter-arcs (classic Truchet).
                drawArc(
                    color = TonalPalette.withAlpha(tint, 0.55f * dim),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - arcSize * 0.5f, cy - arcSize * 0.5f),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = TonalPalette.withAlpha(tint, 0.4f * dim),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - arcSize * 0.5f, cy - arcSize * 0.5f),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round),
                )
            }
            // Soft fill hint under arcs.
            drawCircle(
                color = TonalPalette.withAlpha(tint, 0.06f * dim),
                radius = cell * 0.35f,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class ArcTile(
    val rot0: Float,
    val speedMul: Float,
    val colorIndex: Int,
    val strokeFrac: Float,
)
