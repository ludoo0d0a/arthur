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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun Pseudo3DEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grid = qualityCount(quality, low = 5, medium = 7, high = 9)
    val points = remember(grid) {
        buildList {
            for (ix in 0 until grid) {
                for (iy in 0 until grid) {
                    for (iz in 0 until grid) {
                        add(
                            Vec3(
                                x = (ix / (grid - 1f)) * 2f - 1f,
                                y = (iy / (grid - 1f)) * 2f - 1f,
                                z = (iz / (grid - 1f)) * 2f - 1f,
                            ),
                        )
                    }
                }
            }
        }
    }
    val transition = rememberInfiniteTransition(label = "pseudo3d")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pseudo3d_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF111827), Color(0xFF020617)),
            ),
        )
        val angle = phase01(t) * 2f * PI.toFloat()
        val tilt = 0.35f + 0.15f * sin(angle * 0.5f)
        val focal = maxOf(w, h) * 0.85f
        val projected = points.map { p ->
            val ry = rotateY(p, angle)
            val rx = rotateX(ry, tilt)
            val depth = rx.z + 2.6f
            val scale = focal / depth
            Offset(cx + rx.x * scale * 0.28f * minOf(w, h), cy + rx.y * scale * 0.28f * minOf(w, h)) to depth
        }
        val edgeStep = if (isActive) 1 else 2
        for (i in projected.indices step edgeStep) {
            val (a, da) = projected[i]
            val neighbors = listOf(i + 1, i + grid, i + grid * grid).filter { it < projected.size }
            neighbors.forEach { j ->
                if (sameLatticeEdge(i, j, grid)) {
                    val (b, db) = projected[j]
                    val depth = ((da + db) * 0.5f).coerceIn(1.2f, 4.5f)
                    val alpha = ((4.5f - depth) / 3.3f).coerceIn(0.15f, 0.75f) * brightness.coerceAtMost(1.2f)
                    val color = TonalPalette.brightness(
                        TonalPalette.pick(paletteColors, (i + j) % paletteColors.size),
                        brightness,
                    )
                    drawLine(
                        color = TonalPalette.withAlpha(color, alpha * 0.55f),
                        start = a,
                        end = b,
                        strokeWidth = if (isActive) 1.6f else 1.2f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        projected.forEachIndexed { index, (pt, depth) ->
            val alpha = ((4.2f - depth) / 3f).coerceIn(0.2f, 0.95f) * brightness.coerceAtMost(1.2f)
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, index), brightness)
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = (3.2f + (3.5f - depth)).coerceIn(1.5f, 5.5f),
                center = pt,
            )
        }
        // Soft horizon ring
        drawCircle(
            color = TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 0), 0.12f * brightness),
            radius = minOf(w, h) * 0.32f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f),
        )
    }
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun rotateY(v: Vec3, a: Float): Vec3 {
    val c = cos(a)
    val s = sin(a)
    return Vec3(x = v.x * c + v.z * s, y = v.y, z = -v.x * s + v.z * c)
}

private fun rotateX(v: Vec3, a: Float): Vec3 {
    val c = cos(a)
    val s = sin(a)
    return Vec3(x = v.x, y = v.y * c - v.z * s, z = v.y * s + v.z * c)
}

private fun sameLatticeEdge(i: Int, j: Int, grid: Int): Boolean {
    val n = grid
    val ix = i % n
    val iy = (i / n) % n
    val iz = i / (n * n)
    val jx = j % n
    val jy = (j / n) % n
    val jz = j / (n * n)
    val dx = kotlin.math.abs(ix - jx)
    val dy = kotlin.math.abs(iy - jy)
    val dz = kotlin.math.abs(iz - jz)
    return (dx + dy + dz) == 1
}
