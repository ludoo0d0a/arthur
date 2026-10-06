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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Wire lattice with lit terminator nodes, depth fog, and a sliding spotlight. */
@Composable
internal fun LitLatticeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grid = qualityCount(quality, low = 4, medium = 5, high = 6)
    val points = remember(grid) {
        buildList {
            for (ix in 0 until grid) {
                for (iy in 0 until grid) {
                    for (iz in 0 until grid) {
                        add(
                            LitLatticeVec3(
                                x = (ix / (grid - 1f).coerceAtLeast(1f)) * 2f - 1f,
                                y = (iy / (grid - 1f).coerceAtLeast(1f)) * 2f - 1f,
                                z = (iz / (grid - 1f).coerceAtLeast(1f)) * 2f - 1f,
                            ),
                        )
                    }
                }
            }
        }
    }
    val edges = remember(grid, points) {
        buildList {
            val strideY = grid
            val strideZ = grid * grid
            for (ix in 0 until grid) {
                for (iy in 0 until grid) {
                    for (iz in 0 until grid) {
                        val i = ix * strideZ + iy * strideY + iz
                        if (ix + 1 < grid) add(i to (i + strideZ))
                        if (iy + 1 < grid) add(i to (i + strideY))
                        if (iz + 1 < grid) add(i to (i + 1))
                    }
                }
            }
        }
    }
    val transition = rememberInfiniteTransition(label = "lit_lattice")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lit_lattice_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val minDim = minOf(w, h)
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1220), Color(0xFF030712)),
            ),
        )

        val angle = time
        val tilt = 0.32f + 0.1f * sin(time * 0.5f)
        val focal = maxOf(w, h) * 0.9f
        val lightDir = LitLatticeVec3(
            x = cos(time * 0.4f + 0.8f),
            y = 0.55f,
            z = sin(time * 0.4f + 0.8f),
        )
        val spotX = cx + cos(time * 0.55f) * minDim * 0.28f
        val spotY = cy + sin(time * 0.45f) * minDim * 0.18f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(
                        TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness),
                        0.22f * dim,
                    ),
                    Color.Transparent,
                ),
                center = Offset(spotX, spotY),
                radius = minDim * 0.45f,
            ),
            radius = minDim * 0.45f,
            center = Offset(spotX, spotY),
        )

        data class Proj(val x: Float, val y: Float, val depth: Float, val lit: Float, val world: LitLatticeVec3)
        val projected = points.map { p ->
            val ry = litRotateY(p, angle)
            val rx = litRotateX(ry, tilt)
            val depth = rx.z + 2.6f
            val scale = focal / depth
            val lit = ((rx.x * lightDir.x + rx.y * lightDir.y + rx.z * lightDir.z) * 0.5f + 0.5f).coerceIn(0f, 1f)
            Proj(cx + rx.x * scale * 0.42f, cy + rx.y * scale * 0.42f, depth, lit, rx)
        }

        val wire = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
        edges.forEach { (a, b) ->
            val pa = projected[a]
            val pb = projected[b]
            val fog = ((1f - ((pa.depth + pb.depth) * 0.5f - 1.6f) / 2.4f).coerceIn(0.15f, 1f))
            drawLine(
                color = TonalPalette.withAlpha(wire, 0.18f * fog * dim),
                start = Offset(pa.x, pa.y),
                end = Offset(pb.x, pb.y),
                strokeWidth = minDim * 0.0022f,
                cap = StrokeCap.Round,
            )
        }

        projected.forEach { p ->
            val fog = (1f - (p.depth - 1.6f) / 2.4f).coerceIn(0.2f, 1f)
            val spotDist = kotlin.math.hypot(p.x - spotX, p.y - spotY) / (minDim * 0.45f)
            val spot = (1f - spotDist.coerceIn(0f, 1f)).coerceIn(0f, 1f)
            val glow = (0.25f + 0.75f * p.lit) * (0.55f + 0.45f * spot)
            val r = minDim * (0.006f + 0.01f * glow) * fog
            val tint = TonalPalette.mix(
                wire,
                Color.White,
                0.2f + 0.55f * glow,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color.White, 0.55f * glow * dim),
                    0.45f to TonalPalette.withAlpha(tint, 0.45f * glow * dim),
                    1f to Color.Transparent,
                    center = Offset(p.x - r * 0.2f, p.y - r * 0.25f),
                    radius = r * 2.2f,
                ),
                radius = r * 2.2f,
                center = Offset(p.x, p.y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(tint, (0.4f + 0.5f * glow) * fog * dim),
                radius = r,
                center = Offset(p.x, p.y),
            )
        }
    }
}

private data class LitLatticeVec3(val x: Float, val y: Float, val z: Float)

private fun litRotateY(p: LitLatticeVec3, a: Float): LitLatticeVec3 {
    val c = cos(a)
    val s = sin(a)
    return LitLatticeVec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
}

private fun litRotateX(p: LitLatticeVec3, a: Float): LitLatticeVec3 {
    val c = cos(a)
    val s = sin(a)
    return LitLatticeVec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
}
