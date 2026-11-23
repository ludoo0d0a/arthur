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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Soft Ambient Ink Tendril — branching ink blooms over warm paper. */
@Composable
internal fun InkTendrilEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val branches = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "inktendril")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inktendril_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val phase = phase01(t)
        val drift = loopedFbm(phase, radius = 1.1f, octaves = 3, seedOffset = 37) - 0.5f
        val pulse = 0.94f + 0.06f * loopedFbm(phase, radius = 1.5f, octaves = 2, seedOffset = 83)
        val paper = TonalPalette.brightness(Color(0xFFECE6DC), brightness * dim)
        val paperShade = TonalPalette.brightness(Color(0xFFC8C0B4), brightness * dim)
        val ink = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF101018), TonalPalette.pick(paletteColors, 0), 0.12f),
            brightness * dim,
        )
        drawRect(brush = Brush.verticalGradient(listOf(paper, paperShade)))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFF9EC), 0.34f * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFFE8D8C6), 0.14f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.55f + drift * 0.08f), h * 0.48f),
                radius = unit * 0.72f,
            ),
            center = Offset(w * 0.55f, h * 0.48f),
            radius = unit * 0.72f,
        )

        // A shared dark stem divides into fine capillary branches; all motion
        // is periodic so the ink subtly breathes without a visible loop seam.
        val origin = Offset(w * 0.5f, h * 0.88f)
        val fan = branches + 3
        for (i in 0 until fan) {
            val spread = (i - (fan - 1) * 0.5f) / maxOf(1f, (fan - 1) * 0.5f)
            val angle = -PI.toFloat() * 0.5f + spread * 0.92f + drift * 0.12f
            val length = unit * (0.28f + 0.23f * (1f - kotlin.math.abs(spread)))
            val sway = sin(phase * 2f * PI.toFloat() + i * 0.71f) * unit * 0.018f
            val end = Offset(origin.x + cos(angle) * length + sway, origin.y + sin(angle) * length)
            val bend = Offset(
                origin.x + cos(angle) * length * 0.48f + sway * 0.3f,
                origin.y + sin(angle) * length * 0.48f,
            )
            val path = Path().apply {
                moveTo(origin.x, origin.y)
                quadraticTo(bend.x - sin(angle) * unit * 0.055f, bend.y + cos(angle) * unit * 0.055f, end.x, end.y)
            }
            val mainWidth = unit * (0.008f + (1f - kotlin.math.abs(spread)) * 0.008f)
            drawPath(path, TonalPalette.withAlpha(ink, 0.10f * pulse * dim), style = Stroke(mainWidth * 4f, cap = StrokeCap.Round))
            drawPath(path, TonalPalette.withAlpha(ink, 0.56f * pulse * dim), style = Stroke(mainWidth, cap = StrokeCap.Round))

            for (side in -1..1 step 2) {
                val branchStart = Offset(
                    origin.x + (end.x - origin.x) * (0.38f + (i % 3) * 0.1f),
                    origin.y + (end.y - origin.y) * (0.38f + (i % 3) * 0.1f),
                )
                val branchAngle = angle + side * (0.50f + 0.12f * sin(phase * 2f * PI.toFloat() + i))
                val branchLen = length * (0.32f + 0.08f * loopedFbm(phase, radius = 1.2f, seedOffset = 109 + i))
                val branchEnd = Offset(
                    branchStart.x + cos(branchAngle) * branchLen,
                    branchStart.y + sin(branchAngle) * branchLen,
                )
                val branchPath = Path().apply {
                    moveTo(branchStart.x, branchStart.y)
                    quadraticTo(
                        branchStart.x + cos(branchAngle - 0.45f) * branchLen * 0.55f,
                        branchStart.y + sin(branchAngle - 0.45f) * branchLen * 0.55f,
                        branchEnd.x,
                        branchEnd.y,
                    )
                }
                val branchWidth = mainWidth * (0.30f + 0.1f * ((i + side + 2) % 3))
                drawPath(branchPath, TonalPalette.withAlpha(ink, 0.08f * dim), style = Stroke(branchWidth * 3.5f, cap = StrokeCap.Round))
                drawPath(branchPath, TonalPalette.withAlpha(ink, 0.42f * pulse * dim), style = Stroke(branchWidth, cap = StrokeCap.Round))
            }
        }

        // Small soft ink blooms gather at the roots and branch junctions.
        for (i in 0 until branches.coerceAtMost(9)) {
            val x = w * (0.28f + i * 0.055f)
            val y = h * (0.70f - 0.035f * (i % 3))
            val radius = unit * (0.022f + 0.006f * loopedFbm(phase, radius = 1.2f, seedOffset = 151 + i))
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(ink, 0.22f * pulse * dim),
                    0.5f to TonalPalette.withAlpha(ink, 0.11f * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}
