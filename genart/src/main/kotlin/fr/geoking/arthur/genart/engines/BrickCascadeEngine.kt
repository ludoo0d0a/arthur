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
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.abs

/**
 * Soft ball bouncing under brick rows that gently fade — ambient cascade, no flash.
 */
@Composable
internal fun BrickCascadeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cols = qualityCount(quality, low = 8, medium = 10, high = 12)
    val rows = qualityCount(quality, low = 4, medium = 5, high = 6)
    val bricks = remember(cols, rows) {
        List(cols * rows) { i ->
            BrickSeed(
                col = i % cols,
                row = i / cols,
                fadePhase = seededUnit(i * 17 + 3),
                fadeSpeed = seededRange(i * 29 + 7, 0.15f, 0.55f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "brickcascade")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "brickcascade_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cycle = phase01(t)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = TonalPalette.brightness(Color(0xFF0A0C14), brightness * dim))

        val marginX = w * 0.08f
        val marginTop = h * 0.1f
        val brickAreaH = h * 0.32f
        val gap = 3f
        val brickW = (w - marginX * 2f - gap * (cols - 1)) / cols
        val brickH = (brickAreaH - gap * (rows - 1)) / rows

        bricks.forEach { brick ->
            val life = sin01((cycle * brick.fadeSpeed + brick.fadePhase) * 2f * PI.toFloat())
            val alpha = (0.15f + 0.7f * life) * dim
            if (alpha < 0.05f) return@forEach
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    BrickColors[brick.colorIndex % BrickColors.size],
                    TonalPalette.pick(paletteColors, brick.colorIndex),
                    0.25f,
                ),
                brightness * dim,
            )
            val x = marginX + brick.col * (brickW + gap)
            val y = marginTop + brick.row * (brickH + gap)
            drawRect(
                color = TonalPalette.withAlpha(tint, alpha),
                topLeft = Offset(x, y),
                size = Size(brickW, brickH),
            )
            drawRect(
                color = TonalPalette.withAlpha(Color.White, 0.1f * alpha),
                topLeft = Offset(x, y),
                size = Size(brickW, brickH * 0.25f),
            )
        }

        // Soft paddle
        val paddleY = h * 0.88f
        val paddleW = w * 0.18f
        val paddleX = w * 0.5f + (abs(sin01(cycle * 2f * PI.toFloat()) * 2f - 1f) - 0.5f) * w * 0.5f
        val paddleColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8E4D8), TonalPalette.pick(paletteColors, 2), 0.2f),
            brightness * dim,
        )
        drawRect(
            color = TonalPalette.withAlpha(paddleColor, 0.85f * dim),
            topLeft = Offset(paddleX - paddleW * 0.5f, paddleY),
            size = Size(paddleW, h * 0.018f),
        )

        // Ball bounce (triangle-ish path)
        val bx = marginX + abs(sin01(cycle * 2.4f * PI.toFloat()) * 2f - 1f) * (w - marginX * 2f)
        val byTop = marginTop + brickAreaH + h * 0.05f
        val byBot = paddleY - h * 0.03f
        val by = byTop + abs(sin01(cycle * 3.6f * PI.toFloat() + 0.6f) * 2f - 1f) * (byBot - byTop)
        val ballR = minOf(w, h) * 0.012f
        val ball = TonalPalette.brightness(Color(0xFFFFF6E8), brightness * dim)
        drawCircle(color = TonalPalette.withAlpha(ball, 0.25f * dim), radius = ballR * 2.2f, center = Offset(bx, by))
        drawCircle(color = TonalPalette.withAlpha(ball, 0.95f * dim), radius = ballR, center = Offset(bx, by))
    }
}

private val BrickColors = listOf(
    Color(0xFFFF6B6B),
    Color(0xFFFFB347),
    Color(0xFF6BCB77),
    Color(0xFF4D96FF),
    Color(0xFFC77DFF),
    Color(0xFFFFE066),
)

private data class BrickSeed(
    val col: Int,
    val row: Int,
    val fadePhase: Float,
    val fadeSpeed: Float,
    val colorIndex: Int,
)
