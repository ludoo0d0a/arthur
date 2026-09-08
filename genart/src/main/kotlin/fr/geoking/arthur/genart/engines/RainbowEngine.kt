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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

private val RainbowSpectrum = listOf(
    Color(0xFFC9A6FF),
    Color(0xFFA6B8FF),
    Color(0xFF9BD3FF),
    Color(0xFFA6F0C6),
    Color(0xFFFFF3A6),
    Color(0xFFFFCBA0),
    Color(0xFFFFA6A6),
)

/** Soft pastel rainbow bowing across the top of the frame, breathing slowly. */
@Composable
internal fun RainbowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val haloLayers = qualityCount(quality, low = 1, medium = 2, high = 3)

    val transition = rememberInfiniteTransition(label = "rainbow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainbow_t",
    )

    val dim = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.4f)
        val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.24f)
        val skyBot = Color(0xFF0A1018)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, skyBot),
            ),
        )

        val breathe = 0.55f + 0.45f * sin01(phase01(t) * 2f * PI.toFloat())

        val centerX = w * 0.5f
        val centerY = h * 1.35f
        val bandGap = h * 0.045f
        val baseRadius = h * 1.25f
        val bandWidth = bandGap * 0.85f

        val extraOuter = haloLayers - 1
        for (i in RainbowSpectrum.indices) {
            val radius = baseRadius + i * bandGap
            val base = RainbowSpectrum[i]
            val tint = TonalPalette.brightness(base, brightness.coerceAtMost(1.2f))
            val alpha = 0.14f * dim * breathe
            drawArc(
                color = TonalPalette.withAlpha(tint, alpha),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = bandWidth),
            )
        }

        for (i in 0 until extraOuter) {
            val radius = baseRadius + (RainbowSpectrum.size + i) * bandGap
            val tint = TonalPalette.brightness(RainbowSpectrum.last(), brightness.coerceAtMost(1.2f))
            val alpha = 0.05f * dim * breathe
            drawArc(
                color = TonalPalette.withAlpha(tint, alpha),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = bandWidth),
            )
        }
    }
}
