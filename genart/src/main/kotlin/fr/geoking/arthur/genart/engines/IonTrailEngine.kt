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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * A lone probe drifting along a lazy S-curve above a [StarFieldEngine] backdrop, dragging an
 * exponentially-fading analytic trail of cyan-white glow segments behind it.
 */
@Composable
internal fun IonTrailEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "iontrail")
    val probeT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "iontrail_probe",
    )

    val dim = if (isActive) 1f else 0.6f
    val glowColor = TonalPalette.mix(Color(0xFFBFEFFF), TonalPalette.pick(paletteColors, 0), 0.25f)
    val tint = TonalPalette.brightness(glowColor, brightness)

    Box(modifier = modifier) {
        StarFieldEngine(isActive, paletteColors, quality, brightness, speed, Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = phase01(probeT)
            val baseY = h * 0.42f
            val amplitude = h * 0.16f
            val freq = 1.3f

            fun pathPoint(tt: Float): Offset {
                val x = tt * w
                val y = baseY + sin(tt * 2f * PI.toFloat() * freq) * amplitude
                return Offset(x, y)
            }

            val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val headAlpha = 0.75f * dim * edgeFade
            if (headAlpha <= 0.01f) return@Canvas

            val segmentCount = 18
            val trailSpan = 0.05f
            for (i in segmentCount downTo 0) {
                val back = i.toFloat() / segmentCount
                val tt = t - back * trailSpan
                if (tt < 0f) continue
                val point = pathPoint(tt)
                val decay = (1f - back).pow(2)
                val alpha = (headAlpha * decay).coerceIn(0f, 1f)
                if (alpha <= 0.01f) continue
                val radius = (1.2f + (1f - back) * 2.6f)
                drawCircle(
                    color = TonalPalette.withAlpha(tint, alpha),
                    radius = radius,
                    center = point,
                )
                drawCircle(
                    color = TonalPalette.withAlpha(Color.White, alpha * 0.5f),
                    radius = radius * 0.4f,
                    center = point,
                )
            }
        }
    }
}
