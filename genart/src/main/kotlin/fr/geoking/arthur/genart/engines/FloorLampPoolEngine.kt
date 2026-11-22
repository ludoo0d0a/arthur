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

private data class FloorLampPoolMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient Floor Lamp Pool — seamless loop via loopedFbm. */
@Composable
internal fun FloorLampPoolEngine(
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
            FloorLampPoolMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.04f, 0.14f),
                driftX = seededRange(i * 53 + 13, 0.02f, 0.06f),
                driftY = seededRange(i * 67 + 19, 0.015f, 0.05f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "floorlamppool")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floorlamppool_t",
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
                    TonalPalette.brightness(Color(0xFF0E1014), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF1C2028), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD8A0), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // Soft focal glow
        val cx = w * (0.35f + 0.3f * loopedFbm(time, radius = 1.2f, seedOffset = 29))
        val cy = h * (0.3f + 0.25f * loopedFbm(time, radius = 1.4f, seedOffset = 47))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.35f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = minOf(w, h) * 0.55f,
            ),
            radius = minOf(w, h) * 0.55f,
            center = Offset(cx, cy),
        )
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.25f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + 1f) % 1f * w
            val y = (mote.y0 + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.5f + 1f) % 1f * h
            val r = minOf(w, h) * mote.radius * (0.85f + 0.3f * n)
            val col = TonalPalette.withAlpha(
                TonalPalette.brightness(TonalPalette.pick(paletteColors, mote.colorIndex), brightness * dim),
                (0.12f + 0.18f * n) * dim,
            )
            drawCircle(color = col, radius = r, center = Offset(x, y))
        }
        // Pack-flavored silhouette accents
        when ("interior") {
            "kaleido" -> {
                val rings = 4
                for (i in 0 until rings) {
                    val ang = time * 2f * PI.toFloat() * (0.15f + i * 0.03f)
                    val rr = minOf(w, h) * (0.12f + i * 0.08f)
                    for (k in 0 until 6) {
                        val a = ang + k * (2f * PI.toFloat() / 6f)
                        drawCircle(
                            color = TonalPalette.withAlpha(accent, 0.2f * dim),
                            radius = rr * 0.12f,
                            center = Offset(w * 0.5f + cos(a) * rr, h * 0.5f + sin(a) * rr),
                        )
                    }
                }
            }
            "zen" -> {
                drawCircle(
                    color = TonalPalette.withAlpha(accent, 0.22f * dim),
                    radius = minOf(w, h) * (0.18f + 0.02f * wash),
                    center = Offset(w * 0.5f, h * 0.52f),
                    style = Stroke(width = minOf(w, h) * 0.018f),
                )
            }
            "interior" -> {
                // soft vertical light well
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.45f to TonalPalette.withAlpha(accent, 0.18f * dim),
                        0.55f to TonalPalette.withAlpha(accent, 0.18f * dim),
                        1f to Color.Transparent,
                    ),
                    topLeft = Offset(w * 0.35f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.3f, h),
                )
            }
            "coast" -> {
                val horizon = h * (0.58f + 0.02f * wash)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to TonalPalette.withAlpha(accent, 0.25f * dim),
                        startY = horizon - h * 0.05f,
                        endY = h,
                    ),
                )
            }
            "fire" -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(accent, 0.55f * dim),
                        1f to Color.Transparent,
                        center = Offset(w * 0.5f, h * 0.72f),
                        radius = minOf(w, h) * 0.35f,
                    ),
                    radius = minOf(w, h) * 0.35f,
                    center = Offset(w * 0.5f, h * 0.72f),
                )
            }
            else -> {}
        }
    }
}
