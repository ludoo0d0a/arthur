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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Soft Voronoi-like cells from drifting sites with palette wash. */
@Composable
internal fun VoronoiWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 6, medium = 9, high = 12)
    val sites = remember(count) {
        List(count) { i ->
            VoronoiSite(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                driftX = seededRange(i * 41 + 11, -0.04f, 0.04f),
                driftY = seededRange(i * 53 + 13, -0.04f, 0.04f),
                radiusFrac = seededRange(i * 67 + 19, 0.12f, 0.22f),
                sides = 5 + (i % 3),
                rot0 = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                rotSpeed = seededRange(i * 89 + 29, 0.1f, 0.4f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "voronoi_wash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "voronoi_wash_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF07060C))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val positions = sites.map { site ->
            Offset(
                phase01(site.x0 + time * site.driftX) * w,
                phase01(site.y0 + time * site.driftY) * h,
            )
        }
        sites.forEachIndexed { index, site ->
            val center = positions[index]
            // Approximate cell radius from nearest-neighbor distance.
            var nearest = minDim * 0.35f
            positions.forEachIndexed { j, other ->
                if (j != index) {
                    val d = hypot(center.x - other.x, center.y - other.y)
                    if (d < nearest) nearest = d
                }
            }
            val radius = (nearest * 0.48f).coerceIn(minDim * 0.08f, site.radiusFrac * minDim * 1.4f)
            val rot = site.rot0 + time * 2f * PI.toFloat() * site.rotSpeed
            val path = Path()
            for (k in 0 until site.sides) {
                val a = rot + k * 2f * PI.toFloat() / site.sides
                // Soft irregularity from neighbor direction.
                val neighborBias = if (positions.size > 1) {
                    val next = positions[(index + 1) % positions.size]
                    atan2(next.y - center.y, next.x - center.x) * 0.05f
                } else {
                    0f
                }
                val r = radius * (0.92f + 0.08f * sin(a * 2f + neighborBias))
                val px = center.x + cos(a) * r
                val py = center.y + sin(a) * r
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, site.colorIndex),
                brightness,
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, 0.22f * dim),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, 0.45f * dim),
                style = Stroke(width = 1.2f),
            )
            drawCircle(
                color = TonalPalette.withAlpha(tint, 0.35f * dim),
                radius = 2.5f,
                center = center,
            )
        }
    }
}

private data class VoronoiSite(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val radiusFrac: Float,
    val sides: Int,
    val rot0: Float,
    val rotSpeed: Float,
    val colorIndex: Int,
)
