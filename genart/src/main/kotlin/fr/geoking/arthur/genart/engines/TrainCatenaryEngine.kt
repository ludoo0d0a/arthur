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

/** Soft Ambient POV Train Soft Catenary — receding wires and masts in a slow seamless loop. */
@Composable
internal fun TrainCatenaryEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detailCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "traincatenary")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "traincatenary_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val sway = (loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 29) - 0.5f) * 0.012f
        val horizon = h * (0.49f + 0.025f * (wash - 0.5f))
        val skyTop = TonalPalette.brightness(Color(0xFF12161C), brightness * dim)
        val skyBottom = TonalPalette.brightness(Color(0xFF1C222C), brightness * dim)
        val steel = TonalPalette.brightness(Color(0xFFC0C8D0), brightness * dim)
        val silhouette = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom)))

        // Quiet dusk glow and low haze around the vanishing point.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(steel, 0.17f * dim),
                0.48f to TonalPalette.withAlpha(Color(0xFF8996A8), 0.07f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.5f + sway), horizon),
                radius = minDim * 0.63f,
            ),
            radius = minDim * 0.63f,
            center = Offset(w * (0.5f + sway), horizon),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFFB0B8C4), 0.06f * dim),
                startY = horizon - h * 0.11f,
                endY = horizon + h * 0.11f,
            ),
        )

        // Soft, shallow cloud bands keep the horizon atmospheric.
        for (i in 0 until 4) {
            val y = horizon - h * (0.05f + i * 0.045f)
            val offset = (loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 51 + i * 13) - 0.5f) * w * 0.07f
            drawOval(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to TonalPalette.withAlpha(steel, 0.035f * dim),
                    1f to Color.Transparent,
                ),
                topLeft = Offset(-w * 0.12f + offset, y),
                size = Size(w * 1.24f, h * (0.035f + i * 0.003f)),
            )
        }

        // Distant land and rail bed fade in below the horizon.
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.16f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.78f * dim),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )
        val cx = w * (0.5f + sway)
        val railAlpha = 0.17f * dim
        listOf(-1f, 1f).forEach { side ->
            val path = Path().apply {
                moveTo(cx + side * w * 0.006f, horizon + h * 0.005f)
                cubicTo(
                    cx + side * w * 0.025f, horizon + h * 0.15f,
                    cx + side * w * 0.12f, h * 0.78f,
                    cx + side * w * 0.22f, h * 1.04f,
                )
            }
            drawPath(path, TonalPalette.withAlpha(steel, railAlpha), style = Stroke(width = minDim * 0.004f))
        }

        // The long overhead conductors converge toward the same vanishing point.
        for (side in listOf(-1f, 1f)) {
            val conductor = Path().apply {
                moveTo(cx + side * w * 0.43f, h * 0.035f)
                cubicTo(
                    cx + side * w * 0.22f, h * 0.12f,
                    cx + side * w * 0.075f, horizon - h * 0.04f,
                    cx + side * w * 0.012f, horizon,
                )
            }
            drawPath(conductor, TonalPalette.withAlpha(steel, 0.14f * dim), style = Stroke(width = minDim * 0.0018f, cap = StrokeCap.Round))
        }

        // Receding support masts, crossarms, and repeating wire spans.
        val phase = time * detailCount
        for (index in detailCount - 1 downTo 0) {
            val u = ((index + phase) % detailCount) / detailCount
            val depth = u * u
            val y = horizon + (h - horizon) * (0.08f + 0.90f * depth)
            val scale = 0.16f + 0.84f * depth
            val halfSpan = minDim * (0.045f + 0.055f * depth)
            val topY = horizon - h * (0.18f - 0.045f * depth)
            val mastColor = TonalPalette.withAlpha(silhouette, (0.68f + 0.2f * depth) * dim)
            val wireColor = TonalPalette.withAlpha(steel, (0.19f + 0.12f * (1f - depth)) * dim)
            val poleStroke = minDim * (0.0022f + 0.004f * depth)
            val leftX = cx - w * (0.035f + 0.19f * depth)
            val rightX = cx + w * (0.035f + 0.19f * depth)
            val crossY = topY + h * 0.035f * scale

            // Poles recede into the same central vanishing point as the rails.
            drawLine(mastColor, Offset(leftX, topY), Offset(leftX - halfSpan * 0.18f, y), poleStroke, cap = StrokeCap.Round)
            drawLine(mastColor, Offset(rightX, topY), Offset(rightX + halfSpan * 0.18f, y), poleStroke, cap = StrokeCap.Round)
            drawLine(mastColor, Offset(leftX - halfSpan, crossY), Offset(rightX + halfSpan, crossY), poleStroke, cap = StrokeCap.Round)
            drawLine(mastColor, Offset(leftX, topY + h * 0.035f * scale), Offset(leftX - halfSpan * 0.72f, crossY), poleStroke * 0.65f)
            drawLine(mastColor, Offset(rightX, topY + h * 0.035f * scale), Offset(rightX + halfSpan * 0.72f, crossY), poleStroke * 0.65f)

            // Insulator nubs and a soft highlight on the catenary contact wire.
            val wireY = crossY + h * 0.012f * scale
            val insulator = minDim * (0.0025f + 0.003f * depth)
            drawLine(mastColor, Offset(cx - halfSpan * 0.72f, crossY), Offset(cx - halfSpan * 0.72f, wireY), insulator)
            drawLine(mastColor, Offset(cx + halfSpan * 0.72f, crossY), Offset(cx + halfSpan * 0.72f, wireY), insulator)
            drawCircle(wireColor, radius = minDim * (0.0017f + 0.0015f * depth), center = Offset(cx, wireY))

            val span = Path().apply {
                moveTo(cx - halfSpan * 0.72f, wireY)
                quadraticTo(cx, wireY + h * 0.014f * scale, cx + halfSpan * 0.72f, wireY)
            }
            drawPath(span, wireColor, style = Stroke(width = minDim * (0.0012f + 0.001f * depth), cap = StrokeCap.Round))

            // Two parallel feeder wires stay faint and converge more quickly.
            for (wireOffset in listOf(-0.35f, 0.35f)) {
                val feeder = Path().apply {
                    moveTo(cx + wireOffset * halfSpan, topY - h * 0.025f * scale)
                    quadraticTo(
                        cx + wireOffset * halfSpan * 0.35f,
                        topY - h * 0.02f * scale,
                        cx,
                        horizon - h * 0.005f,
                    )
                }
                drawPath(feeder, TonalPalette.withAlpha(steel, 0.10f * dim), style = Stroke(width = minDim * 0.0012f))
            }
        }

        // Restrained peripheral carriage/window shadow adds a POV frame.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.42f * dim),
                0.08f to Color.Transparent,
                0.92f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.42f * dim),
            ),
        )
        val vignette = Brush.radialGradient(
            0f to Color.Transparent,
            0.72f to Color.Transparent,
            1f to TonalPalette.withAlpha(Color(0xFF080A0C), 0.46f * dim),
            center = Offset(w * 0.5f, h * 0.48f),
            radius = minDim * 0.78f,
        )
        drawRect(brush = vignette)
    }
}
