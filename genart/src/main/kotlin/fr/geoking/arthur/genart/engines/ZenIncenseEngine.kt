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

/** Soft Ambient Zen Incense — a glowing stick and slowly curling smoke. */
@Composable
internal fun ZenIncenseEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val moteCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "zenincense")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "zenincense_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val flow = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 19)
        val sway = sin(time * 2f * PI.toFloat())
        val background = TonalPalette.brightness(Color(0xFF1A1612), brightness * dim)
        val shadow = TonalPalette.brightness(Color(0xFF2E2820), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFC8B090), TonalPalette.pick(paletteColors, 0), 0.2f),
            brightness * dim,
        )
        val smoke = TonalPalette.mix(Color(0xFFB8B0A4), accent, 0.3f)
        val cx = w * 0.5f
        val stickTip = Offset(cx - unit * 0.08f, h * 0.73f)

        drawRect(
            brush = Brush.verticalGradient(
                0f to background,
                0.68f to shadow,
                1f to Color(0xFF120F0C),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.13f * (0.9f + 0.1f * flow) * dim),
                0.5f to TonalPalette.withAlpha(accent, 0.055f * dim),
                1f to Color.Transparent,
                center = stickTip,
                radius = unit * 0.52f,
            ),
            radius = unit * 0.52f,
            center = stickTip,
        )

        // Quiet altar ledge and an unlit incense bowl.
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF080706), 0.42f * dim),
            start = Offset(w * 0.14f, h * 0.91f),
            end = Offset(w * 0.86f, h * 0.91f),
            strokeWidth = unit * 0.018f,
            cap = StrokeCap.Round,
        )
        drawOval(
            color = TonalPalette.brightness(Color(0xFF342D24), brightness * dim),
            topLeft = Offset(cx - unit * 0.16f, h * 0.865f),
            size = androidx.compose.ui.geometry.Size(unit * 0.32f, unit * 0.055f),
        )
        drawOval(
            color = Color(0xFF191612),
            topLeft = Offset(cx - unit * 0.115f, h * 0.868f),
            size = androidx.compose.ui.geometry.Size(unit * 0.23f, unit * 0.034f),
        )

        // Incense stick rises from the bowl at a slight, calm angle.
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF8C6848), 0.9f * dim),
            start = Offset(cx + unit * 0.11f, h * 0.87f),
            end = stickTip,
            strokeWidth = unit * 0.012f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFFD1A876), 0.25f * dim),
            start = Offset(cx + unit * 0.105f, h * 0.86f),
            end = stickTip,
            strokeWidth = unit * 0.0025f,
            cap = StrokeCap.Round,
        )

        // A warm ember with a restrained halo.
        val emberPulse = 0.86f + 0.14f * (0.5f + 0.5f * sway)
        drawCircle(
            color = TonalPalette.withAlpha(Color(0xFFE9A65F), 0.22f * emberPulse * dim),
            radius = unit * 0.027f,
            center = stickTip,
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFE2A1),
                0.35f to Color(0xFFEAA15A),
                1f to Color.Transparent,
                center = stickTip,
                radius = unit * 0.012f,
            ),
            radius = unit * 0.012f,
            center = stickTip,
        )

        // Three translucent strands share a slow phase-driven curl.
        for (strand in 0..2) {
            val offset = (strand - 1) * unit * 0.018f
            val path = Path().apply {
                moveTo(stickTip.x + offset * 0.25f, stickTip.y - unit * 0.012f)
                cubicTo(
                    stickTip.x - unit * (0.015f + 0.025f * sway) + offset,
                    stickTip.y - unit * 0.08f,
                    stickTip.x + unit * (0.04f + 0.02f * sway) + offset,
                    stickTip.y - unit * 0.15f,
                    stickTip.x - unit * (0.01f - 0.018f * sway) + offset,
                    stickTip.y - unit * 0.22f,
                )
                cubicTo(
                    stickTip.x - unit * (0.06f - 0.025f * flow) + offset,
                    stickTip.y - unit * 0.31f,
                    stickTip.x + unit * (0.045f + 0.025f * flow) + offset,
                    stickTip.y - unit * 0.37f,
                    stickTip.x + unit * (0.015f + 0.035f * sway) + offset,
                    stickTip.y - unit * (0.46f + 0.025f * flow),
                )
            }
            val alpha = (0.13f + strand * 0.035f) * dim
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(smoke, alpha * 0.45f),
                style = Stroke(width = unit * (0.026f - strand * 0.004f), cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(smoke, alpha),
                style = Stroke(width = unit * (0.007f - strand * 0.001f), cap = StrokeCap.Round),
            )
        }

        // Sparse floating ash motes drift in a closed, looping path.
        repeat(moteCount) { i ->
            val n = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = i * 71 + 5)
            val side = if (i % 2 == 0) -1f else 1f
            val x = w * (0.5f + (seededUnit(i * 17 + 3) - 0.5f) * 0.62f + 0.025f * sway * side)
            val y = h * (0.22f + seededUnit(i * 29 + 7) * 0.64f + 0.018f * sin(time * 2f * PI.toFloat() + i))
            val r = unit * seededRange(i * 41 + 11, 0.002f, 0.004f) * (0.75f + n * 0.5f)
            drawCircle(
                color = TonalPalette.withAlpha(accent, (0.16f + 0.18f * n) * dim),
                radius = r,
                center = Offset(x, y),
            )
        }
    }
}
