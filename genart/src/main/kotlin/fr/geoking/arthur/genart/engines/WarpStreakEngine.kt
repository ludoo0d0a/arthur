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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Faint star-streaks creeping outward from a distant vanishing point — a slow nebula glow, not a hyperspace jump. */
@Composable
internal fun WarpStreakEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 12, high = 18)
    val streaks = remember(count) {
        List(count) { i ->
            StreakSeed(
                angle = (i.toFloat() / count) * 2f * PI.toFloat() + seededRange(i * 13 + 5, -0.08f, 0.08f),
                lengthFrac = seededRange(i * 23 + 7, 0.55f, 0.95f),
                widthBase = seededRange(i * 31 + 11, 1.1f, 2.4f),
                alphaBase = seededRange(i * 41 + 17, 0.1f, 0.26f),
                swayAmp = seededRange(i * 53 + 19, 0.008f, 0.02f),
                swayFreq = seededRange(i * 61 + 23, 0.05f, 0.13f),
                colorIndex = i,
            )
        }
    }
    val starCount = qualityCount(quality, low = 18, medium = 28, high = 40)
    val backgroundStars = remember(starCount) {
        List(starCount) { i ->
            val s = i * 89 + 5003
            BackgroundStar(
                xFrac = seededUnit(s + 3),
                yFrac = seededUnit(s + 7),
                radius = 0.6f + seededUnit(s + 11) * 1.1f,
                alphaUnit = seededRange(s + 17, 0.15f, 0.5f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "warpstreak")
    val swayT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((240000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "warpstreak_sway",
    )
    val breatheT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "warpstreak_breathe",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF04060E), Color(0xFF010104), Color(0xFF000000)),
            ),
        )
        val dim = if (isActive) 1f else 0.55f
        val center = Offset(w * 0.5f, h * 0.5f)
        val reach = hypot(w, h) * 0.55f
        val time = phase01(swayT) * 2f * PI.toFloat()
        val breathe = sin01(phase01(breatheT) * 2f * PI.toFloat())

        backgroundStars.forEach { star ->
            val twinkle = 0.7f + 0.3f * breathe
            val alpha = (star.alphaUnit * twinkle * dim).coerceIn(0f, 1f)
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFE8F1FF), alpha),
                radius = star.radius,
                center = Offset(star.xFrac * w, star.yFrac * h),
            )
        }

        streaks.forEach { streak ->
            val sway = streak.swayAmp * sin(time * streak.swayFreq)
            val a = streak.angle + sway
            val length = reach * streak.lengthFrac * (0.94f + 0.06f * breathe)
            val end = Offset(
                center.x + sin(a) * length,
                center.y - cos(a) * length,
            )
            val base = TonalPalette.mix(Color(0xFFCFE8FF), TonalPalette.pick(paletteColors, streak.colorIndex), 0.22f)
            val tint = TonalPalette.brightness(base, brightness)
            val alpha = streak.alphaBase * dim * (0.85f + 0.15f * breathe)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, alpha),
                        Color.Transparent,
                    ),
                    start = center,
                    end = end,
                ),
                start = center,
                end = end,
                strokeWidth = streak.widthBase,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFFEAF4FF), 0.3f * dim * brightness.coerceAtMost(1.2f)),
                    Color.Transparent,
                ),
                center = center,
                radius = minOf(w, h) * 0.18f,
            ),
            radius = minOf(w, h) * 0.18f,
            center = center,
        )
    }
}

private data class StreakSeed(
    val angle: Float,
    val lengthFrac: Float,
    val widthBase: Float,
    val alphaBase: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val colorIndex: Int,
)

private data class BackgroundStar(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val alphaUnit: Float,
)
