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
import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.PI
import kotlin.math.sin

private data class ShelfBook(
    val widthFraction: Float,
    val heightFraction: Float,
    val colorIndex: Int,
    val inset: Float,
)

/** A quiet, warmly lit bookshelf with gentle dust drifting through the light. */
@Composable
internal fun BookshelfGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val shelves = remember {
        List(3) { row ->
            List(12) { index ->
                ShelfBook(
                    widthFraction = seededRange(row * 101 + index * 17 + 3, 0.052f, 0.082f),
                    heightFraction = seededRange(row * 103 + index * 19 + 7, 0.19f, 0.28f),
                    colorIndex = row * 13 + index,
                    inset = seededRange(row * 107 + index * 23 + 11, 0f, 0.035f),
                )
            }
        }
    }
    val transition = rememberInfiniteTransition(label = "bookshelfglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((11000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bookshelfglow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val glowNoise = loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = 11)
        val glowPulse = 0.80f + 0.20f * glowNoise
        val wood = TonalPalette.brightness(Color(0xFF241A14), brightness * dim)
        val backWood = TonalPalette.brightness(Color(0xFF3F2E1F), brightness * dim)
        val gold = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8C080), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )

        drawRect(brush = Brush.verticalGradient(0f to wood, 1f to backWood))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(gold, 0.30f * glowPulse * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF9B6337), 0.16f * glowPulse * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, h * 0.48f),
                radius = minDim * 0.76f,
            ),
            radius = minDim * 0.76f,
            center = Offset(w * 0.5f, h * 0.48f),
        )

        val frameColor = TonalPalette.brightness(Color(0xFF291A12), brightness * dim)
        val shelfColor = TonalPalette.brightness(Color(0xFF4A3020), brightness * dim)
        val spinePalette = listOf(
            Color(0xFF6E3024), Color(0xFF9B6B3B), Color(0xFF405044),
            Color(0xFF8A5234), Color(0xFF564050), Color(0xFFB17A42),
        )
        val startX = w * 0.085f
        val endX = w * 0.915f
        val shelfWidth = endX - startX
        val rowHeight = h * 0.255f
        val top = h * 0.105f
        val gap = h * 0.033f

        // Side uprights and a dark back panel make the shelving read as a single unit.
        drawRoundRect(
            color = TonalPalette.withAlpha(frameColor, 0.88f * dim),
            topLeft = Offset(startX - minDim * 0.025f, top - minDim * 0.025f),
            size = Size(shelfWidth + minDim * 0.05f, rowHeight * 3f + gap * 2f + minDim * 0.085f),
            cornerRadius = CornerRadius(minDim * 0.018f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF211710), 0.92f * dim),
            topLeft = Offset(startX, top),
            size = Size(shelfWidth, rowHeight * 3f + gap * 2f),
            cornerRadius = CornerRadius(minDim * 0.008f),
        )

        shelves.forEachIndexed { row, books ->
            val rowTop = top + row * (rowHeight + gap)
            val bookBottom = rowTop + rowHeight - minDim * 0.022f
            var cursorX = startX + shelfWidth * 0.018f
            books.forEach { book ->
                val bookWidth = shelfWidth * book.widthFraction
                val bookHeight = rowHeight * book.heightFraction
                if (cursorX + bookWidth < endX - shelfWidth * 0.012f) {
                    val bookTop = bookBottom - bookHeight - rowHeight * book.inset
                    val baseColor = TonalPalette.brightness(
                        TonalPalette.pick(spinePalette, book.colorIndex),
                        brightness * dim,
                    )
                    drawRoundRect(
                        color = TonalPalette.withAlpha(baseColor, 0.97f * dim),
                        topLeft = Offset(cursorX, bookTop),
                        size = Size(bookWidth, bookHeight),
                        cornerRadius = CornerRadius(minDim * 0.004f),
                    )
                    // Narrow spine highlights and a small gilt title mark.
                    drawLine(
                        color = TonalPalette.withAlpha(Color(0xFFE9BD78), 0.20f * dim),
                        start = Offset(cursorX + bookWidth * 0.18f, bookTop + bookHeight * 0.12f),
                        end = Offset(cursorX + bookWidth * 0.18f, bookBottom - rowHeight * book.inset - bookHeight * 0.12f),
                        strokeWidth = minDim * 0.002f,
                    )
                    drawLine(
                        color = TonalPalette.withAlpha(Color(0xFFE8C080), 0.42f * dim),
                        start = Offset(cursorX + bookWidth * 0.35f, bookTop + bookHeight * 0.32f),
                        end = Offset(cursorX + bookWidth * 0.72f, bookTop + bookHeight * 0.32f),
                        strokeWidth = minDim * 0.003f,
                    )
                    cursorX += bookWidth + shelfWidth * 0.008f
                }
            }

            val boardY = rowTop + rowHeight
            // Amber light catches the front edge of each shelf, fading softly below.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to TonalPalette.withAlpha(gold, 0.22f * glowPulse * dim),
                    0.18f to TonalPalette.withAlpha(shelfColor, 0.96f * dim),
                    1f to TonalPalette.withAlpha(frameColor, 0.98f * dim),
                    startY = boardY - minDim * 0.018f,
                    endY = boardY + minDim * 0.045f,
                ),
                topLeft = Offset(startX - minDim * 0.01f, boardY - minDim * 0.018f),
                size = Size(shelfWidth + minDim * 0.02f, minDim * 0.065f),
            )
            drawLine(
                color = TonalPalette.withAlpha(gold, 0.38f * glowPulse * dim),
                start = Offset(startX, boardY - minDim * 0.016f),
                end = Offset(endX, boardY - minDim * 0.016f),
                strokeWidth = minDim * 0.003f,
            )
        }

        // Dust catches the lamp glow and drifts on a seamless circular path.
        repeat(count) { i ->
            val phase = i * 2f * PI.toFloat() / count
            val n = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = i * 97 + 5)
            val x = (0.18f + 0.64f * ((phase / (2f * PI.toFloat()) + 0.06f * sin(time * 2f * PI.toFloat() + phase) + 1f) % 1f)) * w
            val y = (0.12f + 0.76f * ((i * 0.173f + time * 0.20f + 1f) % 1f)) * h
            val radius = minDim * (0.0018f + 0.0024f * n)
            drawCircle(
                color = TonalPalette.withAlpha(gold, (0.25f + 0.32f * n) * dim),
                radius = radius,
                center = Offset(x + (n - 0.5f) * minDim * 0.012f, y),
            )
        }
    }
}
