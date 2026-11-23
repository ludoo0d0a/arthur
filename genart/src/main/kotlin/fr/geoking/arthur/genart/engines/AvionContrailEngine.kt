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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class AvionContrailMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient POV Avion Contrail — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun AvionContrailEngine(
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
            AvionContrailMote(
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
    val transition = rememberInfiniteTransition(label = "avioncontrail")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avioncontrail_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        drawRect(brush = Brush.verticalGradient(
            0f to TonalPalette.brightness(Color(0xFF5A90C8), brightness * dim * (0.92f + 0.08f * wash)),
            0.58f to TonalPalette.brightness(Color(0xFFB8D4EC), brightness * dim),
            1f to TonalPalette.brightness(Color(0xFF829FBD), brightness * dim),
        ))
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFF0F6FC), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // High-altitude cloud deck, with broad soft bands rather than a ground horizon.
        val cloudDrift = (loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 37) - 0.5f) * w * 0.025f
        for (i in 0..3) {
            val y = h * (0.56f + i * 0.105f) + (loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 51 + i * 13) - 0.5f) * h * 0.025f
            val bandAlpha = (0.09f - i * 0.012f) * dim
            drawOval(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.2f to TonalPalette.withAlpha(Color(0xFFF0F6FC), bandAlpha),
                    0.72f to TonalPalette.withAlpha(Color(0xFFE4EFF9), bandAlpha * 0.8f),
                    1f to Color.Transparent,
                    startX = -w * 0.15f + cloudDrift,
                    endX = w * 1.1f + cloudDrift,
                ),
                topLeft = Offset(-w * 0.08f + cloudDrift, y),
                size = Size(w * 1.16f, h * (0.10f + i * 0.012f)),
            )
        }
        val horizon = h * (0.52f + 0.025f * (wash - 0.5f))
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
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + time * 0.05f + 1f) % 1f * w
            val y = (mote.y0 + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.5f + 1f) % 1f * h
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFEAF4FC), (0.10f + 0.14f * n) * dim),
                radius = minOf(w, h) * (0.0015f + 0.002f * n),
                center = Offset(x, y),
            )
        }

        // A long white contrail curves gently across the sky and softens as it disperses.
        val bend = (loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 83) - 0.5f) * h * 0.035f
        val trail = Path().apply {
            moveTo(w * 0.08f, h * 0.29f + bend * 0.45f)
            cubicTo(w * 0.35f, h * 0.24f + bend, w * 0.63f, h * 0.35f - bend * 0.35f, w * 0.94f, h * 0.27f - bend * 0.15f)
        }
        drawPath(trail, color = TonalPalette.withAlpha(Color(0xFFF0F6FC), 0.13f * dim), style = Stroke(width = minOf(w, h) * 0.042f, cap = StrokeCap.Round))
        drawPath(trail, color = TonalPalette.withAlpha(Color(0xFFF0F6FC), 0.36f * dim), style = Stroke(width = minOf(w, h) * 0.014f, cap = StrokeCap.Round))
        drawPath(trail, color = TonalPalette.withAlpha(Color.White, 0.78f * dim), style = Stroke(width = minOf(w, h) * 0.0035f, cap = StrokeCap.Round))
        // Fine feathering wisps soften the trail edges.
        for (i in 0 until qualityCount(quality, low = 3, medium = 5, high = 8)) {
            val f = (i + 1f) / (qualityCount(quality, low = 3, medium = 5, high = 8) + 1f)
            val x = w * (0.12f + f * 0.74f)
            val y = h * (0.29f + 0.015f * sin(f * 2f * PI.toFloat()) + bend / h)
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFFF0F6FC), 0.13f * dim),
                start = Offset(x, y),
                end = Offset(x + w * 0.045f, y - h * 0.018f),
                strokeWidth = minOf(w, h) * 0.003f,
                cap = StrokeCap.Round,
            )
        }
    }
}
