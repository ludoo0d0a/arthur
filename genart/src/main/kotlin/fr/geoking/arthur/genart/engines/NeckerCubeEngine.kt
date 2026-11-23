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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Necker cube wireframe — transparent cube whose front-face highlight slowly
 * bistable-flips between the two depth interpretations.
 */
@Composable
internal fun NeckerCubeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val edges = remember { neckerCubeEdges() }
    val verts = remember { neckerCubeVerts() }
    val strokeBase = when (quality) {
        GenartQuality.Low -> 2.2f
        GenartQuality.Medium -> 3f
        GenartQuality.High -> 3.8f
    }
    val transition = rememberInfiniteTransition(label = "necker_cube")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "necker_cube_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF141820), brightness * 0.52f),
                    Color(0xFF0A0C12),
                    Color(0xFF030408),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.92f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        // Soft dwell on each interpretation (ease, not hard flash).
        val raw = sin01(time)
        val flip = smoothstep(0.12f, 0.88f, raw)
        val dim = if (isActive) 1f else 0.58f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.24f
        val yaw = 0.08f * sin(time * 0.35f)
        val accent = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
        val ghost = TonalPalette.mix(accent, Color(0xFF64748B), 0.5f)
        val screenVerts = verts.map { projectNecker(it, cx, cy, scale, yaw, flip) }
        edges.forEach { edge ->
            val aFlip = if (edge.faceA) flip else 1f - flip
            val bFlip = if (edge.faceB) flip else 1f - flip
            val frontBias = (aFlip + bFlip) * 0.5f
            val p0 = screenVerts[edge.i0]
            val p1 = screenVerts[edge.i1]
            drawLine(
                color = TonalPalette.withAlpha(ghost, (0.2f + 0.2f * (1f - frontBias)) * dim),
                start = p0,
                end = p1,
                strokeWidth = strokeBase * 0.9f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = TonalPalette.withAlpha(accent, (0.48f + 0.48f * frontBias) * dim),
                start = p0,
                end = p1,
                strokeWidth = strokeBase * (0.8f + 0.45f * frontBias),
                cap = StrokeCap.Round,
            )
            if (frontBias > 0.58f) {
                val mid = Offset((p0.x + p1.x) * 0.5f, (p0.y + p1.y) * 0.5f)
                drawCircle(
                    color = TonalPalette.withAlpha(
                        TonalPalette.mix(accent, Color.White, 0.4f),
                        0.26f * dim * frontBias,
                    ),
                    radius = strokeBase * 1.7f,
                    center = mid,
                )
            }
        }
    }
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private data class NeckerEdge(val i0: Int, val i1: Int, val faceA: Boolean, val faceB: Boolean)

private fun neckerCubeVerts(): List<Triple<Float, Float, Float>> = listOf(
    Triple(-1f, -1f, -1f),
    Triple(1f, -1f, -1f),
    Triple(1f, 1f, -1f),
    Triple(-1f, 1f, -1f),
    Triple(-1f, -1f, 1f),
    Triple(1f, -1f, 1f),
    Triple(1f, 1f, 1f),
    Triple(-1f, 1f, 1f),
)

private fun neckerCubeEdges(): List<NeckerEdge> = listOf(
    // Face A (z=-1) — interpretation 1 front
    NeckerEdge(0, 1, true, true),
    NeckerEdge(1, 2, true, true),
    NeckerEdge(2, 3, true, true),
    NeckerEdge(3, 0, true, true),
    // Face B (z=+1) — interpretation 2 front
    NeckerEdge(4, 5, false, false),
    NeckerEdge(5, 6, false, false),
    NeckerEdge(6, 7, false, false),
    NeckerEdge(7, 4, false, false),
    // Connectors
    NeckerEdge(0, 4, true, false),
    NeckerEdge(1, 5, true, false),
    NeckerEdge(2, 6, true, false),
    NeckerEdge(3, 7, true, false),
)

/**
 * Slight depth morph: as [flip] rises, face B vertices pull forward and face A
 * recede — enough to feel bistable without snapping.
 */
private fun projectNecker(
    v: Triple<Float, Float, Float>,
    cx: Float,
    cy: Float,
    scale: Float,
    yaw: Float,
    flip: Float,
): Offset {
    val (x0, y0, z0) = v
    val depthBias = (1f - flip) * (-z0) * 0.12f + flip * z0 * 0.12f
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x = x0 * cosY - z0 * sinY
    val z = x0 * sinY + z0 * cosY + depthBias
    val px = (x - z) * cos(PI.toFloat() / 6f) * scale + cx
    val py = -(y0 + (x + z) * 0.34f) * scale + cy
    return Offset(px, py)
}
