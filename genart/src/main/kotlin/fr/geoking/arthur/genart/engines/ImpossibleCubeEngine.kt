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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Impossible cube frame — thick beams with one paradoxical overlap; soft highlights crawl edges.
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
    val beamSegments = remember { impossibleCubeSegments() }
    val strokeMul = when (quality) {
        GenartQuality.Low -> 0.85f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.15f
    }
    val transition = rememberInfiniteTransition(label = "impossible_cube")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
                    TonalPalette.brightness(Color(0xFF151825), brightness * 0.48f),
                    Color(0xFF080A10),
                    Color(0xFF020306),
                ),
                center = Offset(w * 0.5f, h * 0.48f),
                radius = maxOf(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.58f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.2f
        val base = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
        val beamW = scale * 0.22f * strokeMul
        val paradox = TonalPalette.brightness(TonalPalette.pick(paletteColors, 3), brightness)
        beamSegments.forEachIndexed { i, seg ->
            val a = projectIso(seg.ax, seg.ay, cx, cy, scale)
            val b = projectIso(seg.bx, seg.by, cx, cy, scale)
            val along = (i / beamSegments.size.toFloat() + phase01(sin01(time + seg.phase)) * 0.15f) % 1f
            val body = if (seg.paradox) paradox else base
            val shaded = TonalPalette.mix(body, Color.White, along * 0.28f)
            val strokeW = if (seg.paradox) beamW * 1.08f else beamW
            drawLine(
                color = TonalPalette.withAlpha(TonalPalette.mix(body, Color.Black, 0.25f), 0.7f * dim),
                start = a,
                end = b,
                strokeWidth = strokeW * 1.35f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = TonalPalette.withAlpha(shaded, 0.9f * dim),
                start = a,
                end = b,
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
            val hiT = (along * 0.85f + 0.075f).coerceIn(0.05f, 0.92f)
            val hi = Offset(
                a.x + (b.x - a.x) * hiT,
                a.y + (b.y - a.y) * hiT,
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, 0.32f * dim),
                radius = strokeW * 0.55f,
                center = hi,
            )
        }
        val outerVerts = listOf(
            projectIso(-1f, -1f, cx, cy, scale),
            projectIso(1f, -1f, cx, cy, scale),
            projectIso(1f, 1f, cx, cy, scale),
            projectIso(-1f, 1f, cx, cy, scale),
        )
        val outline = TonalPalette.withAlpha(TonalPalette.mix(base, Color.White, 0.12f), 0.25f * dim)
        for (j in outerVerts.indices) {
            val k = (j + 1) % outerVerts.size
            drawLine(
                color = outline,
                start = outerVerts[j],
                end = outerVerts[k],
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class ImpossibleCubeSeg(
    val ax: Float,
    val ay: Float,
    val bx: Float,
    val by: Float,
    val paradox: Boolean = false,
    val phase: Float = 0f,
)

private fun impossibleCubeSegments(): List<ImpossibleCubeSeg> = listOf(
    // Front square
    ImpossibleCubeSeg(-1f, -1f, 1f, -1f, phase = 0.1f),
    ImpossibleCubeSeg(1f, -1f, 1f, 1f, phase = 0.2f),
    ImpossibleCubeSeg(1f, 1f, -1f, 1f, phase = 0.3f),
    ImpossibleCubeSeg(-1f, 1f, -1f, -1f, phase = 0.4f),
    // Depth legs (offset for paradox)
    ImpossibleCubeSeg(-1f, -1f, -0.35f, -1.65f, phase = 0.5f),
    ImpossibleCubeSeg(1f, -1f, 1.65f, -0.35f, phase = 0.55f),
    ImpossibleCubeSeg(1f, 1f, 1.65f, 1.65f, phase = 0.6f),
    ImpossibleCubeSeg(-1f, 1f, -0.35f, 1.65f, phase = 0.65f),
    // Back partial frame
    ImpossibleCubeSeg(-0.35f, -1.65f, 1.65f, -0.35f, phase = 0.7f),
    ImpossibleCubeSeg(1.65f, -0.35f, 1.65f, 1.65f, phase = 0.75f),
    ImpossibleCubeSeg(1.65f, 1.65f, -0.35f, 1.65f, paradox = true, phase = 0.8f),
    ImpossibleCubeSeg(-0.35f, 1.65f, -0.35f, -1.65f, paradox = true, phase = 0.85f),
)

private fun projectIso(x: Float, y: Float, cx: Float, cy: Float, scale: Float): Offset {
    val px = (x - y) * cos(PI.toFloat() / 6f) * scale + cx
    val py = -(x + y) * 0.38f * scale + cy - y * scale * 0.12f
    return Offset(px, py)
}