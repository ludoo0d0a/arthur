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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount

/**
 * Pseudo-3D vanishing neon highway with soft roadside posts — calm night drive.
 */
@Composable
internal fun NeonHighwayEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val postCount = qualityCount(quality, low = 8, medium = 12, high = 18)
    val markerCount = qualityCount(quality, low = 8, medium = 12, high = 16)
    val transition = rememberInfiniteTransition(label = "neonhighway")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "neonhighway_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cyan = TonalPalette.mix(Color(0xFF33F0FF), TonalPalette.pick(paletteColors, 0), 0.25f)
    val magenta = TonalPalette.mix(Color(0xFFFF2BD6), TonalPalette.pick(paletteColors, 1), 0.25f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vanishX = w * 0.5f
        val vanishY = h * 0.36f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF0A0618), brightness * dim),
                    TonalPalette.brightness(Color(0xFF12081F), brightness * dim),
                    Color(0xFF04020A),
                ),
            ),
        )

        // Soft horizon glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(magenta, 0.22f * dim),
                    Color.Transparent,
                ),
                center = Offset(vanishX, vanishY),
                radius = w * 0.35f,
            ),
            radius = w * 0.35f,
            center = Offset(vanishX, vanishY),
        )

        val roadPath = Path().apply {
            moveTo(vanishX - w * 0.015f, vanishY)
            lineTo(vanishX + w * 0.015f, vanishY)
            lineTo(w * 0.92f, h)
            lineTo(w * 0.08f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A1228), Color(0xFF0A0810)),
                startY = vanishY,
                endY = h,
            ),
        )

        val neon = TonalPalette.brightness(cyan, brightness * dim)
        drawPath(
            path = roadPath,
            color = TonalPalette.withAlpha(neon, 0.35f * dim),
            style = Stroke(width = 2.2f, cap = StrokeCap.Round),
        )

        val scroll = phase01(t)
        val k = 0.55f
        val step = 1f / markerCount
        val markerColor = TonalPalette.brightness(Color(0xFFE8E4D8), brightness * dim)
        for (i in 0 until markerCount) {
            val depth = phase01(i * step + scroll * step)
            val nearness = 1f / (1f + depth * markerCount * k)
            val y = vanishY + (h - vanishY) * (1f - nearness)
            val half = (w * 0.015f) + (w * 0.42f) * (1f - nearness)
            val mw = half * 0.1f
            val mh = mw * 2.8f
            val alpha = (0.2f + 0.7f * (1f - nearness)).coerceIn(0.1f, 0.85f)
            drawRect(
                color = TonalPalette.withAlpha(markerColor, alpha * dim),
                topLeft = Offset(vanishX - mw * 0.5f, y - mh * 0.5f),
                size = Size(mw, mh),
            )
        }

        // Roadside posts
        val postColor = TonalPalette.brightness(magenta, brightness * dim)
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until postCount) {
                val depth = phase01(i / postCount.toFloat() + scroll)
                val nearness = 1f / (1f + depth * postCount * 0.45f)
                val y = vanishY + (h - vanishY) * (1f - nearness)
                val half = (w * 0.02f) + (w * 0.48f) * (1f - nearness)
                val x = vanishX + side * half
                val postH = (8f + 28f * (1f - nearness))
                val alpha = (0.15f + 0.7f * (1f - nearness)).coerceIn(0.1f, 0.85f)
                drawLine(
                    color = TonalPalette.withAlpha(postColor, alpha * dim),
                    start = Offset(x, y),
                    end = Offset(x, y - postH),
                    strokeWidth = 1.5f + 2.5f * (1f - nearness),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = TonalPalette.withAlpha(neon, alpha * 0.9f * dim),
                    radius = 2f + 3f * (1f - nearness),
                    center = Offset(x, y - postH),
                )
            }
        }
    }
}
