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
 * Endless staircase loop — four isometric flights around a rectangle with soft tread shadows,
 * a warm light sweeping the perimeter, and a slow breathing scale.
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
            animation = tween((12000 / speed).coerceIn(8000f, 30000f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "penrose_stairs_t",
    )
    val iso = remember { PenroseStairsIso() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val dim = if (isActive) 1f else 0.6f
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.brightness(Color(0xFF26324A), brightness * 0.62f),
                0.55f to TonalPalette.brightness(Color(0xFF182238), brightness * 0.55f),
                0.85f to Color(0xFF0B1220),
                1f to Color(0xFF05080F),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = maxOf(w, h) * 0.95f,
            ),
        )
        // Soft floor plane with a faint warm reflection below the stairs.
        val floorY = h * 0.8f
        val floorBrush = Brush.verticalGradient(
            colors = listOf(
                TonalPalette.brightness(Color(0xFF131B2B), brightness * dim * 0.9f),
                TonalPalette.brightness(Color(0xFF0B101B), brightness * dim * 0.9f),
            ),
        )
        drawRect(
            brush = floorBrush,
            topLeft = Offset(0f, floorY),
            size = Size(width = w, height = (h - floorY).coerceAtLeast(1f)),
        )

        val loop = phase01(t)
        val time = loop * 2f * PI.toFloat()
        val breath = 1f + 0.02f * (sin01(time) * 2f - 1f)
        val rotRad = (sin01(time * 0.83f) * 2f - 1f) * (PI.toFloat() / 180f)
        val cx = w * 0.5f
        val cy = h * 0.52f
        val ringHalfW = minDim * 0.44f
        val ringHalfH = minDim * 0.30f
        val stepW = minDim * 0.075f
        val stepH = stepW * 0.42f
        val rise = stepW * 0.24f
        val tread = TonalPalette.brightness(TonalPalette.mix(TonalPalette.pick(paletteColors, 1), Color.White, 0.24f), brightness * dim)
        val darkTread = TonalPalette.mix(tread, Color.Black, 0.38f)
        val litTread = TonalPalette.mix(tread, Color.White, 0.55f)
        val light = TonalPalette.mix(TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * dim * 1.3f), Color(0xFFFFF3E0), 0.35f)
        val washAngle = time * 0.25f
        var globalStep = 0
        for (side in 0 until 4) {
            for (s in 0 until stepsPerSide) {
                val progress = (globalStep + s) / (stepsPerSide * 4f)
                val wash = (0.5f + 0.5f * sin(washAngle + progress * 2f * PI.toFloat() - side * PI.toFloat() / 2f)).coerceIn(0f, 1f)
                val lit = (0.30f + 0.70f * wash).coerceIn(0f, 1f)
                val pts = iso.stepQuad(cx, cy, side, s, stepsPerSide, ringHalfW, ringHalfH, stepW, stepH, rise, breath, rotRad)
                val riser = TonalPalette.mix(TonalPalette.mix(tread, Color.Black, 0.30f), TonalPalette.mix(tread, Color.White, 0.5f), lit * 0.8f)
                val riserPath = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    lineTo(pts[1].x, pts[1].y)
                    lineTo(pts[2].x, pts[2].y)
                    lineTo(pts[3].x, pts[3].y)
                    close()
                }
                drawPath(path = riserPath, color = TonalPalette.withAlpha(riser, 0.95f * dim))
                val treadPath = Path().apply {
                    moveTo(pts[1].x, pts[1].y)
                    lineTo(pts[4].x, pts[4].y)
                    lineTo(pts[5].x, pts[5].y)
                    lineTo(pts[2].x, pts[2].y)
                    close()
                }
                drawPath(
                    path = treadPath,
                    color = TonalPalette.withAlpha(TonalPalette.mix(darkTread, litTread, lit), 0.95f * dim),
                )
                val shadowPath = Path().apply {
                    val lift = stepH * 0.15f
                    moveTo(pts[4].x, pts[4].y + lift)
                    lineTo(pts[5].x, pts[5].y + lift)
                    lineTo(pts[2].x, pts[2].y + lift + stepH * 0.05f)
                    lineTo(pts[1].x, pts[1].y + lift + stepH * 0.06f)
                    close()
                }
                drawPath(
                    path = shadowPath,
                    color = TonalPalette.withAlpha(Color.Black, (0.30f - 0.16f * lit).coerceIn(0.06f, 0.30f) * dim),
                )

                if (lit > 0.55f) {
                    val center = Offset((pts[1].x + pts[2].x) * 0.5f, (pts[1].y + pts[2].y) * 0.5f)
                    val glowRadius = minDim * 0.055f
                    val glowAlpha = ((lit - 0.55f) / 0.45f * 0.35f).coerceIn(0f, 0.35f) * dim
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TonalPalette.withAlpha(light, glowAlpha),
                                TonalPalette.withAlpha(TonalPalette.mix(light, Color.White, 0.4f), glowAlpha * 0.4f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = center,
                    )
                }
            }
            globalStep += stepsPerSide
        }
    }
}

private class PenroseStairsIso {
    private val cos30 = cos(PI.toFloat() / 6f)
    private val sin30 = sin(PI.toFloat() / 6f)

    fun project(x: Float, y: Float, scale: Float, rotRad: Float, cx: Float, cy: Float): Offset {
        val cosR = cos(rotRad)
        val sinR = sin(rotRad)
        val dx = x - cx
        val dy = y - cy
        val rx = dx * cosR - dy * sinR + cx
        val ry = dx * sinR + dy * cosR + cy
        val sx = cx + (rx - cx) * scale
        val sy = cy + (ry - cy) * scale
        return Offset(
            sx * cos30 - sy * sin30 * 0.35f,
            -sy * 0.55f + sx * sin30 * 0.18f,
        )
    }

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
        scale: Float,
        rotRad: Float,
    ): List<Offset> {
        val t0 = step.toFloat() / total
        val t1 = (step + 1f) / total
        val (ax0, ay0, ax1, ay1) = sideSpan(side, t0, t1, halfW, halfH, cx, cy)
        val riseOff = step * rise
        val p0 = project(ax0, ay0 + riseOff, scale, rotRad, cx, cy)
        val p1 = project(ax1, ay1 + riseOff, scale, rotRad, cx, cy)
        val p2 = project(ax1, ay1 + riseOff + stepH, scale, rotRad, cx, cy)
        val p3 = project(ax0, ay0 + riseOff + stepH, scale, rotRad, cx, cy)
        val p4 = project(ax0 + (ax1 - ax0) * 0.15f, ay0 + riseOff - stepW * 0.35f, scale, rotRad, cx, cy)
        val p5 = project(ax1 - (ax1 - ax0) * 0.15f, ay1 + riseOff - stepW * 0.35f, scale, rotRad, cx, cy)
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
}
