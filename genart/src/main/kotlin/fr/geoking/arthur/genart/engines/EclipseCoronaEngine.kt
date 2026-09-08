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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Dark occluding disc over a starfield, rimmed by a softly blurred corona that shimmers gently — never a hard flash (car-safe). */
@Composable
internal fun EclipseCoronaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "eclipse_corona")
    val shimmerT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eclipse_corona_shimmer",
    )

    val dim = if (isActive) 1f else 0.55f
    // Corona shimmer: slow sine, capped tight (alpha ~0.5..0.7), never a hard flash — same pattern as Storm's glow pulse.
    val shimmer = sin01(phase01(shimmerT) * 2f * PI.toFloat())
    val ringAlpha = (0.5f + 0.2f * shimmer) * dim

    Box(modifier = modifier) {
        StarFieldEngine(
            isActive = isActive,
            paletteColors = paletteColors,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize().blur(24.dp)) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val discRadius = w.coerceAtMost(h) * 0.22f
            val coronaRadius = w.coerceAtMost(h) * 0.42f
            val ringColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFF3D6), TonalPalette.pick(paletteColors, 0), 0.25f),
                brightness,
            )
            val innerStop = (discRadius / coronaRadius).coerceIn(0.05f, 0.95f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    innerStop to TonalPalette.withAlpha(ringColor, ringAlpha),
                    1f to Color.Transparent,
                    center = center,
                    radius = coronaRadius,
                ),
                radius = coronaRadius,
                center = center,
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val discRadius = w.coerceAtMost(h) * 0.22f
            drawCircle(
                color = Color(0xFF05060A),
                radius = discRadius,
                center = Offset(w * 0.5f, h * 0.5f),
            )
        }
    }
}
