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

/** Trailer-style warp: dense cyan/magenta star-streaks from a bright vanishing point, soft flare — no hyperspace flash. */
@Composable
internal fun WarpStreakEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 22, high = 32)
    val streaks = remember(count) {
        List(count) { i ->
            WarpStreakSeed(
                angle = (i.toFloat() / count) * 2f * PI.toFloat() + seededRange(i * 13 + 5, -0.06f, 0.06f),
                lengthFrac = seededRange(i * 23 + 7, 0.5f, 1.05f),
                widthBase = seededRange(i * 31 + 11, 1.0f, 3.2f),
                alphaBase = seededRange(i * 41 + 17, 0.18f, 0.42f),
                swayAmp = seededRange(i * 53 + 19, 0.006f, 0.018f),
                swayFreq = seededRange(i * 61 + 23, 0.05f, 0.14f),
                colorBias = seededUnit(i * 71 + 29),
                colorIndex = i,
            )
        }
    }
    val wispCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val wisps = remember(wispCount) {
        List(wispCount) { i ->
            WarpWispSeed(
                angle = seededRange(i * 19 + 3, 0f, 2f * PI.toFloat()),
                distFrac = seededRange(i * 37 + 9, 0.18f, 0.55f),
                radiusFrac = seededRange(i * 47 + 13, 0.08f, 0.2f),
                alphaBase = seededRange(i * 59 + 17, 0.12f, 0.28f),
                pulsePhase = seededRange(i * 73 + 21, 0f, 2f * PI.toFloat()),
                colorIndex = i + 2,
            )
        }
    }
    val starCount = qualityCount(quality, low = 28, medium = 48, high = 72)
    val backgroundStars = remember(starCount) {
        List(starCount) { i ->
            val s = i * 89 + 5003
            WarpBackgroundStar(
                xFrac = seededUnit(s + 3),
                yFrac = seededUnit(s + 7),
                radius = 0.55f + seededUnit(s + 11) * 1.4f,
                alphaUnit = seededRange(s + 17, 0.2f, 0.7f),
                tintBias = seededUnit(s + 23),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "warpstreak")
    val swayT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((180000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "warpstreak_sway",
    )
    val breatheT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "warpstreak_breathe",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0A0618), Color(0xFF04030C), Color(0xFF010104)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = hypot(w, h) * 0.65f,
            ),
        )
        val dim = if (isActive) 1f else 0.55f
        val center = Offset(w * 0.5f, h * 0.5f)
        val reach = hypot(w, h) * 0.58f
        val time = phase01(swayT) * 2f * PI.toFloat()
        val breathe = sin01(phase01(breatheT) * 2f * PI.toFloat())
        val cyan = Color(0xFF5EEBFF)
        val magenta = Color(0xFFFF4FD8)
        val ice = Color(0xFFE8F8FF)

        backgroundStars.forEach { star ->
            val twinkle = 0.65f + 0.35f * breathe
            val tint = when {
                star.tintBias > 0.72f -> magenta
                star.tintBias > 0.45f -> cyan
                else -> ice
            }
            val alpha = (star.alphaUnit * twinkle * dim).coerceIn(0f, 1f)
            drawCircle(
                color = TonalPalette.withAlpha(tint, alpha),
                radius = star.radius,
                center = Offset(star.xFrac * w, star.yFrac * h),
            )
        }

        wisps.forEach { wisp ->
            val pulse = 0.75f + 0.25f * sin(time * 0.4f + wisp.pulsePhase)
            val a = wisp.angle + 0.04f * sin(time * 0.2f + wisp.pulsePhase)
            val dist = reach * wisp.distFrac
            val cx = center.x + sin(a) * dist
            val cy = center.y - cos(a) * dist
            val base = TonalPalette.mix(magenta, TonalPalette.pick(paletteColors, wisp.colorIndex), 0.35f)
            val tint = TonalPalette.brightness(base, brightness)
            val radius = minOf(w, h) * wisp.radiusFrac * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, wisp.alphaBase * dim * pulse),
                        TonalPalette.withAlpha(cyan, wisp.alphaBase * 0.35f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        streaks.forEach { streak ->
            val sway = streak.swayAmp * sin(time * streak.swayFreq)
            val a = streak.angle + sway
            val length = reach * streak.lengthFrac * (0.92f + 0.08f * breathe)
            val end = Offset(
                center.x + sin(a) * length,
                center.y - cos(a) * length,
            )
            val sciFi = if (streak.colorBias > 0.55f) magenta else cyan
            val base = TonalPalette.mix(sciFi, TonalPalette.pick(paletteColors, streak.colorIndex), 0.28f)
            val tint = TonalPalette.brightness(base, brightness)
            val alpha = streak.alphaBase * dim * (0.8f + 0.2f * breathe)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(ice, alpha * 0.95f),
                        TonalPalette.withAlpha(tint, alpha * 0.55f),
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

        // Soft anamorphic horizontal flare (trailer look, capped alpha)
        val flareAlpha = 0.22f * dim * (0.85f + 0.15f * breathe) * brightness.coerceAtMost(1.25f)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    TonalPalette.withAlpha(magenta, flareAlpha * 0.55f),
                    TonalPalette.withAlpha(ice, flareAlpha),
                    TonalPalette.withAlpha(cyan, flareAlpha * 0.55f),
                    Color.Transparent,
                ),
            ),
            start = Offset(0f, center.y),
            end = Offset(w, center.y),
            strokeWidth = minOf(w, h) * 0.012f,
            cap = StrokeCap.Round,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(ice, 0.55f * dim * brightness.coerceAtMost(1.3f)),
                    TonalPalette.withAlpha(cyan, 0.28f * dim),
                    TonalPalette.withAlpha(magenta, 0.12f * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = minOf(w, h) * 0.22f,
            ),
            radius = minOf(w, h) * 0.22f,
            center = center,
        )
    }
}

private data class WarpStreakSeed(
    val angle: Float,
    val lengthFrac: Float,
    val widthBase: Float,
    val alphaBase: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val colorBias: Float,
    val colorIndex: Int,
)

private data class WarpWispSeed(
    val angle: Float,
    val distFrac: Float,
    val radiusFrac: Float,
    val alphaBase: Float,
    val pulsePhase: Float,
    val colorIndex: Int,
)

private data class WarpBackgroundStar(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val alphaUnit: Float,
    val tintBias: Float,
)
