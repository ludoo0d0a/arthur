package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Close-up Saturn: banded gas giant with a wide multi-lane ring plane, over a soft starfield.
 */
@Composable
internal fun SaturnRingsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bandCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val ringCount = qualityCount(quality, low = 3, medium = 4, high = 6)
    val bands = remember(bandCount) {
        List(bandCount) { i ->
            SaturnBandSeed(
                yFrac = seededRange(i * 37 + 5, -0.55f, 0.55f),
                heightFrac = seededRange(i * 53 + 11, 0.06f, 0.14f),
                alpha = seededRange(i * 71 + 17, 0.12f, 0.28f),
                colorIndex = i,
            )
        }
    }
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            SaturnRingLaneSeed(
                radiusMul = 1.55f + i * 0.28f,
                strokeFrac = seededRange(i * 41 + 7, 0.04f, 0.09f),
                alpha = seededRange(i * 61 + 13, 0.25f, 0.55f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "saturn_rings")
    val spinT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "saturn_rings_spin",
    )
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "saturn_rings_glow",
    )

    val dim = if (isActive) 1f else 0.55f

    Box(modifier = modifier) {
        StarFieldEngine(isActive, paletteColors, quality, brightness, speed * 0.4f, Modifier.fillMaxSize())

        Canvas(modifier = Modifier.fillMaxSize().blur(14.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.52f
            val bodyR = minDim * 0.22f
            val pulse = 0.85f + 0.15f * sin01(phase01(glowT) * 2f * PI.toFloat())
            val atm = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFE8C898), TonalPalette.pick(paletteColors, 0), 0.3f),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(atm, 0.35f * dim * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = bodyR * 1.55f,
                ),
                radius = bodyR * 1.55f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.52f
            val bodyR = minDim * 0.22f
            val bandDrift = phase01(spinT)
            val bodyTint = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFD4B07A), TonalPalette.pick(paletteColors, 0), 0.35f),
                brightness,
            )
            val ringTint = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFC8B8A0), TonalPalette.pick(paletteColors, 1), 0.4f),
                brightness,
            )

            // Far half of rings (behind planet)
            rings.forEach { lane ->
                val rx = bodyR * lane.radiusMul
                val ry = rx * 0.22f
                drawOval(
                    color = TonalPalette.withAlpha(ringTint, lane.alpha * 0.55f * dim),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = bodyR * lane.strokeFrac),
                )
            }

            // Planet body
            drawCircle(color = Color(0xFF1A140E), radius = bodyR, center = Offset(cx, cy))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(bodyTint, 0.95f * dim), bodyTint.copy(alpha = 0.7f * dim)),
                    center = Offset(cx - bodyR * 0.35f, cy - bodyR * 0.35f),
                    radius = bodyR * 1.35f,
                ),
                radius = bodyR,
                center = Offset(cx, cy),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF0A0806).copy(alpha = 0.75f * dim)),
                    center = Offset(cx + bodyR * 0.45f, cy + bodyR * 0.4f),
                    radius = bodyR * 1.3f,
                ),
                radius = bodyR,
                center = Offset(cx, cy),
            )

            clipPath(Path().apply { addOval(Rect(Offset(cx - bodyR, cy - bodyR), Size(bodyR * 2f, bodyR * 2f))) }) {
                bands.forEach { band ->
                    val by = cy + band.yFrac * bodyR
                    val bh = band.heightFrac * bodyR
                    val driftX = (bandDrift - 0.5f) * bodyR * 0.4f
                    val tint = TonalPalette.brightness(TonalPalette.pick(paletteColors, band.colorIndex), brightness)
                    drawOval(
                        color = TonalPalette.withAlpha(tint, band.alpha * dim),
                        topLeft = Offset(cx - bodyR + driftX, by - bh * 0.5f),
                        size = Size(bodyR * 2f, bh),
                    )
                }
            }

            // Near half of rings (in front) — slightly brighter
            rings.forEach { lane ->
                val rx = bodyR * lane.radiusMul
                val ry = rx * 0.22f
                // Draw only the lower arc by overlapping a thicker bottom stroke feel via full oval + lighter body occlusion already done
                drawOval(
                    color = TonalPalette.withAlpha(ringTint, lane.alpha * dim),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = bodyR * lane.strokeFrac * 0.85f),
                )
            }
            // Re-draw lower planet crescent so rings pass behind the disc visually
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(bodyTint, 0.55f * dim), Color.Transparent),
                    center = Offset(cx, cy + bodyR * 0.15f),
                    radius = bodyR * 0.95f,
                ),
                radius = bodyR * 0.92f,
                center = Offset(cx, cy + bodyR * 0.08f),
            )
        }
    }
}

private data class SaturnBandSeed(
    val yFrac: Float,
    val heightFrac: Float,
    val alpha: Float,
    val colorIndex: Int,
)

private data class SaturnRingLaneSeed(
    val radiusMul: Float,
    val strokeFrac: Float,
    val alpha: Float,
    val colorIndex: Int,
)
