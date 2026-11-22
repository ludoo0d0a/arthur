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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class AvionWingGlowMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient POV Avion Wing Glow — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun AvionWingGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = remember(count) {
        List(count) { i ->
            AvionWingGlowMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.03f, 0.12f),
                driftX = seededRange(i * 53 + 13, 0.02f, 0.08f),
                driftY = seededRange(i * 67 + 19, 0.01f, 0.04f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "avionwingglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avionwingglow_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF1A2840), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF2A3858), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD080), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // Soft horizon / vanishing point
        val horizon = h * (0.55f + 0.04f * (wash - 0.5f))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(accent, 0.12f * dim),
                startY = horizon - h * 0.08f,
                endY = horizon + h * 0.08f,
            ),
        )
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.2f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + time * 0.15f + 1f) % 1f * w
            val y = (mote.y0 + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.5f + 1f) % 1f * h
            val r = minOf(w, h) * mote.radius * (0.85f + 0.3f * n)
            drawCircle(
                color = TonalPalette.withAlpha(
                    TonalPalette.brightness(TonalPalette.pick(paletteColors, mote.colorIndex), brightness * dim),
                    (0.10f + 0.16f * n) * dim,
                ),
                radius = r,
                center = Offset(x, y),
            )
        }
        when ("avion") {
            "auto", "moto" -> {
                // soft dash silhouette band
                drawRect(
                    color = TonalPalette.withAlpha(Color(0xFF080A0C), 0.45f * dim),
                    topLeft = Offset(0f, h * 0.72f),
                    size = Size(w, h * 0.28f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(accent, 0.4f * dim),
                        1f to Color.Transparent,
                        center = Offset(w * 0.5f, h * 0.62f),
                        radius = minOf(w, h) * 0.25f,
                    ),
                    radius = minOf(w, h) * 0.25f,
                    center = Offset(w * 0.5f, h * 0.62f),
                )
            }
            "avion" -> {
                // soft wing edge
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to TonalPalette.withAlpha(accent, 0.2f * dim),
                        1f to Color.Transparent,
                    ),
                    topLeft = Offset(0f, h * 0.62f),
                    size = Size(w * 0.55f, h * 0.08f),
                )
            }
            "velo" -> {
                // path ribbon
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to TonalPalette.withAlpha(accent, 0.22f * dim),
                    ),
                    topLeft = Offset(w * 0.42f, horizon),
                    size = Size(w * 0.16f, h - horizon),
                )
            }
            "train" -> {
                // window frame bars
                for (i in 1..2) {
                    val x = w * (0.28f * i)
                    drawRect(
                        color = TonalPalette.withAlpha(Color(0xFF0A0C10), 0.35f * dim),
                        topLeft = Offset(x, 0f),
                        size = Size(w * 0.02f, h),
                    )
                }
            }
            "us_vintage" -> {
                // neon arc
                drawCircle(
                    color = TonalPalette.withAlpha(accent, 0.35f * dim),
                    radius = minOf(w, h) * 0.22f,
                    center = Offset(w * 0.5f, h * 0.38f),
                    style = Stroke(width = minOf(w, h) * 0.02f),
                )
            }
            else -> {}
        }
    }
}
