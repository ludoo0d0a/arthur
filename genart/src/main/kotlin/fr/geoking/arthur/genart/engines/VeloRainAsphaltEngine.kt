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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class RainAsphaltStreak(
    val x: Float,
    val y: Float,
    val width: Float,
    val speed: Float,
    val phase: Float,
    val cool: Boolean,
)

/** Wet road reflections from a quiet bicycle POV, with no traffic simulation. */
@Composable
internal fun VeloRainAsphaltEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 10, medium = 18, high = 28)
    val streaks = remember(count) {
        List(count) { i ->
            RainAsphaltStreak(
                x = seededUnit(i * 17 + 3),
                y = seededRange(i * 29 + 7, 0.48f, 1.02f),
                width = seededRange(i * 41 + 11, 0.008f, 0.045f),
                speed = seededRange(i * 53 + 13, 0.32f, 0.82f),
                phase = seededUnit(i * 67 + 19),
                cool = i % 3 == 0,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "velorainasphalt")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "velorainasphalt_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 37)
        val horizonY = h * (0.43f + 0.012f * (wash - 0.5f))
        val cool = TonalPalette.brightness(Color(0xFF90B0C0), brightness * dim)
        val amber = TonalPalette.brightness(Color(0xFFC89A70), brightness * dim)
        val asphalt = TonalPalette.brightness(Color(0xFF14181C), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF2A3038), brightness * dim * (0.92f + 0.08f * wash)),
                0.43f to TonalPalette.brightness(Color(0xFF202830), brightness * dim),
                0.60f to asphalt,
                1f to TonalPalette.brightness(Color(0xFF080A0C), brightness * dim),
            ),
        )

        // Distant rain-softened glow above the road.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(cool, 0.14f * dim),
                startY = horizonY - h * 0.09f,
                endY = horizonY + h * 0.09f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(amber, 0.18f * dim),
                0.45f to TonalPalette.withAlpha(cool, 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.55f + 0.06f * (warp - 0.5f)), horizonY),
                radius = minDim * 0.34f,
            ),
            radius = minDim * 0.34f,
            center = Offset(w * (0.55f + 0.06f * (warp - 0.5f)), horizonY),
        )

        // Road shoulders and short center marks narrow into the distance.
        drawLine(
            color = TonalPalette.withAlpha(cool, 0.15f * dim),
            start = Offset(w * 0.47f, horizonY),
            end = Offset(-w * 0.16f, h),
            strokeWidth = minDim * 0.004f,
        )
        drawLine(
            color = TonalPalette.withAlpha(cool, 0.15f * dim),
            start = Offset(w * 0.53f, horizonY),
            end = Offset(w * 1.16f, h),
            strokeWidth = minDim * 0.004f,
        )
        for (i in 0 until 6) {
            val depth = (i + 1) / 7f
            val y = horizonY + (h - horizonY) * depth * depth
            val dashW = minDim * (0.004f + 0.010f * depth)
            val dashH = minDim * (0.008f + 0.026f * depth)
            drawRoundRect(
                color = TonalPalette.withAlpha(Color(0xFFB8C0C0), 0.13f * depth * dim),
                topLeft = Offset(w * 0.5f - dashW * 0.5f, y),
                size = Size(dashW, dashH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(dashW),
            )
        }

        // Wet asphalt carries broken, elongated reflections toward the viewer.
        streaks.forEachIndexed { index, streak ->
            val phase = (time * streak.speed + streak.phase) % 1f
            val depth = 0.18f + 0.82f * phase
            val y = horizonY + (h - horizonY) * depth * depth
            val spread = (streak.x - 0.5f) * (0.08f + 0.95f * depth)
            val x = w * (0.5f + spread) + sin((time + streak.phase) * 2f * PI.toFloat()) * minDim * 0.008f
            val streakW = minDim * streak.width * (0.28f + depth * 1.7f)
            val streakH = minDim * (0.003f + depth * 0.018f) * (0.75f + 0.25f * wash)
            val light = if (streak.cool) cool else amber
            val alpha = (0.05f + 0.16f * depth) * dim
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.42f to TonalPalette.withAlpha(light, alpha),
                    1f to Color.Transparent,
                    startX = x - streakW,
                    endX = x + streakW,
                ),
                topLeft = Offset(x - streakW, y),
                size = Size(streakW * 2f, streakH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(streakH),
            )
            if (index % 4 == 0) {
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFFC8D8E0), 0.12f * depth * dim),
                    radius = minDim * (0.0015f + 0.0025f * depth),
                    center = Offset(x, y + streakH * 1.6f),
                )
            }
        }

        // Fine rain stipple across the foreground asphalt.
        for (i in 0 until count) {
            val phase = (time * (0.18f + i % 5 * 0.025f) + streaks[i].phase) % 1f
            val depth = 0.35f + phase * 0.65f
            val x = w * (0.5f + (streaks[i].x - 0.5f) * depth * 1.8f)
            val y = horizonY + (h - horizonY) * depth
            drawLine(
                color = TonalPalette.withAlpha(cool, 0.08f * depth * dim),
                start = Offset(x, y),
                end = Offset(x - minDim * 0.002f, y + minDim * (0.008f + 0.012f * depth)),
                strokeWidth = minDim * 0.0012f,
            )
        }
    }
}
