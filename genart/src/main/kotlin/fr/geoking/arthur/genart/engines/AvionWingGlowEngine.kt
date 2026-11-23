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
        val drift = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 29) - 0.5f
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF080A0C), brightness * dim),
                    TonalPalette.brightness(Color(0xFF1A2840), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF2A3858), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD080), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // A soft cloud deck and a low distant horizon establish the cabin view.
        val horizon = h * (0.50f + 0.025f * (wash - 0.5f))
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF9BB5D0), 0.02f * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFFB5C4D4), 0.10f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF2A3858), 0.04f * dim),
                startY = horizon - h * 0.10f,
                endY = horizon + h * 0.22f,
            ),
        )
        for (i in 0..2) {
            val cloudY = horizon + h * (0.08f + i * 0.075f) + drift * h * 0.015f
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFB8C8D8), (0.14f - i * 0.025f) * dim),
                    1f to Color.Transparent,
                    center = Offset(w * (0.25f + 0.31f * i), cloudY),
                    radius = minDim * (0.40f + 0.05f * i),
                ),
                topLeft = Offset(w * (0.25f + 0.31f * i) - minDim * 0.40f, cloudY - minDim * 0.13f),
                size = Size(minDim * 0.80f, minDim * 0.26f),
            )
        }

        // Broad wing seen from the passenger window; its leading edge catches cabin light.
        val wing = Path().apply {
            moveTo(w * 0.39f, h * 0.78f)
            cubicTo(w * 0.55f, h * 0.66f, w * 0.76f, h * 0.54f, w * 0.95f, h * 0.52f)
            lineTo(w * 1.02f, h * 0.89f)
            cubicTo(w * 0.83f, h * 0.91f, w * 0.58f, h * 0.91f, w * 0.39f, h * 0.78f)
            close()
        }
        drawPath(
            path = wing,
            brush = Brush.linearGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF202B3C), brightness * dim),
                    TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim),
                ),
                start = Offset(w * 0.52f, h * 0.68f),
                end = Offset(w * 0.75f, h * 0.95f),
            ),
        )
        val leadingEdge = Path().apply {
            moveTo(w * 0.39f, h * 0.78f)
            cubicTo(w * 0.55f, h * 0.66f, w * 0.76f, h * 0.54f, w * 0.95f, h * 0.52f)
        }
        val glowX = w * (0.67f + drift * 0.035f)
        val glowY = h * (0.64f + 0.02f * sin(time * 2f * PI.toFloat()))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.32f * dim),
                0.45f to TonalPalette.withAlpha(accent, 0.10f * dim),
                1f to Color.Transparent,
                center = Offset(glowX, glowY),
                radius = minDim * 0.32f,
            ),
            radius = minDim * 0.32f,
            center = Offset(glowX, glowY),
        )
        drawPath(
            path = leadingEdge,
            color = TonalPalette.withAlpha(accent, (0.28f + 0.08f * wash) * dim),
            style = Stroke(width = minDim * 0.006f),
        )
        drawPath(
            path = leadingEdge,
            color = TonalPalette.withAlpha(Color(0xFFFFE7B0), 0.16f * dim),
            style = Stroke(width = minDim * 0.018f),
        )

        // Small navigation light glow near the wing tip, kept soft and restrained.
        val tip = Offset(w * 0.925f, h * 0.535f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFD080), (0.50f + 0.12f * wash) * dim),
                0.25f to TonalPalette.withAlpha(Color(0xFFFFD080), 0.18f * dim),
                1f to Color.Transparent,
                center = tip,
                radius = minDim * 0.14f,
            ),
            radius = minDim * 0.14f,
            center = tip,
        )
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.2f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + time * 0.04f + 1f) % 1f * w
            val y = (mote.y0 + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.25f + 1f) % 1f * h
            val r = minDim * mote.radius * 0.28f * (0.85f + 0.3f * n)
            drawCircle(
                color = TonalPalette.withAlpha(
                    TonalPalette.brightness(Color(0xFFDDE7F2), brightness * dim),
                    (0.035f + 0.055f * n) * dim,
                ),
                radius = r,
                center = Offset(x, y),
            )
        }
        // Dark curved window/cabin edge hints frame the view without a hard border.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.62f * dim),
                0.07f to Color.Transparent,
                0.92f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.48f * dim),
            ),
        )
    }
}
