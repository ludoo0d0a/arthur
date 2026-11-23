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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient Coast Cliff Haze — layered cliffs fading into a softly moving sea. */
@Composable
internal fun CoastCliffHazeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "coastcliffhaze")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coastcliffhaze_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 19)
        val haze = loopedFbm(time, radius = 1.6f, octaves = 2, seedOffset = 47)
        val paletteMix = TonalPalette.mix(Color(0xFFC8D4E0), TonalPalette.pick(paletteColors, 0), 0.28f)
        val skyTop = TonalPalette.brightness(Color(0xFF6A7A88), brightness * dim)
        val seaBase = TonalPalette.brightness(Color(0xFF3A4854), brightness * dim)
        val cliffFar = TonalPalette.brightness(Color(0xFF586570), brightness * dim)
        val cliffNear = TonalPalette.brightness(Color(0xFF343B43), brightness * dim)
        val accent = TonalPalette.brightness(paletteMix, brightness * dim)
        val horizon = h * 0.52f

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, seaBase), endY = h))

        // Distant headlands softened by sea mist.
        val farCliffs = Path().apply {
            moveTo(0f, horizon + h * 0.02f)
            cubicTo(w * 0.12f, horizon - h * 0.23f, w * 0.24f, horizon - h * 0.16f, w * 0.37f, horizon - h * 0.04f)
            cubicTo(w * 0.52f, horizon - h * 0.20f, w * 0.68f, horizon - h * 0.24f, w * 0.82f, horizon - h * 0.08f)
            cubicTo(w * 0.91f, horizon - h * 0.01f, w * 0.96f, horizon - h * 0.03f, w, horizon - h * 0.09f)
            lineTo(w, h * 0.68f)
            lineTo(0f, h * 0.68f)
            close()
        }
        drawPath(farCliffs, TonalPalette.withAlpha(cliffFar, 0.52f * dim))

        // Broad haze bank and distant light where the water meets the sky.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.44f to TonalPalette.withAlpha(accent, (0.10f + 0.04f * haze) * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.12f,
                endY = horizon + h * 0.18f,
            ),
            topLeft = Offset(0f, horizon - h * 0.12f),
            size = Size(w, h * 0.30f),
        )
        val glowCenter = Offset(w * (0.53f + (drift - 0.5f) * 0.08f), horizon + h * 0.01f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.19f * dim),
                0.45f to TonalPalette.withAlpha(accent, 0.07f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = minDim * 0.52f,
            ),
            radius = minDim * 0.52f,
            center = glowCenter,
        )

        // Sea bands are faint and horizontal, broken by haze.
        for (i in 0 until detailCount) {
            val f = (i + 1f) / (detailCount + 1f)
            val y = horizon + h * (0.06f + f * 0.25f)
            val sway = sin(time * 2f * PI.toFloat() + i * 1.3f) * minDim * 0.014f
            val alpha = (0.07f + 0.10f * (1f - f)) * dim
            drawLine(
                color = TonalPalette.withAlpha(accent, alpha),
                start = Offset(w * 0.18f + sway, y),
                end = Offset(w * 0.84f + sway, y + h * 0.008f),
                strokeWidth = minDim * (0.0015f + 0.001f * (1f - f)),
            )
        }

        // Near headlands frame the bay and lead the eye toward the hazy opening.
        val leftCliff = Path().apply {
            moveTo(0f, h * 0.34f)
            cubicTo(w * 0.16f, h * 0.36f, w * 0.19f, h * 0.52f, w * 0.35f, h * 0.61f)
            cubicTo(w * 0.25f, h * 0.74f, w * 0.20f, h * 0.89f, w * 0.12f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(leftCliff, cliffNear)
        val rightCliff = Path().apply {
            moveTo(w, h * 0.32f)
            cubicTo(w * 0.85f, h * 0.39f, w * 0.82f, h * 0.54f, w * 0.66f, h * 0.64f)
            cubicTo(w * 0.77f, h * 0.77f, w * 0.81f, h * 0.90f, w * 0.90f, h)
            lineTo(w, h)
            close()
        }
        drawPath(rightCliff, cliffNear)

        // A few understated ledges pick up the cool ambient light.
        for (i in 0..2) {
            val y = h * (0.46f + i * 0.07f)
            drawLine(
                color = TonalPalette.withAlpha(accent, (0.14f - i * 0.025f) * dim),
                start = Offset(w * (0.02f + i * 0.025f), y),
                end = Offset(w * (0.13f + i * 0.03f), y + h * 0.012f),
                strokeWidth = minDim * 0.003f,
            )
            drawLine(
                color = TonalPalette.withAlpha(accent, (0.12f - i * 0.02f) * dim),
                start = Offset(w * (0.98f - i * 0.025f), y + h * 0.01f),
                end = Offset(w * (0.87f - i * 0.03f), y + h * 0.02f),
                strokeWidth = minDim * 0.003f,
            )
        }

        // Mist feathers drift over the bay without distinct particle specks.
        for (i in 0..2) {
            val cy = horizon + h * (0.12f + i * 0.055f)
            val shift = (drift - 0.5f) * w * (0.12f + i * 0.03f)
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(accent, (0.08f - i * 0.015f) * dim),
                    1f to Color.Transparent,
                    center = Offset(w * 0.5f + shift, cy),
                    radius = minDim * (0.34f + i * 0.06f),
                ),
                topLeft = Offset(w * 0.5f + shift - minDim * 0.34f, cy - minDim * 0.10f),
                size = Size(minDim * 0.68f, minDim * 0.20f),
            )
        }
    }
}
