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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Classic impossible cube — hollow isometric frame whose near edge wrongly passes
 * behind a far strut; soft light wash crawls the beams.
 */
@Composable
internal fun ImpossibleCubeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val beams = remember { impossibleCubeBeams() }
    val strokeMul = when (quality) {
        GenartQuality.Low -> 0.88f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.18f
    }
    val transition = rememberInfiniteTransition(label = "impossible_cube")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((38000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "impossible_cube_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF151825), brightness * 0.5f),
                    Color(0xFF080A10),
                    Color(0xFF020306),
                ),
                center = Offset(w * 0.5f, h * 0.46f),
                radius = maxOf(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val yaw = 0.12f * sin(time * 0.5f)
        val dim = if (isActive) 1f else 0.58f
        val cx = w * 0.5f
        val cy = h * 0.52f
        val scale = minDim * 0.26f
        val base = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
        val paradox = TonalPalette.brightness(TonalPalette.pick(paletteColors, 3), brightness)
        val beamW = scale * 0.16f * strokeMul
        val projected = beams.map { beam ->
            val a = projectCube(beam.ax, beam.ay, beam.az, cx, cy, scale, yaw)
            val b = projectCube(beam.bx, beam.by, beam.bz, cx, cy, scale, yaw)
            beam to (a to b)
        }
        // Back / normal beams first, paradoxical overlap last for the contradiction.
        projected.filter { !it.first.paradox }.forEachIndexed { i, (beam, ab) ->
            drawImpossibleBeam(
                a = ab.first,
                b = ab.second,
                body = base,
                along = phase01(i * 0.07f + sin01(time + beam.phase)),
                beamW = beamW,
                dim = dim,
            )
        }
        projected.filter { it.first.paradox }.forEach { (beam, ab) ->
            val along = phase01(0.55f + sin01(time + beam.phase) * 0.2f)
            drawImpossibleBeam(
                a = ab.first,
                b = ab.second,
                body = paradox,
                along = along,
                beamW = beamW * 1.06f,
                dim = dim,
            )
        }
        // Soft silhouette outline for readable Ambient shape.
        val hull = listOf(
            projectCube(-1f, -1f, -1f, cx, cy, scale, yaw),
            projectCube(1f, -1f, -1f, cx, cy, scale, yaw),
            projectCube(1f, 1f, -1f, cx, cy, scale, yaw),
            projectCube(-1f, 1f, -1f, cx, cy, scale, yaw),
        )
        val outline = Path().apply {
            moveTo(hull[0].x, hull[0].y)
            hull.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(
            path = outline,
            color = TonalPalette.withAlpha(TonalPalette.mix(base, Color.White, 0.15f), 0.18f * dim),
            style = Stroke(width = 1.4f, cap = StrokeCap.Round),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawImpossibleBeam(
    a: Offset,
    b: Offset,
    body: Color,
    along: Float,
    beamW: Float,
    dim: Float,
) {
    val shaded = TonalPalette.mix(body, Color.White, along * 0.3f)
    drawLine(
        color = TonalPalette.withAlpha(TonalPalette.mix(body, Color.Black, 0.28f), 0.72f * dim),
        start = a,
        end = b,
        strokeWidth = beamW * 1.38f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = TonalPalette.withAlpha(shaded, 0.92f * dim),
        start = a,
        end = b,
        strokeWidth = beamW,
        cap = StrokeCap.Round,
    )
    val hiT = (along * 0.8f + 0.1f).coerceIn(0.08f, 0.9f)
    drawCircle(
        color = TonalPalette.withAlpha(Color.White, 0.28f * dim),
        radius = beamW * 0.48f,
        center = Offset(a.x + (b.x - a.x) * hiT, a.y + (b.y - a.y) * hiT),
    )
}

private data class CubeBeam(
    val ax: Float,
    val ay: Float,
    val az: Float,
    val bx: Float,
    val by: Float,
    val bz: Float,
    val paradox: Boolean = false,
    val phase: Float = 0f,
)

/**
 * Cube edges in unit space. Paradox beams are the rear vertical/horizontal that
 * incorrectly overdraw the near face — classic impossible-cube contradiction.
 */
private fun impossibleCubeBeams(): List<CubeBeam> = listOf(
    // Near face (z = -1)
    CubeBeam(-1f, -1f, -1f, 1f, -1f, -1f, phase = 0.05f),
    CubeBeam(1f, -1f, -1f, 1f, 1f, -1f, phase = 0.12f),
    CubeBeam(1f, 1f, -1f, -1f, 1f, -1f, phase = 0.18f),
    CubeBeam(-1f, 1f, -1f, -1f, -1f, -1f, phase = 0.24f),
    // Far face (z = +1) — partial, two edges left for paradox redraw
    CubeBeam(-1f, -1f, 1f, 1f, -1f, 1f, phase = 0.32f),
    CubeBeam(1f, -1f, 1f, 1f, 1f, 1f, phase = 0.38f),
    // Depth connectors
    CubeBeam(-1f, -1f, -1f, -1f, -1f, 1f, phase = 0.46f),
    CubeBeam(1f, -1f, -1f, 1f, -1f, 1f, phase = 0.52f),
    CubeBeam(1f, 1f, -1f, 1f, 1f, 1f, phase = 0.58f),
    CubeBeam(-1f, 1f, -1f, -1f, 1f, 1f, phase = 0.64f),
    // Paradoxical rear edges drawn last (appear in front of near face)
    CubeBeam(-1f, 1f, 1f, 1f, 1f, 1f, paradox = true, phase = 0.72f),
    CubeBeam(-1f, -1f, 1f, -1f, 1f, 1f, paradox = true, phase = 0.8f),
)

private fun projectCube(
    x: Float,
    y: Float,
    z: Float,
    cx: Float,
    cy: Float,
    scale: Float,
    yaw: Float,
): Offset {
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val rx = x * cosY - z * sinY
    val rz = x * sinY + z * cosY
    val px = (rx - rz) * cos(PI.toFloat() / 6f) * scale + cx
    val py = -(y * 0.92f + (rx + rz) * 0.32f) * scale + cy
    return Offset(px, py)
}
