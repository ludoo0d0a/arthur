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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

private data class FieldGrass(val x: Float, val height: Float, val phase: Float)

/** Soft Ambient POV Velo Golden Hour: a warm cycling route through sunlit fields. */
@Composable
internal fun VeloGoldenHourEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val grass = remember(count) {
        List(count * 3) { i ->
            FieldGrass(
                x = (i + 0.5f) / (count * 3f),
                height = seededRange(i * 13 + 7, 0.018f, 0.075f),
                phase = seededRange(i * 29 + 3, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "velogoldenhour")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "velogoldenhour_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val warp = loopedFbm(time, radius = 1.2f, octaves = 3, seedOffset = 11)
        val horizon = h * (0.49f + 0.025f * (warp - 0.5f))
        val sky = TonalPalette.brightness(Color(0xFFC87830), brightness * dim)
        val farField = TonalPalette.brightness(Color(0xFF6A4018), brightness * dim)
        val sunGold = TonalPalette.brightness(Color(0xFFFFD080), brightness * dim)
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFFAA6030), brightness * dim),
                0.52f to sky,
                1f to farField,
            ),
        )

        // Low sun haze and a diffuse golden band over the far fields.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(sunGold, 0.40f * dim),
                0.36f to TonalPalette.withAlpha(sunGold, 0.16f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.72f, horizon + h * 0.015f),
                radius = minDim * 0.48f,
            ),
            radius = minDim * 0.48f,
            center = Offset(w * 0.72f, horizon + h * 0.015f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.52f to TonalPalette.withAlpha(sunGold, 0.13f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.075f,
                endY = horizon + h * 0.075f,
            ),
        )

        // Rolling field bands soften into the haze at the horizon.
        val fieldColors = listOf(
            TonalPalette.withAlpha(Color(0xFFB97835), 0.48f * dim),
            TonalPalette.withAlpha(Color(0xFF8B5422), 0.78f * dim),
            TonalPalette.withAlpha(Color(0xFF503117), 0.95f * dim),
        )
        listOf(0.04f, 0.10f, 0.17f).forEachIndexed { index, rise ->
            val y = horizon + h * rise
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(w * 0.28f, y - h * 0.04f, w * 0.66f, y + h * 0.035f, w, y - h * 0.012f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, fieldColors[index])
        }

        // The road narrows to the sunlit horizon and widens beneath the rider.
        val road = Path().apply {
            moveTo(w * (0.47f + 0.012f * (warp - 0.5f)), horizon + h * 0.012f)
            cubicTo(w * 0.40f, h * 0.67f, w * 0.20f, h * 0.82f, w * 0.08f, h)
            lineTo(w * 0.92f, h)
            cubicTo(w * 0.80f, h * 0.82f, w * 0.60f, h * 0.67f, w * (0.53f + 0.012f * (warp - 0.5f)), horizon + h * 0.012f)
            close()
        }
        drawPath(road, TonalPalette.brightness(Color(0xFF251B14), brightness * dim))

        // Softly lit road shoulders and an intermittent center line create depth.
        drawPath(
            path = Path().apply {
                moveTo(w * 0.465f, horizon + h * 0.018f)
                cubicTo(w * 0.38f, h * 0.65f, w * 0.17f, h * 0.84f, w * 0.075f, h)
            },
            color = TonalPalette.withAlpha(sunGold, 0.42f * dim),
            style = Stroke(width = minDim * 0.004f),
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.535f, horizon + h * 0.018f)
                cubicTo(w * 0.62f, h * 0.65f, w * 0.83f, h * 0.84f, w * 0.925f, h)
            },
            color = TonalPalette.withAlpha(sunGold, 0.32f * dim),
            style = Stroke(width = minDim * 0.003f),
        )
        for (i in 0 until 7) {
            val p = (i + 1) / 8f
            val y = horizon + (h - horizon) * p * p
            val halfWidth = minDim * (0.002f + 0.009f * p)
            val sway = (warp - 0.5f) * w * 0.008f
            drawLine(
                color = TonalPalette.withAlpha(sunGold, (0.46f - 0.20f * p) * dim),
                start = Offset(w * 0.5f + sway - halfWidth, y),
                end = Offset(w * 0.5f + sway + halfWidth, y),
                strokeWidth = minDim * (0.003f + p * 0.004f),
            )
        }

        // Fine grasses catch the low sun along both field edges.
        grass.forEachIndexed { i, blade ->
            val side = if (i % 2 == 0) -1f else 1f
            val x = if (side < 0f) blade.x * w * 0.28f else w * (0.72f + blade.x * 0.28f)
            val yBase = horizon + h * (0.19f + blade.x * 0.30f)
            val sway = sin(time * 2f * PI.toFloat() + blade.phase) * minDim * 0.009f
            drawLine(
                color = TonalPalette.withAlpha(sunGold, (0.20f + 0.16f * warp) * dim),
                start = Offset(x, yBase),
                end = Offset(x + sway + side * minDim * 0.006f, yBase - minDim * blade.height),
                strokeWidth = minDim * 0.0018f,
            )
        }
    }
}
