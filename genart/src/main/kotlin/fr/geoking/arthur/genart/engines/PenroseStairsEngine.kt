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
 * Endless staircase loop — four isometric flights around a rectangle with soft tread shadows
 * and a slow light wash traveling the perimeter.
 */
@Composable
internal fun PenroseStairsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val stepsPerSide = qualityCount(quality, low = 6, medium = 9, high = 12)
    val transition = rememberInfiniteTransition(label = "penrose_stairs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "penrose_stairs_t",
    )
    val iso = remember { PenroseStairsIso() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF1E293B), brightness * 0.5f),
                    Color(0xFF0F172A),
                    Color(0xFF020617),
                ),
                center = Offset(w * 0.5f, h * 0.44f),
                radius = maxOf(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.6f
        val cx = w * 0.5f
        val cy = h * 0.52f
        val stepW = minDim * 0.042f
        val stepH = stepW * 0.38f
        val rise = stepW * 0.22f
        val ringHalfW = minDim * 0.34f
        val ringHalfH = minDim * 0.26f
        val tread = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
        val riser = TonalPalette.mix(tread, Color.Black, 0.22f)
        val highlight = TonalPalette.mix(tread, Color.White, 0.18f)
        val washAngle = time * 0.25f
        var globalStep = 0
        for (side in 0 until 4) {
            for (s in 0 until stepsPerSide) {
                val progress = (globalStep + s) / (stepsPerSide * 4f)
                val wash = 0.5f + 0.5f * sin(washAngle + progress * 2f * PI.toFloat() - side * PI.toFloat() / 2f)
                val pts = iso.stepQuad(cx, cy, side, s, stepsPerSide, ringHalfW, ringHalfH, stepW, stepH, rise)
                val shade = TonalPalette.mix(tread, highlight, (wash * 0.35f).coerceIn(0f, 0.35f))
                val riserPath = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    lineTo(pts[1].x, pts[1].y)
                    lineTo(pts[2].x, pts[2].y)
                    lineTo(pts[3].x, pts[3].y)
                    close()
                }
                drawPath(
                    path = riserPath,
                    color = TonalPalette.withAlpha(riser, 0.88f * dim),
                )
                val treadPath = Path().apply {
                    moveTo(pts[1].x, pts[1].y)
                    lineTo(pts[4].x, pts[4].y)
                    lineTo(pts[5].x, pts[5].y)
                    lineTo(pts[2].x, pts[2].y)
                    close()
                }
                drawPath(
                    path = treadPath,
                    color = TonalPalette.withAlpha(shade, 0.92f * dim),
                )
                val shadowPath = Path().apply {
                    moveTo(pts[4].x, pts[4].y + stepH * 0.15f)
                    lineTo(pts[5].x, pts[5].y + stepH * 0.15f)
                    lineTo(pts[2].x, pts[2].y + stepH * 0.2f)
                    lineTo(pts[1].x, pts[1].y + stepH * 0.18f)
                    close()
                }
                drawPath(
                    path = shadowPath,
                    color = TonalPalette.withAlpha(Color.Black, 0.22f * dim * wash.coerceIn(0.4f, 1f)),
                )
            }
            globalStep += stepsPerSide
        }
    }
}

private class PenroseStairsIso {
    private val cos30 = cos(PI.toFloat() / 6f)
    private val sin30 = sin(PI.toFloat() / 6f)

    fun stepQuad(
        cx: Float,
        cy: Float,
        side: Int,
        step: Int,
        total: Int,
        halfW: Float,
        halfH: Float,
        stepW: Float,
        stepH: Float,
        rise: Float,
    ): List<Offset> {
        val t0 = step.toFloat() / total
        val t1 = (step + 1f) / total
        val (ax0, ay0, ax1, ay1) = sideSpan(side, t0, t1, halfW, halfH, cx, cy)
        val riseOff = step * rise
        val p0 = iso(ax0, ay0 + riseOff)
        val p1 = iso(ax1, ay1 + riseOff)
        val p2 = iso(ax1, ay1 + riseOff + stepH)
        val p3 = iso(ax0, ay0 + riseOff + stepH)
        val p4 = iso(ax0 + (ax1 - ax0) * 0.15f, ay0 + riseOff - stepW * 0.35f)
        val p5 = iso(ax1 - (ax1 - ax0) * 0.15f, ay1 + riseOff - stepW * 0.35f)
        return listOf(p0, p1, p2, p3, p4, p5)
    }

    private fun sideSpan(
        side: Int,
        t0: Float,
        t1: Float,
        halfW: Float,
        halfH: Float,
        cx: Float,
        cy: Float,
    ): FloatArray {
        return when (side) {
            0 -> {
                val x0 = cx - halfW + t0 * halfW * 2f
                val x1 = cx - halfW + t1 * halfW * 2f
                floatArrayOf(x0, cy - halfH, x1, cy - halfH)
            }
            1 -> {
                val y0 = cy - halfH + t0 * halfH * 2f
                val y1 = cy - halfH + t1 * halfH * 2f
                floatArrayOf(cx + halfW, y0, cx + halfW, y1)
            }
            2 -> {
                val x0 = cx + halfW - t0 * halfW * 2f
                val x1 = cx + halfW - t1 * halfW * 2f
                floatArrayOf(x0, cy + halfH, x1, cy + halfH)
            }
            else -> {
                val y0 = cy + halfH - t0 * halfH * 2f
                val y1 = cy + halfH - t1 * halfH * 2f
                floatArrayOf(cx - halfW, y0, cx - halfW, y1)
            }
        }
    }

    private fun iso(x: Float, y: Float): Offset =
        Offset(
            x * cos30 - y * sin30 * 0.35f,
            -y * 0.55f + x * sin30 * 0.18f,
        )
}