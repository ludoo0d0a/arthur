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
import kotlin.math.cos
import kotlin.math.sin

/** Warm sunbeams through haze — fixed origin, slow sway, soft falloff. */
@Composable
internal fun SunbeamsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 9)
    val beams = remember(count) {
        List(count) { i ->
            BeamSeed(
                angle = seededRange(i * 17 + 3, -0.55f, 0.55f),
                width = seededRange(i * 29 + 7, 0.04f, 0.1f),
                swayAmp = seededRange(i * 41 + 11, 0.02f, 0.06f),
                swayFreq = seededRange(i * 53 + 13, 0.15f, 0.5f),
                alphaBase = seededRange(i * 67 + 19, 0.06f, 0.18f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "sunbeams")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sunbeams_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A1408), Color(0xFF08060A), Color(0xFF040408)),
            ),
        )
        val origin = Offset(w * 0.5f, -h * 0.05f)
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val reach = h * 1.35f
        beams.forEach { beam ->
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val half = beam.width
            val left = Offset(
                origin.x + sin(a - half) * reach,
                origin.y + cos(a - half) * reach,
            )
            val right = Offset(
                origin.x + sin(a + half) * reach,
                origin.y + cos(a + half) * reach,
            )
            val path = Path().apply {
                moveTo(origin.x, origin.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            val base = TonalPalette.mix(Color(0xFFF5D78A), TonalPalette.pick(paletteColors, beam.colorIndex), 0.3f)
            val tint = TonalPalette.brightness(base, brightness)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, beam.alphaBase * dim),
                        Color.Transparent,
                    ),
                    start = origin,
                    end = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f),
                ),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFFFFF0C8), 0.35f * dim * brightness.coerceAtMost(1.2f)),
                    Color.Transparent,
                ),
                center = origin,
                radius = minOf(w, h) * 0.35f,
            ),
            radius = minOf(w, h) * 0.35f,
            center = origin,
        )
    }
}

private data class BeamSeed(
    val angle: Float,
    val width: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
