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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class VeilLayer(
    val y: Float,
    val amplitude: Float,
    val width: Float,
    val phase: Float,
    val colorIndex: Int,
)

/** Soft Ambient Dream Veil — layered, slowly drifting luminous mist. */
@Composable
internal fun DreamVeilEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val veils = androidx.compose.runtime.remember(count) {
        List(count) { i ->
            VeilLayer(
                y = 0.16f + 0.68f * ((i + 0.5f) / count),
                amplitude = seededRange(i * 17 + 3, 0.035f, 0.11f),
                width = seededRange(i * 29 + 7, 0.34f, 0.62f),
                phase = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "dreamveil")
    val rawTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dreamveil_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(rawTime)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 13)
        val pulse = 0.88f + 0.12f * loopedFbm(time, radius = 1.5f, octaves = 2, seedOffset = 37)
        val deep = TonalPalette.brightness(Color(0xFF1A1430), brightness * dim)
        val mid = TonalPalette.brightness(Color(0xFF2A2048), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFC0A0E8), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        drawRect(brush = Brush.verticalGradient(listOf(deep, mid, Color(0xFF17142A))))

        // Diffuse, overlapping mist blooms keep the scene luminous without hard light sources.
        val focusX = w * (0.48f + 0.10f * (drift - 0.5f))
        val focusY = h * (0.43f + 0.06f * (loopedFbm(time, radius = 1.3f, seedOffset = 53) - 0.5f))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.25f * pulse * dim),
                0.48f to TonalPalette.withAlpha(Color(0xFF8670B8), 0.11f * dim),
                1f to Color.Transparent,
                center = Offset(focusX, focusY),
                radius = minDim * 0.62f,
            ),
            radius = minDim * 0.62f,
            center = Offset(focusX, focusY),
        )
        for (i in 0..2) {
            val x = w * (0.22f + i * 0.28f + 0.025f * sin(time * 2f * PI.toFloat() + i))
            val y = h * (0.28f + i * 0.20f)
            val radius = minDim * (0.23f + 0.04f * i)
            val color = if (i % 2 == 0) accent else Color(0xFF8870C0)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(color, 0.12f * pulse * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }

        veils.forEach { layer ->
            val layerTime = time * 2f * PI.toFloat() + layer.phase
            val centerY = h * (layer.y + layer.amplitude * 0.35f * sin(layerTime))
            val left = w * (-0.08f + 0.04f * cos(layerTime * 0.5f))
            val right = w * (1.08f + 0.04f * sin(layerTime * 0.5f))
            val path = Path().apply {
                moveTo(left, centerY)
                cubicTo(
                    w * 0.28f, centerY - h * layer.amplitude * (0.45f + 0.2f * drift),
                    w * 0.68f, centerY + h * layer.amplitude * (0.45f + 0.2f * drift),
                    right, centerY + h * layer.amplitude * 0.15f,
                )
            }
            val color = TonalPalette.mix(accent, TonalPalette.pick(paletteColors, layer.colorIndex), 0.18f)
            // Several broad, low-alpha strokes create a soft translucent fabric-like veil.
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(color, 0.035f * dim),
                style = Stroke(width = minDim * layer.width * 0.62f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(color, 0.065f * dim),
                style = Stroke(width = minDim * layer.width * 0.28f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(Color(0xFFE8DCFF), 0.08f * dim),
                style = Stroke(width = minDim * 0.012f, cap = StrokeCap.Round),
            )
        }
    }
}
