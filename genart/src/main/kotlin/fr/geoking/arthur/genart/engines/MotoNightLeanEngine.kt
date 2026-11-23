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

private data class RoadStreak(
    val depth: Float,
    val side: Float,
    val phase: Float,
    val length: Float,
    val colorIndex: Int,
)

/** Soft Ambient POV Moto Night Lean — a gentle, seamless night turn. */
@Composable
internal fun MotoNightLeanEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 9, high = 14)
    val streaks = List(count) { i ->
        RoadStreak(
            depth = seededUnit(i * 31 + 9),
            side = if (i % 2 == 0) -1f else 1f,
            phase = seededUnit(i * 47 + 13),
            length = seededRange(i * 59 + 17, 0.035f, 0.11f),
            colorIndex = i,
        )
    }
    val transition = rememberInfiniteTransition(label = "motonightlean")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "motonightlean_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val noise = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val lean = sin(time * 2f * PI.toFloat()) * 0.11f + (noise - 0.5f) * 0.045f
        val horizonY = h * (0.52f + (noise - 0.5f) * 0.025f)
        val horizonX = w * (0.5f + lean * 0.45f)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF6080C0), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )
        val cool = TonalPalette.brightness(Color(0xFF181E28), brightness * dim)
        val asphalt = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF0C1018), brightness * dim * (0.94f + noise * 0.06f)),
                0.52f to cool,
                1f to asphalt,
            ),
        )

        // Distant halo and low, dark scenery frame the road without hard lights.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(accent, 0.18f * dim),
                startY = horizonY - h * 0.11f,
                endY = horizonY + h * 0.055f,
            ),
        )
        val farRidge = Path().apply {
            moveTo(0f, horizonY + h * 0.025f)
            cubicTo(w * 0.18f, horizonY - h * 0.025f, w * 0.27f, horizonY + h * 0.08f, horizonX, horizonY + h * 0.035f)
            cubicTo(w * 0.72f, horizonY - h * 0.015f, w * 0.86f, horizonY + h * 0.07f, w, horizonY + h * 0.01f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(farRidge, color = TonalPalette.withAlpha(Color(0xFF080A10), 0.52f * dim))

        // Leaning road: its vanishing point drifts gently while both shoulders arc together.
        val leftEdge = Path().apply {
            moveTo(horizonX - w * 0.018f, horizonY)
            cubicTo(horizonX - w * 0.11f + lean * w * 0.10f, h * 0.66f, w * 0.20f + lean * w * 0.30f, h * 0.84f, -w * 0.08f, h * 1.04f)
        }
        val rightEdge = Path().apply {
            moveTo(horizonX + w * 0.018f, horizonY)
            cubicTo(horizonX + w * 0.11f + lean * w * 0.10f, h * 0.66f, w * 0.80f + lean * w * 0.30f, h * 0.84f, w * 1.08f, h * 1.04f)
        }
        val road = Path().apply {
            addPath(leftEdge)
            cubicTo(w * 0.20f + lean * w * 0.30f, h * 0.84f, w * 0.80f + lean * w * 0.30f, h * 0.84f, horizonX + w * 0.018f, horizonY)
            close()
        }
        drawPath(
            road,
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF202834), 0.78f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF05070A), 0.97f * dim),
                startY = horizonY,
                endY = h,
            ),
        )

        // Soft shoulder lines bend into the turn; the outer shoulder moves a little faster.
        drawPath(leftEdge, color = TonalPalette.withAlpha(accent, 0.28f * dim), style = Stroke(width = minDim * 0.004f))
        drawPath(rightEdge, color = TonalPalette.withAlpha(accent, 0.24f * dim), style = Stroke(width = minDim * 0.0035f))

        // Dashed center line follows the same curved perspective path.
        val centerLine = Path().apply {
            moveTo(horizonX, horizonY)
            cubicTo(horizonX + lean * w * 0.10f, h * 0.67f, w * 0.50f + lean * w * 0.30f, h * 0.84f, w * (0.50f + lean * 0.30f), h * 1.02f)
        }
        drawPath(centerLine, color = TonalPalette.withAlpha(Color(0xFF9AAAC8), 0.32f * dim), style = Stroke(width = minDim * 0.004f))

        // Roadside light trails slide through the bend in a seamless loop.
        streaks.forEach { streak ->
            val travel = (streak.phase + time * (0.45f + streak.depth * 0.25f)) % 1f
            val depth = (0.12f + travel * 0.88f)
            val perspective = depth * depth
            val center = horizonX + lean * w * perspective * 0.45f
            val roadHalf = w * (0.025f + perspective * 0.57f)
            val x = center + streak.side * (roadHalf * (1.18f + streak.depth * 0.38f))
            val y = horizonY + (h - horizonY) * perspective
            val dx = streak.side * (w * streak.length * (0.20f + perspective))
            val dy = h * streak.length * (0.12f + perspective * 0.65f)
            val trailColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, streak.colorIndex), brightness * dim)
            drawLine(
                color = TonalPalette.withAlpha(trailColor, (0.12f + 0.16f * perspective) * dim),
                start = Offset(x - dx * 0.18f, y - dy * 0.25f),
                end = Offset(x + dx, y + dy),
                strokeWidth = minDim * (0.0015f + perspective * 0.003f),
            )
            drawCircle(
                color = TonalPalette.withAlpha(trailColor, 0.24f * dim),
                radius = minDim * (0.002f + perspective * 0.004f),
                center = Offset(x, y),
            )
        }

        // A soft headlamp wash lights the near road and a faint tail glow recedes behind.
        val lampX = w * (0.5f + lean * 0.24f)
        val lampY = h * 0.82f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.16f * dim),
                0.55f to TonalPalette.withAlpha(accent, 0.055f * dim),
                1f to Color.Transparent,
                center = Offset(lampX, lampY),
                radius = minDim * 0.62f,
            ),
            radius = minDim * 0.62f,
            center = Offset(lampX, lampY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFC06068), 0.10f * dim),
                1f to Color.Transparent,
                center = Offset(horizonX, horizonY + h * 0.01f),
                radius = minDim * 0.16f,
            ),
            radius = minDim * 0.16f,
            center = Offset(horizonX, horizonY + h * 0.01f),
        )
    }
}
