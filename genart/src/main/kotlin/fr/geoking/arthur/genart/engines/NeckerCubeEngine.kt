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

/**
 * Necker cube wireframe — bistable depth flips with soft glow on the active front edges.
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
        GenartQuality.Low -> 2f
        GenartQuality.Medium -> 2.8f
        GenartQuality.High -> 3.6f
    }
    val transition = rememberInfiniteTransition(label = "necker_cube")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
                    TonalPalette.brightness(Color(0xFF141820), brightness * 0.5f),
                    Color(0xFF0A0C12),
                    Color(0xFF030408),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.9f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val flip = (sin01(time) + 1f) * 0.5f
        val dim = if (isActive) 1f else 0.58f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.22f
        val accent = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
        val ghost = TonalPalette.mix(accent, Color(0xFF64748B), 0.45f)
        val screenVerts = verts.map { projectNecker(it, cx, cy, scale) }
        edges.forEach { edge ->
            val aFlip = if (edge.interpA) flip else 1f - flip
            val bFlip = if (edge.interpB) flip else 1f - flip
            val frontBias = (aFlip + bFlip) * 0.5f
            val alphaBack = 0.22f + 0.18f * (1f - frontBias)
            val alphaFront = 0.55f + 0.4f * frontBias
            val p0 = screenVerts[edge.i0]
            val p1 = screenVerts[edge.i1]
            drawLine(
                color = TonalPalette.withAlpha(ghost, alphaBack * dim),
                start = p0,
                end = p1,
                strokeWidth = strokeBase,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = TonalPalette.withAlpha(accent, alphaFront * dim),
                start = p0,
                end = p1,
                strokeWidth = strokeBase * (0.85f + 0.35f * frontBias),
                cap = StrokeCap.Round,
            )
            if (frontBias > 0.62f) {
                val mid = Offset((p0.x + p1.x) * 0.5f, (p0.y + p1.y) * 0.5f)
                drawCircle(
                    color = TonalPalette.withAlpha(TonalPalette.mix(accent, Color.White, 0.35f), 0.28f * dim * frontBias),
                    radius = strokeBase * 1.8f,
                    center = mid,
                )
            }
        }
    }
}

private data class NeckerEdge(val i0: Int, val i1: Int, val interpA: Boolean, val interpB: Boolean)

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
    NeckerEdge(0, 1, true, true),
    NeckerEdge(1, 2, true, true),
    NeckerEdge(2, 3, true, true),
    NeckerEdge(3, 0, true, true),
    NeckerEdge(4, 5, false, false),
    NeckerEdge(5, 6, false, false),
    NeckerEdge(6, 7, false, false),
    NeckerEdge(7, 4, false, false),
    NeckerEdge(0, 4, true, false),
    NeckerEdge(1, 5, true, false),
    NeckerEdge(2, 6, true, false),
    NeckerEdge(3, 7, true, false),
)

private fun projectNecker(v: Triple<Float, Float, Float>, cx: Float, cy: Float, scale: Float): Offset {
    val (x, y, z) = v
    val px = (x - z) * cos(PI.toFloat() / 6f) * scale + cx
    val py = -(y + (x + z) * 0.35f) * scale + cy
    return Offset(px, py)
}