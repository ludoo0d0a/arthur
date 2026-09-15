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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Floating geometric picture pieces — rounded rects, diamonds, capsules, triangles —
 * with paper-cut shadows. Shape kind + color are seeded-random; [quality] tweaks density.
 */
@Composable
internal fun TonalGeometryEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 10, medium = 16, high = 24)
    val shadowMul = when (quality) {
        GenartQuality.Low -> 0.55f
        GenartQuality.Medium -> 0.85f
        GenartQuality.High -> 1.1f
    }
    val shapes = remember(count) {
        List(count) { i ->
            TonalGeomSeed(
                kind = (seededUnit(i * 11 + 2) * 5f).toInt().coerceIn(0, 4),
                x = seededUnit(i * 13 + 1),
                y = seededUnit(i * 17 + 3),
                size = seededRange(i * 23 + 5, 0.07f, 0.24f),
                aspect = seededRange(i * 27 + 9, 0.55f, 1.9f),
                rot = seededUnit(i * 29 + 7) * 2f * PI.toFloat(),
                colorIndex = (seededUnit(i * 31 + 8) * 8).toInt(),
                colorMix = seededUnit(i * 33 + 10),
                morph = seededUnit(i * 37 + 11),
                corner = seededRange(i * 41 + 13, 0.28f, 0.5f),
                alpha = seededRange(i * 43 + 15, 0.45f, 0.85f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "tonalgeometry")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tonalgeometry_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val bg = TonalPalette.mix(TonalPalette.pick(paletteColors, 0), Color(0xFF0F172A), 0.9f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    TonalPalette.brightness(bg, brightness * 0.45f),
                    Color(0xFF1E1B4B),
                    Color(0xFF020617),
                ),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        val time = phase01(t)
        val reshuffle = (time * 4f).toInt()
        val dim = if (isActive) 1f else 0.55f
        val shadowPx = (minDim * 0.016f * shadowMul).coerceIn(4f, 24f)
        shapes.forEachIndexed { index, seed ->
            val phaseSeed = reshuffle * 97 + index * 13
            val x = lerp(
                seed.x,
                seededUnit(phaseSeed + 1),
                0.28f + 0.18f * sin01(time * 2f * PI.toFloat() + seed.morph),
            )
            val y = lerp(
                seed.y,
                seededUnit(phaseSeed + 2),
                0.28f + 0.18f * sin01(time * 2f * PI.toFloat() + seed.rot),
            )
            val scale = seed.size * (0.78f + 0.28f * sin01(time * 2f * PI.toFloat() + seed.morph * 4f))
            val cx = x * w
            val cy = y * h
            val dimSize = scale * minDim
            val cA = TonalPalette.pick(paletteColors, seed.colorIndex)
            val cB = TonalPalette.pick(paletteColors, seed.colorIndex + 1)
            val color = TonalPalette.brightness(TonalPalette.mix(cA, cB, seed.colorMix), brightness)
            val fill = TonalPalette.withAlpha(color, seed.alpha * dim)
            val shadow = TonalPalette.withAlpha(Color.Black, 0.30f * dim * shadowMul)
            val rotDeg = Math.toDegrees((seed.rot + time * 2f * PI.toFloat() * 0.12f).toDouble()).toFloat()
            rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
                when (seed.kind) {
                    0 -> { // rounded rect
                        val bw = dimSize
                        val bh = dimSize * 0.75f
                        drawRoundRect(
                            color = shadow,
                            topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.5f, cy - bh * 0.5f + shadowPx),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bw * seed.corner, bh * seed.corner),
                        )
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bw * seed.corner, bh * seed.corner),
                        )
                    }
                    1 -> { // triangle
                        val r = dimSize * 0.55f
                        val path = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r * 0.92f, cy + r * 0.72f)
                            lineTo(cx - r * 0.92f, cy + r * 0.72f)
                            close()
                        }
                        val shadowPath = Path().apply {
                            addPath(path, Offset(shadowPx * 0.45f, shadowPx))
                        }
                        drawPath(path = shadowPath, color = shadow)
                        drawPath(path = path, color = fill)
                    }
                    2 -> { // circle
                        drawCircle(
                            color = shadow,
                            radius = dimSize * 0.45f,
                            center = Offset(cx + shadowPx * 0.4f, cy + shadowPx),
                        )
                        drawCircle(color = fill, radius = dimSize * 0.45f, center = Offset(cx, cy))
                    }
                    3 -> { // diamond / rhombus
                        val rx = dimSize * 0.48f
                        val ry = dimSize * 0.62f
                        val path = Path().apply {
                            moveTo(cx, cy - ry)
                            lineTo(cx + rx, cy)
                            lineTo(cx, cy + ry)
                            lineTo(cx - rx, cy)
                            close()
                        }
                        drawPath(
                            path = Path().apply { addPath(path, Offset(shadowPx * 0.4f, shadowPx)) },
                            color = shadow,
                        )
                        drawPath(path = path, color = fill)
                    }
                    else -> { // capsule
                        val bw = dimSize * seed.aspect
                        val bh = dimSize * 0.55f
                        drawRoundRect(
                            color = shadow,
                            topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.5f, cy - bh * 0.5f + shadowPx),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bh * 0.5f, bh * 0.5f),
                        )
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bh * 0.5f, bh * 0.5f),
                        )
                    }
                }
            }
        }
    }
}

private data class TonalGeomSeed(
    val kind: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val aspect: Float,
    val rot: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val morph: Float,
    val corner: Float,
    val alpha: Float,
)
