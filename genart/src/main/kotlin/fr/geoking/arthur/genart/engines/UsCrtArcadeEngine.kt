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
import androidx.compose.ui.geometry.CornerRadius
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

/** Soft Ambient POV US CRT Arcade — a glowing phosphor screen in a dim cabinet. */
@Composable
internal fun UsCrtArcadeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val scanlineCount = qualityCount(quality, low = 28, medium = 48, high = 72)
    val transition = rememberInfiniteTransition(label = "uscrtarcade")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "uscrtarcade_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val shimmer = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 11)
        val roll = (time * 2f + 0.16f * shimmer) % 1f
        val frame = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val bezel = TonalPalette.brightness(Color(0xFF12161C), brightness * dim)
        val phosphor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF40FF80), TonalPalette.pick(paletteColors, 0), 0.2f),
            brightness * dim,
        )
        val screenLeft = w * 0.09f
        val screenTop = h * 0.13f
        val screenWidth = w * 0.82f
        val screenHeight = h * 0.69f
        val screenRight = screenLeft + screenWidth
        val screenBottom = screenTop + screenHeight
        val cx = w * 0.5f
        val cy = screenTop + screenHeight * 0.47f

        drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF050609), Color(0xFF11151A))))
        drawRoundRect(
            color = frame,
            topLeft = Offset(w * 0.045f, h * 0.075f),
            size = Size(w * 0.91f, h * 0.79f),
            cornerRadius = CornerRadius(minDim * 0.055f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(colors = listOf(Color(0xFF222831), bezel, Color(0xFF080A0C))),
            topLeft = Offset(w * 0.065f, h * 0.095f),
            size = Size(w * 0.87f, h * 0.75f),
            cornerRadius = CornerRadius(minDim * 0.045f),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF03100C),
                0.5f to TonalPalette.mix(Color(0xFF07150F), phosphor, 0.08f),
                1f to Color(0xFF020806),
                startY = screenTop,
                endY = screenBottom,
            ),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(minDim * 0.028f),
        )

        // Soft phosphor bloom and a slowly breathing scan field.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(phosphor, (0.16f + 0.035f * shimmer) * dim),
                0.62f to TonalPalette.withAlpha(phosphor, 0.055f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = minDim * 0.52f,
            ),
            radius = minDim * 0.52f,
            center = Offset(cx, cy),
        )

        // Subtle arcade geometry rendered as soft phosphor strokes.
        val gridTop = screenTop + screenHeight * 0.56f
        val horizon = screenTop + screenHeight * 0.48f
        for (i in 0..6) {
            val f = i / 6f
            val y = horizon + (screenBottom - horizon) * f * f
            drawLine(
                color = TonalPalette.withAlpha(phosphor, (0.16f + 0.08f * (1f - f)) * dim),
                start = Offset(screenLeft + screenWidth * (0.08f + 0.13f * f), y),
                end = Offset(screenRight - screenWidth * (0.08f + 0.13f * f), y),
                strokeWidth = minDim * 0.0014f,
            )
        }
        for (i in -4..4) {
            val x = cx + screenWidth * i * 0.105f
            drawLine(
                color = TonalPalette.withAlpha(phosphor, 0.14f * dim),
                start = Offset(cx + (x - cx) * 0.12f, horizon),
                end = Offset(x, screenBottom - screenHeight * 0.04f),
                strokeWidth = minDim * 0.0014f,
            )
        }
        val orbRadius = screenWidth * (0.105f + 0.006f * sin(time * 2f * PI.toFloat()))
        drawCircle(
            color = TonalPalette.withAlpha(Color(0xFF40FF80), 0.16f * dim),
            radius = orbRadius + minDim * 0.012f,
            center = Offset(cx, screenTop + screenHeight * 0.40f),
            style = Stroke(width = minDim * 0.002f),
        )
        drawCircle(
            color = TonalPalette.withAlpha(phosphor, 0.58f * dim),
            radius = orbRadius,
            center = Offset(cx, screenTop + screenHeight * 0.40f),
            style = Stroke(width = minDim * 0.003f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(phosphor, 0.17f * dim),
                1f to Color.Transparent,
                center = Offset(cx, screenTop + screenHeight * 0.40f),
                radius = orbRadius * 1.55f,
            ),
            radius = orbRadius * 1.55f,
            center = Offset(cx, screenTop + screenHeight * 0.40f),
        )

        // Scanlines and a very low contrast rolling band.
        for (i in 0 until scanlineCount) {
            val y = screenTop + screenHeight * i / scanlineCount
            val rollDistance = kotlin.math.abs(i.toFloat() / scanlineCount - roll)
            val rollGlow = (1f - rollDistance * 14f).coerceIn(0f, 1f)
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFF010503), (0.18f + 0.08f * (1f - shimmer)) * dim),
                start = Offset(screenLeft + minDim * 0.008f, y),
                end = Offset(screenRight - minDim * 0.008f, y),
                strokeWidth = minDim * 0.0018f,
            )
            if (rollGlow > 0f) {
                drawLine(
                    color = TonalPalette.withAlpha(phosphor, rollGlow * 0.045f * dim),
                    start = Offset(screenLeft + minDim * 0.008f, y),
                    end = Offset(screenRight - minDim * 0.008f, y),
                    strokeWidth = minDim * 0.004f,
                )
            }
        }

        // Convex glass catchlight and screen rim.
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF9CC8AC), 0.2f * dim),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(minDim * 0.028f),
            style = Stroke(width = minDim * 0.003f),
        )
        val reflection = Path().apply {
            moveTo(screenLeft + screenWidth * 0.08f, screenTop + screenHeight * 0.03f)
            cubicTo(
                screenLeft + screenWidth * 0.38f, screenTop + screenHeight * 0.08f,
                screenLeft + screenWidth * 0.48f, screenTop + screenHeight * 0.18f,
                screenLeft + screenWidth * 0.57f, screenTop + screenHeight * 0.30f,
            )
        }
        drawPath(
            path = reflection,
            color = TonalPalette.withAlpha(Color.White, 0.035f * dim),
            style = Stroke(width = minDim * 0.018f),
        )

        // Dim cabinet controls at the bottom.
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF030405), 0.8f * dim),
            topLeft = Offset(w * 0.19f, h * 0.79f),
            size = Size(w * 0.62f, h * 0.07f),
            cornerRadius = CornerRadius(minDim * 0.018f),
        )
        drawCircle(TonalPalette.withAlpha(Color(0xFFFF554C), 0.55f * dim), minDim * 0.012f, Offset(w * 0.68f, h * 0.825f))
        drawCircle(TonalPalette.withAlpha(Color(0xFF38D9B0), 0.48f * dim), minDim * 0.012f, Offset(w * 0.74f, h * 0.825f))
        drawCircle(TonalPalette.withAlpha(Color(0xFF304050), 0.75f * dim), minDim * 0.018f, Offset(w * 0.32f, h * 0.825f))
    }
}
