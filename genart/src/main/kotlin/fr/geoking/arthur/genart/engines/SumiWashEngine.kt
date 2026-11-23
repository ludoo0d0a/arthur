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
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient Sumi Wash — slow, seamless ink diffusion on warm handmade paper. */
@Composable
internal fun SumiWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sumiwash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sumiwash_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 61) - 0.5f
        val breathe = 0.94f + 0.06f * sin(time * 2f * PI.toFloat())
        val paper = TonalPalette.brightness(Color(0xFFF5F0E7), brightness * dim)
        val ink = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF17191D), TonalPalette.pick(paletteColors, 0), 0.12f),
            brightness * dim,
        )
        drawRect(brush = Brush.verticalGradient(listOf(paper, TonalPalette.brightness(Color(0xFFE6DED1), brightness * dim))))

        // Diffused ink pools: layered translucent blooms create soft, feathered edges.
        val bloomX = w * (0.53f + drift * 0.035f)
        val bloomY = h * 0.48f
        listOf(0.28f to 0.10f, 0.20f to 0.12f, 0.12f to 0.15f).forEachIndexed { index, (radius, alpha) ->
            val r = unit * (radius + index * 0.07f)
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(ink, alpha * breathe),
                    0.58f to TonalPalette.withAlpha(ink, alpha * 0.58f),
                    1f to Color.Transparent,
                    center = Offset(bloomX, bloomY),
                    radius = r,
                ),
                topLeft = Offset(bloomX - r * 0.78f, bloomY - r),
                size = androidx.compose.ui.geometry.Size(r * 1.56f, r * 2f),
            )
        }

        // Three uneven brush gestures, with a pale feather halo and a denser ink spine.
        val sway = drift * unit * 0.06f
        val strokes = listOf(
            Triple(0.22f, 0.50f, 0.60f),
            Triple(0.55f, 0.43f, 0.75f),
            Triple(0.84f, 0.53f, 0.50f),
        )
        strokes.forEachIndexed { index, (x, y, reach) ->
            val startX = w * x + sway * (index - 1)
            val startY = h * (y + 0.18f)
            val tipX = startX + unit * (0.12f + index * 0.025f)
            val tipY = startY - unit * reach
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX - unit * 0.10f, startY - unit * reach * 0.22f,
                    tipX + unit * 0.08f, tipY + unit * reach * 0.35f,
                    tipX, tipY,
                )
            }
            drawPath(path, TonalPalette.withAlpha(ink, 0.10f), style = Stroke(unit * 0.11f, cap = StrokeCap.Round))
            drawPath(path, TonalPalette.withAlpha(ink, 0.30f * dim), style = Stroke(unit * (0.035f + index * 0.006f), cap = StrokeCap.Round))
        }

        // Fine branch strokes grow out of the central wash like reeds or winter branches.
        val branchCount = when (quality) {
            GenartQuality.Low -> 3
            GenartQuality.Medium -> 5
            GenartQuality.High -> 7
        }
        repeat(branchCount) { i ->
            val side = if (i % 2 == 0) -1f else 1f
            val progress = 0.20f + (i / 2) * 0.17f
            val baseX = bloomX + side * unit * (0.06f + progress * 0.08f) + sway * 0.2f
            val baseY = bloomY + unit * (0.20f - progress * 0.55f)
            val branch = Path().apply {
                moveTo(baseX, baseY)
                quadraticTo(baseX + side * unit * 0.10f, baseY - unit * 0.08f, baseX + side * unit * 0.16f, baseY - unit * 0.24f)
            }
            drawPath(branch, TonalPalette.withAlpha(ink, 0.12f * dim), style = Stroke(unit * 0.006f, cap = StrokeCap.Round))
        }
    }
}
