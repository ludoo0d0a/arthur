package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Retro-futurist demoscene / Tron vibe: neon perspective grid, slow chrome wireframe
 * shapes, mild phosphor glow — cyan–magenta, car-safe (no strobe).
 */
@Composable
internal fun RetroWaveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lineCount = qualityCount(quality, low = 10, medium = 14, high = 20)
    val fanCount = qualityCount(quality, low = 5, medium = 7, high = 9)
    val shapeCount = qualityCount(quality, low = 2, medium = 3, high = 4)
    val shapes = remember(shapeCount) {
        List(shapeCount) { i ->
            RetroWaveShapeSeed(
                xFrac = seededRange(i * 17 + 3, 0.22f, 0.78f),
                yFrac = seededRange(i * 29 + 7, 0.12f, 0.38f),
                sizeFrac = seededRange(i * 41 + 11, 0.08f, 0.16f),
                sides = 3 + (seededUnit(i * 53 + 13) * 3f).toInt().coerceIn(0, 3),
                spinSpeed = seededRange(i * 67 + 19, 0.15f, 0.45f),
                spinPhase = seededUnit(i * 79 + 23),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "retrowave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "retrowave_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cyan = TonalPalette.mix(Color(0xFF33F0FF), TonalPalette.pick(paletteColors, 0), 0.2f)
    val magenta = TonalPalette.mix(Color(0xFFFF2BD6), TonalPalette.pick(paletteColors, 1), 0.2f)
    val cycle = phase01(t)
    val tint = TonalPalette.mix(cyan, magenta, sin01(cycle * 2f * PI.toFloat()))
    val gridColor = TonalPalette.brightness(tint, brightness * dim)
    val sunColor = TonalPalette.brightness(
        TonalPalette.mix(Color(0xFFFF6B9D), magenta, 0.4f),
        brightness * dim,
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0618), Color(0xFF12081F), Color(0xFF05030A)),
                ),
            )
            // Soft sun disc behind the horizon
            val sunCenter = Offset(w * 0.5f, h * 0.38f)
            val sunR = minOf(w, h) * 0.18f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(sunColor, 0.55f * dim),
                        TonalPalette.withAlpha(sunColor, 0.18f * dim),
                        Color.Transparent,
                    ),
                    center = sunCenter,
                    radius = sunR * 2.2f,
                ),
                radius = sunR * 2.2f,
                center = sunCenter,
            )
            drawCircle(
                color = TonalPalette.withAlpha(sunColor, 0.75f * dim),
                radius = sunR,
                center = sunCenter,
            )
            // Horizontal sun bands (mild, not strobing)
            for (band in 0 until 5) {
                val by = sunCenter.y - sunR * 0.7f + band * sunR * 0.32f
                drawLine(
                    color = TonalPalette.withAlpha(Color(0xFF0A0618), 0.55f * dim),
                    start = Offset(sunCenter.x - sunR, by),
                    end = Offset(sunCenter.x + sunR, by),
                    strokeWidth = sunR * 0.06f,
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(5.dp)) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.48f
            val vanishingX = w * 0.5f
            val spacingFactor = 6.5f
            val scroll = phase01(t * 0.35f)

            for (i in 0 until lineCount) {
                val rawT = (i / lineCount.toFloat() + scroll) % 1f
                val y = horizonY + (h - horizonY) * (1f - 1f / (1f + rawT * spacingFactor))
                val alpha = (0.55f - rawT * 0.4f).coerceIn(0.1f, 0.55f) * dim
                drawLine(
                    color = TonalPalette.withAlpha(gridColor, alpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.6f,
                )
            }
            for (i in 0 until fanCount) {
                val ft = i / (fanCount - 1).coerceAtLeast(1).toFloat()
                drawLine(
                    color = TonalPalette.withAlpha(gridColor, 0.3f * dim),
                    start = Offset(vanishingX, horizonY),
                    end = Offset(ft * w, h),
                    strokeWidth = 1.6f,
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(gridColor, 0.28f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(vanishingX, horizonY),
                    radius = w * 0.32f,
                ),
                radius = w * 0.32f,
                center = Offset(vanishingX, horizonY),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t) * 2f * PI.toFloat()

            shapes.forEach { shape ->
                val angle = time * shape.spinSpeed + shape.spinPhase * 2f * PI.toFloat()
                val cx = w * shape.xFrac
                val cy = h * shape.yFrac
                val r = minDim * shape.sizeFrac
                val wire = TonalPalette.brightness(
                    TonalPalette.mix(
                        if (shape.colorIndex % 2 == 0) cyan else magenta,
                        TonalPalette.pick(paletteColors, shape.colorIndex),
                        0.25f,
                    ),
                    brightness * dim,
                )
                val path = Path()
                for (s in 0..shape.sides) {
                    val a = angle + s * 2f * PI.toFloat() / shape.sides
                    val px = cx + cos(a) * r
                    val py = cy + sin(a) * r * 0.72f
                    if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(wire, 0.55f * dim),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round),
                )
                // Soft chrome highlight edge
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color.White, 0.12f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(cx - r * 0.25f, cy - r * 0.3f),
                        radius = r * 0.7f,
                    ),
                    radius = r * 0.7f,
                    center = Offset(cx - r * 0.25f, cy - r * 0.3f),
                )
            }

            // Mild scanline / phosphor wash (very soft, no flicker)
            val scanAlpha = 0.045f * dim
            val scanStep = (h / 48f).coerceAtLeast(3f)
            var sy = 0f
            while (sy < h) {
                drawLine(
                    color = TonalPalette.withAlpha(cyan, scanAlpha),
                    start = Offset(0f, sy),
                    end = Offset(w, sy),
                    strokeWidth = 1f,
                )
                sy += scanStep
            }
        }
    }
}

private data class RetroWaveShapeSeed(
    val xFrac: Float,
    val yFrac: Float,
    val sizeFrac: Float,
    val sides: Int,
    val spinSpeed: Float,
    val spinPhase: Float,
    val colorIndex: Int,
)
