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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.fbm2D
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A massive sun disc with lava prominences arcing off the rim — fire-like tongues thrown outward.
 */
@Composable
internal fun LavaSunEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val tongueCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val tongues = remember(tongueCount) {
        List(tongueCount) { i ->
            LavaSunTongueSeed(
                angle = seededRange(i * 17 + 3, 0f, 2f * PI.toFloat()),
                lengthFrac = seededRange(i * 29 + 7, 0.18f, 0.42f),
                widthFrac = seededRange(i * 41 + 11, 0.04f, 0.1f),
                swayAmp = seededRange(i * 53 + 13, 0.08f, 0.22f),
                swayFreq = seededRange(i * 67 + 19, 0.8f, 2.2f),
                flickerPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val mottling = remember {
        List(6) { i ->
            LavaSunMottleSeed(
                angle = seededRange(i * 91 + 3, 0f, 2f * PI.toFloat()),
                distFrac = seededRange(i * 103 + 7, 0.15f, 0.7f),
                radiusFrac = seededRange(i * 113 + 11, 0.08f, 0.18f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "lava_sun")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lava_sun_t",
    )
    val pulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lava_sun_pulse",
    )

    val dim = if (isActive) 1f else 0.55f
    val time = phase01(t) * 2f * PI.toFloat()
    val pulse = 0.88f + 0.12f * sin01(phase01(pulseT) * 2f * PI.toFloat())

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF080204))
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(20.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val sunR = minDim * 0.28f
            val corona = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFAA44), TonalPalette.pick(paletteColors, 0), 0.25f),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(corona, 0.55f * dim * pulse),
                        TonalPalette.withAlpha(corona, 0.18f * dim * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = sunR * 2.4f,
                ),
                radius = sunR * 2.4f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val sunR = minDim * 0.28f

            tongues.forEach { tongue ->
                val noise = fbm2D(time * 0.4f, tongue.angle * 3f, octaves = 2, seedOffset = tongue.colorIndex * 101)
                val sway = sin(time * tongue.swayFreq + tongue.flickerPhase) * tongue.swayAmp * noise
                val angle = tongue.angle + sway
                val breath = 0.85f + 0.15f * loopedFbm(time * 0.25f, radius = 1f, octaves = 2, seedOffset = tongue.colorIndex * 307)
                val length = tongue.lengthFrac * minDim * breath * pulse
                val halfW = tongue.widthFrac * minDim
                val baseX = cx + cos(angle) * sunR * 0.92f
                val baseY = cy + sin(angle) * sunR * 0.92f
                val tipX = cx + cos(angle) * (sunR + length)
                val tipY = cy + sin(angle) * (sunR + length)
                val perpX = -sin(angle)
                val perpY = cos(angle)
                val tint = TonalPalette.mix(
                    TonalPalette.pick(paletteColors, tongue.colorIndex),
                    Color(0xFFFF6622),
                    0.45f,
                )
                val flame = TonalPalette.brightness(tint, brightness)
                val core = TonalPalette.mix(flame, Color(0xFFFFEEAA), 0.4f)
                val path = Path().apply {
                    moveTo(baseX + perpX * halfW, baseY + perpY * halfW)
                    cubicTo(
                        baseX + perpX * halfW * 1.4f + cos(angle) * length * 0.35f,
                        baseY + perpY * halfW * 1.4f + sin(angle) * length * 0.35f,
                        tipX + perpX * halfW * 0.2f,
                        tipY + perpY * halfW * 0.2f,
                        tipX,
                        tipY,
                    )
                    cubicTo(
                        tipX - perpX * halfW * 0.2f,
                        tipY - perpY * halfW * 0.2f,
                        baseX - perpX * halfW * 1.4f + cos(angle) * length * 0.35f,
                        baseY - perpY * halfW * 1.4f + sin(angle) * length * 0.35f,
                        baseX - perpX * halfW,
                        baseY - perpY * halfW,
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(core, 0.85f * dim),
                            TonalPalette.withAlpha(flame, 0.55f * dim),
                            TonalPalette.withAlpha(flame, 0.05f * dim),
                        ),
                        start = Offset(baseX, baseY),
                        end = Offset(tipX, tipY),
                    ),
                    style = Fill,
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF0C8),
                        Color(0xFFFFB84A),
                        Color(0xFFFF6A22),
                    ),
                    center = Offset(cx - sunR * 0.2f, cy - sunR * 0.2f),
                    radius = sunR * 1.2f,
                ),
                radius = sunR,
                center = Offset(cx, cy),
            )
            mottling.forEach { spot ->
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFFFF4411), 0.25f * dim),
                    radius = spot.radiusFrac * sunR,
                    center = Offset(
                        cx + cos(spot.angle) * spot.distFrac * sunR,
                        cy + sin(spot.angle) * spot.distFrac * sunR,
                    ),
                )
            }
        }
    }
}

private data class LavaSunTongueSeed(
    val angle: Float,
    val lengthFrac: Float,
    val widthFrac: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val flickerPhase: Float,
    val colorIndex: Int,
)

private data class LavaSunMottleSeed(
    val angle: Float,
    val distFrac: Float,
    val radiusFrac: Float,
)
