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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Wave-like flowing horizontal and vertical gradient washes with soft shadow undulations. */
@Composable
internal fun AuroraWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 8)
    val bands = remember(count) {
        List(count) { i ->
            AuroraBand(
                yBaseFrac = (i + 1f) / (count + 1f),
                amplitudeFrac = seededRange(i * 19 + 7, 0.08f, 0.18f),
                freq = seededRange(i * 29 + 11, 1.2f, 2.5f),
                speedMult = seededRange(i * 37 + 13, 0.8f, 1.5f),
                phaseOffset = seededRange(i * 47 + 17, 0f, 2f * PI.toFloat()),
                colorIdx = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "aurora_wash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((25000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora_wash_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF020813))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        bands.forEach { band ->
            val c = TonalPalette.brightness(TonalPalette.pick(paletteColors, band.colorIdx), brightness)
            val path = Path()
            val shadowPath = Path()

            val steps = 40
            val shadowOffsetY = h * 0.04f

            path.moveTo(0f, h)
            shadowPath.moveTo(0f, h)

            for (s in 0..steps) {
                val x = (s.toFloat() / steps) * w
                val angle = (s.toFloat() / steps) * 2f * PI.toFloat() * band.freq + time * 2f * PI.toFloat() * band.speedMult + band.phaseOffset
                val y = band.yBaseFrac * h + sin(angle) * band.amplitudeFrac * h

                if (s == 0) {
                    path.lineTo(x, y)
                    shadowPath.lineTo(x, y + shadowOffsetY)
                } else {
                    path.lineTo(x, y)
                    shadowPath.lineTo(x, y + shadowOffsetY)
                }
            }

            path.lineTo(w, h)
            path.close()
            shadowPath.lineTo(w, h)
            shadowPath.close()

            // Soft shadow under ribbon
            drawPath(
                path = shadowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.35f * dim),
                        Color.Transparent,
                    ),
                    startY = band.yBaseFrac * h,
                    endY = h,
                ),
            )

            // Aurora ribbon wash
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(c, 0.55f * dim),
                        TonalPalette.withAlpha(c, 0.15f * dim),
                        Color.Transparent,
                    ),
                    startY = (band.yBaseFrac - band.amplitudeFrac) * h,
                    endY = h,
                ),
            )
        }
    }
}

private data class AuroraBand(
    val yBaseFrac: Float,
    val amplitudeFrac: Float,
    val freq: Float,
    val speedMult: Float,
    val phaseOffset: Float,
    val colorIdx: Int,
)
