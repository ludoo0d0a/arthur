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
import kotlin.math.sin

/**
 * Descending abstract pixel fleet — soft formation drift, no combat flash.
 */
@Composable
internal fun PixelFormationEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val unitCount = qualityCount(quality, low = 12, medium = 18, high = 28)
    val units = remember(unitCount) {
        List(unitCount) { i ->
            PixelUnitSeed(
                col = i % 7,
                row = i / 7,
                wobbleAmp = seededRange(i * 17 + 3, 0.01f, 0.04f),
                wobblePhase = seededUnit(i * 29 + 7),
                sizeFrac = seededRange(i * 41 + 11, 0.7f, 1.15f),
                colorIndex = i,
                shape = (seededUnit(i * 53 + 13) * 3f).toInt().coerceIn(0, 2),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "pixelformation")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pixelformation_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cycle = phase01(t)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = TonalPalette.brightness(Color(0xFF060810), brightness * dim))

        // Soft star dust
        val dust = TonalPalette.withAlpha(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * dim),
            0.25f * dim,
        )
        for (i in 0 until 24) {
            val sx = seededUnit(i * 19 + 2) * w
            val sy = seededUnit(i * 31 + 5) * h
            drawCircle(color = dust, radius = 1.2f, center = Offset(sx, sy))
        }

        val cell = minOf(w, h) * 0.045f
        val formationW = 7 * cell * 1.35f
        val originX = (w - formationW) * 0.5f
        val descend = phase01(cycle * 0.55f)
        val originY = -h * 0.15f + descend * h * 1.15f
        val sway = sin(cycle * 2f * PI.toFloat()) * w * 0.04f

        units.forEach { unit ->
            val wobble = sin((cycle + unit.wobblePhase) * 2f * PI.toFloat()) * unit.wobbleAmp * w
            val x = originX + unit.col * cell * 1.35f + sway + wobble
            val y = originY + unit.row * cell * 1.45f
            if (y < -cell || y > h + cell) return@forEach
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    FleetColors[unit.colorIndex % FleetColors.size],
                    TonalPalette.pick(paletteColors, unit.colorIndex),
                    0.25f,
                ),
                brightness * dim,
            )
            val s = cell * unit.sizeFrac
            val alpha = (0.55f + 0.35f * sin01(cycle * 2f * PI.toFloat() + unit.wobblePhase)) * dim
            when (unit.shape) {
                0 -> {
                    // Chevron / arrowhead
                    drawRect(
                        color = TonalPalette.withAlpha(tint, alpha),
                        topLeft = Offset(x + s * 0.35f, y),
                        size = Size(s * 0.3f, s * 0.7f),
                    )
                    drawRect(
                        color = TonalPalette.withAlpha(tint, alpha),
                        topLeft = Offset(x, y + s * 0.35f),
                        size = Size(s, s * 0.25f),
                    )
                }
                1 -> {
                    drawRect(
                        color = TonalPalette.withAlpha(tint, alpha),
                        topLeft = Offset(x + s * 0.15f, y + s * 0.1f),
                        size = Size(s * 0.7f, s * 0.7f),
                    )
                }
                else -> {
                    drawRect(
                        color = TonalPalette.withAlpha(tint, alpha),
                        topLeft = Offset(x + s * 0.2f, y),
                        size = Size(s * 0.6f, s * 0.35f),
                    )
                    drawRect(
                        color = TonalPalette.withAlpha(tint, alpha * 0.85f),
                        topLeft = Offset(x, y + s * 0.35f),
                        size = Size(s, s * 0.35f),
                    )
                }
            }
        }
    }
}

private val FleetColors = listOf(
    Color(0xFF5EEAD4),
    Color(0xFF7DD3FC),
    Color(0xFFF9A8D4),
    Color(0xFFFDE68A),
    Color(0xFFC4B5FD),
)

private data class PixelUnitSeed(
    val col: Int,
    val row: Int,
    val wobbleAmp: Float,
    val wobblePhase: Float,
    val sizeFrac: Float,
    val colorIndex: Int,
    val shape: Int,
)
