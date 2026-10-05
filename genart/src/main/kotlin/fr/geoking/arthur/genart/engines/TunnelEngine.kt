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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Vanishing tunnel: concentric rings + radial spokes rushing into a vanishing point.
 * Perspective keeps far rings small/dim at the center and near rings large/bright.
 */
@Composable
internal fun TunnelEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rings = qualityCount(quality, low = 10, medium = 14, high = 20)
    val spokes = qualityCount(quality, low = 8, medium = 12, high = 16)
    val transition = rememberInfiniteTransition(label = "tunnel")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tunnel_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = min(w, h) * 0.78f
        val bright = brightness.coerceIn(0.35f, 1.4f) * dim

        drawRect(color = Color(0xFF020617))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF0B1026), Color(0xFF020617)),
                center = Offset(cx, cy),
                radius = maxR,
            ),
            radius = maxR,
            center = Offset(cx, cy),
        )

        val scroll = phase01(t)
        val spin = scroll * 0.35f * 2f * PI.toFloat()
        val step = 1f / rings
        // Same perspective constant as Roads — near walls fill the frame.
        val k = 0.55f

        val spokeInner = maxR * 0.04f
        for (s in 0 until spokes) {
            val angle = spin + s * (2f * PI.toFloat() / spokes)
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, s), bright)
            drawLine(
                color = TonalPalette.withAlpha(color, 0.28f * bright.coerceAtMost(1f)),
                start = Offset(cx + cos(angle) * spokeInner, cy + sin(angle) * spokeInner),
                end = Offset(cx + cos(angle) * maxR, cy + sin(angle) * maxR),
                strokeWidth = if (isActive) 2.4f else 1.5f,
                cap = StrokeCap.Round,
            )
        }

        // depth 0 = nearest (large); as scroll rises rings shrink into the void, then recycle.
        for (i in 0 until rings) {
            val depth = phase01(i * step + scroll)
            val scale = 1f / (1f + depth * rings * k)
            val radius = (maxR * scale).coerceAtLeast(2f)
            val alpha = (0.22f + 0.68f * scale).coerceIn(0.15f, 0.92f) * bright.coerceAtMost(1.1f)
            val stroke = (1.4f + 5f * scale).coerceIn(1.4f, 6.5f) * if (isActive) 1f else 0.7f
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), bright)
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = stroke),
            )
        }

        val coreColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), bright)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(coreColor, 0.55f * bright.coerceAtMost(1f)),
                    TonalPalette.withAlpha(coreColor, 0.12f * bright.coerceAtMost(1f)),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = maxR * 0.12f,
            ),
            radius = maxR * 0.12f,
            center = Offset(cx, cy),
        )
    }
}
