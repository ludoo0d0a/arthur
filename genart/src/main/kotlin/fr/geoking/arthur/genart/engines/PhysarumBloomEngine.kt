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
import kotlin.math.cos
import kotlin.math.sin

private data class BloomBranch(
    val angle: Float,
    val length: Float,
    val bend: Float,
    val phase: Float,
    val thickness: Float,
)

/** Soft Ambient Physarum Bloom — branching filaments radiate from living focal blooms. */
@Composable
internal fun PhysarumBloomEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val branches = remember(count) {
        List(count) { i ->
            BloomBranch(
                angle = (i.toFloat() / count) * 2f * PI.toFloat() + seededRange(i * 17 + 3, -0.15f, 0.15f),
                length = seededRange(i * 29 + 7, 0.20f, 0.40f),
                bend = seededRange(i * 41 + 11, -0.14f, 0.14f),
                phase = seededUnit(i * 53 + 13) * 2f * PI.toFloat(),
                thickness = seededRange(i * 67 + 19, 0.0022f, 0.0045f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumbloom")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumbloom_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val green = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7CFFB2) }, brightness * dim)
    val coral = TonalPalette.brightness(Color(0xFFFF806E), brightness * dim)
    val ground = TonalPalette.brightness(Color(0xFF05080A), brightness * dim)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val center = Offset(w * (0.5f + 0.035f * sin(2f * PI.toFloat() * time)), h * 0.50f)
        val breathe = 0.94f + 0.06f * loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 81)

        drawRect(color = ground)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(green, 0.14f * dim),
                0.42f to TonalPalette.withAlpha(green, 0.055f * dim),
                1f to Color.Transparent,
                center = center,
                radius = unit * 0.54f,
            ),
            radius = unit * 0.54f,
            center = center,
        )

        branches.forEachIndexed { i, branch ->
            val noise = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = i * 37 + 5) - 0.5f
            val pulse = 0.86f + 0.14f * sin(2f * PI.toFloat() * time + branch.phase)
            val angle = branch.angle + noise * 0.12f
            val length = unit * branch.length * (0.96f + 0.04f * breathe)
            val direction = Offset(cos(angle), sin(angle))
            val normal = Offset(-direction.y, direction.x)
            val end = center + direction * length
            val bend = branch.bend * unit + noise * unit * 0.025f
            val control = center + direction * (length * 0.50f) + normal * bend
            val path = Path().apply {
                moveTo(center.x, center.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            }
            val branchColor = if (i % 5 == 0) coral else green
            val alpha = (0.60f + 0.24f * pulse) * dim
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(branchColor, 0.15f * alpha),
                style = Stroke(width = unit * branch.thickness * 5.5f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(branchColor, alpha),
                style = Stroke(width = unit * branch.thickness, cap = StrokeCap.Round),
            )

            // Short nutrient branches, alternating along each main filament.
            for (side in -1..1 step 2) {
                val branchPoint = center + direction * (length * (0.48f + 0.10f * (i % 3))) + normal * bend * 0.35f
                val twigAngle = angle + side * (0.58f + 0.12f * sin(branch.phase + time * 2f * PI.toFloat()))
                val twigLength = length * (0.20f + 0.035f * ((i + side + 2) % 3))
                val twigEnd = branchPoint + Offset(cos(twigAngle), sin(twigAngle)) * twigLength
                val twigControl = branchPoint + Offset(cos(twigAngle), sin(twigAngle)) * (twigLength * 0.55f) + normal * (side * bend * 0.18f)
                val twigPath = Path().apply {
                    moveTo(branchPoint.x, branchPoint.y)
                    quadraticTo(twigControl.x, twigControl.y, twigEnd.x, twigEnd.y)
                }
                drawPath(
                    path = twigPath,
                    color = TonalPalette.withAlpha(branchColor, 0.11f * alpha),
                    style = Stroke(width = unit * branch.thickness * 3f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = twigPath,
                    color = TonalPalette.withAlpha(branchColor, alpha * 0.72f),
                    style = Stroke(width = unit * branch.thickness * 0.68f, cap = StrokeCap.Round),
                )
            }

            val tipRadius = unit * branch.thickness * 1.35f
            drawCircle(
                color = TonalPalette.withAlpha(branchColor, 0.52f * alpha),
                radius = tipRadius,
                center = end,
            )
        }

        // Two overlapping soft nodes make the bloom read as a living focal colony.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFD8FFE8), 0.52f * dim),
                0.18f to TonalPalette.withAlpha(green, 0.64f * dim),
                0.62f to TonalPalette.withAlpha(green, 0.18f * dim),
                1f to Color.Transparent,
                center = center,
                radius = unit * 0.13f * breathe,
            ),
            radius = unit * 0.13f * breathe,
            center = center,
        )
        drawCircle(
            color = TonalPalette.withAlpha(coral, 0.12f * dim),
            radius = unit * 0.045f,
            center = center + Offset(unit * 0.035f, -unit * 0.025f),
        )
    }
}
