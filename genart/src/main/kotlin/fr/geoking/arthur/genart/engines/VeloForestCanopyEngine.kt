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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Velo Forest Canopy — a shaded ride toward a warm clearing. */
@Composable
internal fun VeloForestCanopyEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "veloforestcanopy")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "veloforestcanopy_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 11)
        val breathing = 0.9f + 0.1f * loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 23)
        val horizonY = h * (0.57f + (drift - 0.5f) * 0.018f)
        val darkCanopy = TonalPalette.brightness(Color(0xFF0C1810), brightness * dim)
        val midGreen = TonalPalette.brightness(Color(0xFF1A3020), brightness * dim)
        val leafGreen = TonalPalette.brightness(Color(0xFF24452D), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF60A070), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to darkCanopy,
                0.55f to midGreen,
                1f to Color(0xFF101D14),
            ),
        )

        // Luminous opening at the end of the forest path.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFD9A0), 0.58f * breathing * dim),
                0.3f to TonalPalette.withAlpha(accent, 0.30f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizonY),
                radius = minDim * 0.54f,
            ),
            radius = minDim * 0.54f,
            center = Offset(w * 0.5f, horizonY),
        )

        // Soft shafts filtered through the high canopy.
        repeat(4) { i ->
            val x = w * (0.30f + i * 0.13f) + (drift - 0.5f) * w * 0.025f
            val shaft = Path().apply {
                moveTo(x - w * 0.025f, h * 0.12f)
                lineTo(x + w * 0.035f, h * 0.12f)
                lineTo(w * (0.47f + i * 0.018f), horizonY + h * 0.12f)
                lineTo(w * (0.46f + i * 0.018f), horizonY + h * 0.12f)
                close()
            }
            drawPath(shaft, TonalPalette.withAlpha(Color(0xFFFFD89A), (0.045f + 0.025f * breathing) * dim))
        }

        // Distant tree line in soft, overlapping layers.
        for (i in 0 until 9) {
            val x = w * (i / 8f)
            val crownW = w * (0.13f + (i % 3) * 0.025f)
            val crownH = h * (0.13f + (i % 4) * 0.025f)
            val y = horizonY + h * (0.03f + (i % 2) * 0.025f)
            drawOval(
                color = TonalPalette.withAlpha(Color(0xFF14261A), 0.75f * dim),
                topLeft = Offset(x - crownW * 0.5f, y - crownH * 0.65f),
                size = Size(crownW, crownH),
            )
        }

        // Cyclist's path narrows toward the clearing, edged by low grasses.
        val path = Path().apply {
            moveTo(w * 0.47f, horizonY)
            lineTo(w * 0.53f, horizonY)
            lineTo(w * 0.91f, h)
            lineTo(w * 0.09f, h)
            close()
        }
        drawPath(path, Brush.verticalGradient(
            0f to TonalPalette.withAlpha(Color(0xFF59634D), 0.58f * dim),
            1f to TonalPalette.withAlpha(Color(0xFF18251A), 0.84f * dim),
            startY = horizonY,
            endY = h,
        ))
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFFD2C49A), 0.24f * dim),
            start = Offset(w * 0.47f, horizonY),
            end = Offset(w * 0.28f, h),
            strokeWidth = minDim * 0.003f,
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFFD2C49A), 0.24f * dim),
            start = Offset(w * 0.53f, horizonY),
            end = Offset(w * 0.72f, h),
            strokeWidth = minDim * 0.003f,
        )

        // Tall trunks and layered foliage frame the POV without closing the view.
        repeat(6) { i ->
            val side = if (i % 2 == 0) -1f else 1f
            val x = if (side < 0f) w * (0.04f + (i / 2) * 0.045f) else w * (0.96f - (i / 2) * 0.045f)
            val topY = h * (0.08f + (i % 3) * 0.06f)
            val width = minDim * (0.022f + (i % 3) * 0.008f)
            val treeColor = TonalPalette.withAlpha(Color(0xFF101B12), (0.7f + (i % 2) * 0.18f) * dim)
            drawLine(
                color = treeColor,
                start = Offset(x, topY),
                end = Offset(x + side * width * 1.5f, h * (0.95f - (i % 2) * 0.08f)),
                strokeWidth = width,
            )
            drawOval(
                color = TonalPalette.withAlpha(leafGreen, 0.65f * dim),
                topLeft = Offset(x - width * 3f, topY - width * 2f),
                size = Size(width * 6f, minDim * 0.18f),
            )
        }

        // Overhanging canopy creates the characteristic tunnel of leaves.
        val canopy = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.16f)
            cubicTo(w * 0.80f, h * 0.11f, w * 0.73f, h * 0.25f, w * 0.58f, h * 0.23f)
            cubicTo(w * 0.42f, h * 0.20f, w * 0.35f, h * 0.12f, w * 0.20f, h * 0.20f)
            cubicTo(w * 0.12f, h * 0.25f, w * 0.08f, h * 0.17f, 0f, h * 0.23f)
            close()
        }
        drawPath(canopy, darkCanopy)
        repeat(detailCount) { i ->
            val x = w * ((i + 0.5f) / detailCount)
            val sway = sin(time * 2f * PI.toFloat() + i) * minDim * 0.006f
            val y = h * (0.17f + (i % 3) * 0.025f) + sway
            val leafW = minDim * (0.08f + (i % 4) * 0.012f)
            drawOval(
                color = TonalPalette.withAlpha(leafGreen, 0.45f * dim),
                topLeft = Offset(x - leafW * 0.5f, y - leafW * 0.16f),
                size = Size(leafW, leafW * 0.32f),
            )
        }

        // Foreground grasses slide gently by as the bicycle rolls forward.
        for (i in 0 until detailCount) {
            val side = if (i % 2 == 0) -1f else 1f
            val progress = (i / detailCount.toFloat() + (sin(time * 2f * PI.toFloat()) + 1f) * 0.06f) % 1f
            val y = horizonY + (h - horizonY) * (0.34f + 0.64f * progress)
            val x = if (side < 0f) w * (0.06f + progress * 0.29f) else w * (0.94f - progress * 0.29f)
            val height = minDim * (0.025f + 0.04f * progress)
            drawLine(
                color = TonalPalette.withAlpha(accent, 0.22f * dim),
                start = Offset(x, y),
                end = Offset(x + side * height * 0.24f, y - height),
                strokeWidth = minDim * 0.002f,
            )
        }

        // Soft edge falloff keeps attention on the bright opening.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.34f * dim),
                0.18f to Color.Transparent,
                0.82f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.34f * dim),
            ),
        )
        drawPath(
            path = canopy,
            color = TonalPalette.withAlpha(accent, 0.08f * breathing * dim),
            style = Stroke(width = minDim * 0.01f),
        )
    }
}
