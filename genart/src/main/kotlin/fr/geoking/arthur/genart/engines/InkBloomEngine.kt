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
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class InkBranch(
    val startX: Float,
    val startY: Float,
    val reach: Float,
    val bend: Float,
    val width: Float,
    val phase: Float,
    val color: Color,
)

/** Soft Ambient Ink Bloom — dark ink slowly blooms through warm handmade paper. */
@Composable
internal fun InkBloomEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val branches = androidx.compose.runtime.remember(count) {
        List(count) { i ->
            InkBranch(
                startX = 0.32f + seededUnit(i * 19 + 5) * 0.36f,
                startY = 0.38f + seededUnit(i * 29 + 9) * 0.34f,
                reach = seededRange(i * 37 + 13, 0.22f, 0.46f),
                bend = seededRange(i * 43 + 17, -0.16f, 0.16f),
                width = seededRange(i * 53 + 21, 0.007f, 0.015f),
                phase = seededRange(i * 61 + 25, 0f, 2f * PI.toFloat()),
                color = TonalPalette.pick(paletteColors, i),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "inkbloom")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((11000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inkbloom_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 11)
        val bloom = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 29)
        val paper = TonalPalette.brightness(Color(0xFFF4F0E8), brightness * dim)
        val paperShade = TonalPalette.brightness(Color(0xFFE0D8C8), brightness * dim)
        val ink = TonalPalette.brightness(Color(0xFF1A1A22), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(paper, paperShade)))

        // Pale, irregular ink-water halos soften the edges of the dark forms.
        val cx = w * (0.50f + 0.04f * (drift - 0.5f))
        val cy = h * (0.51f + 0.06f * (bloom - 0.5f))
        val haloRadius = unit * (0.56f + 0.04f * bloom)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF716C70), 0.16f * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF8B8580), 0.11f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = Offset(cx, cy),
        )

        // A soft central bloom, surrounded by diffuse pools of diluted pigment.
        val coreRadius = unit * (0.24f + 0.04f * bloom)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(ink, 0.68f * dim),
                0.42f to TonalPalette.withAlpha(ink, 0.45f * dim),
                0.78f to TonalPalette.withAlpha(Color(0xFF3D3940), 0.20f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = coreRadius,
            ),
            radius = coreRadius,
            center = Offset(cx, cy),
        )

        // Curving ink tendrils radiate from overlapping soft pools. Their
        // endpoints and control points oscillate around a closed phase loop.
        branches.forEachIndexed { i, branch ->
            val n = loopedFbm(time, radius = 1.2f + i * 0.025f, octaves = 2, seedOffset = i * 47 + 3)
            val sway = sin(time * 2f * PI.toFloat() + branch.phase)
            val x0 = w * (branch.startX + 0.05f * (drift - 0.5f))
            val y0 = h * (branch.startY + 0.04f * (bloom - 0.5f))
            val sign = if (i % 2 == 0) 1f else -1f
            val reach = unit * branch.reach * (0.86f + 0.18f * n)
            val tipX = x0 + unit * (branch.bend + 0.025f * sway)
            val tipY = y0 + sign * reach
            val path = Path().apply {
                moveTo(x0, y0)
                cubicTo(
                    x0 + unit * (branch.bend * 0.25f + 0.035f * sway), y0 + sign * reach * 0.28f,
                    tipX - unit * 0.025f * sway, y0 + sign * reach * 0.72f,
                    tipX, tipY,
                )
            }
            val tint = TonalPalette.mix(ink, branch.color, 0.12f)
            drawPath(path, color = TonalPalette.withAlpha(tint, (0.18f + 0.14f * n) * dim), style = Stroke(width = unit * branch.width * 3.6f, cap = StrokeCap.Round))
            drawPath(path, color = TonalPalette.withAlpha(tint, (0.36f + 0.28f * n) * dim), style = Stroke(width = unit * branch.width, cap = StrokeCap.Round))

            // Tiny capillary offshoots make the bloom feel organic rather than geometric.
            for (side in -1..1 step 2) {
                val fork = Path().apply {
                    moveTo(x0 + (tipX - x0) * 0.42f, y0 + sign * reach * 0.42f)
                    cubicTo(
                        x0 + unit * (branch.bend + side * 0.035f), y0 + sign * reach * 0.50f,
                        tipX + unit * side * 0.045f, tipY - sign * reach * 0.10f,
                        tipX + unit * side * (0.055f + 0.015f * sway), tipY - sign * reach * 0.18f,
                    )
                }
                drawPath(fork, color = TonalPalette.withAlpha(tint, 0.32f * dim), style = Stroke(width = unit * branch.width * 0.52f, cap = StrokeCap.Round))
            }
        }

        // A few suspended pigment beads echo the ink dispersing into water.
        for (i in 0 until count) {
            val x = w * (0.22f + seededUnit(i * 83 + 7) * 0.56f) + unit * 0.018f * sin(time * 2f * PI.toFloat() + i)
            val y = h * (0.22f + seededUnit(i * 97 + 31) * 0.56f) + unit * 0.015f * sin(time * 2f * PI.toFloat() + i * 1.7f)
            val radius = unit * (0.006f + 0.012f * seededUnit(i * 109 + 41))
            drawCircle(TonalPalette.withAlpha(ink, 0.16f * dim), radius * 2.8f, Offset(x, y))
            drawCircle(TonalPalette.withAlpha(ink, 0.45f * dim), radius, Offset(x, y))
        }
    }
}
