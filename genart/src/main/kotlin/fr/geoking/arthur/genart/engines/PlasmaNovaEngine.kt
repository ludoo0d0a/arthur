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
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Trailer-style plasma nova: white-hot core, fibrous magenta/cyan filaments, soft anamorphic flare.
 * Expands and breathes slowly — capped alpha, no strobe.
 */
@Composable
internal fun PlasmaNovaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val filamentCount = qualityCount(quality, low = 48, medium = 72, high = 110)
    val filaments = remember(filamentCount) {
        List(filamentCount) { i ->
            PlasmaNovaFilament(
                angle = seededRange(i * 17 + 3, 0f, 2f * PI.toFloat()),
                lengthMul = seededRange(i * 29 + 7, 0.35f, 1.15f),
                thickness = seededRange(i * 41 + 11, 0.9f, 2.8f),
                wobble = seededRange(i * 53 + 13, -0.22f, 0.22f),
                alpha = seededRange(i * 67 + 19, 0.2f, 0.55f),
                hueBias = seededUnit(i * 79 + 23),
                colorIdx = i,
            )
        }
    }
    val starCount = qualityCount(quality, low = 40, medium = 70, high = 110)
    val stars = remember(starCount) {
        List(starCount) { i ->
            val s = i * 97 + 4001
            PlasmaNovaStar(
                xFrac = seededUnit(s),
                yFrac = seededUnit(s + 5),
                radius = 0.5f + seededUnit(s + 11) * 1.3f,
                alpha = seededRange(s + 17, 0.15f, 0.65f),
            )
        }
    }
    val dripCount = qualityCount(quality, low = 12, medium = 20, high = 30)
    val drips = remember(dripCount) {
        List(dripCount) { i ->
            PlasmaNovaDrip(
                angle = seededRange(i * 23 + 5, 0f, 2f * PI.toFloat()),
                startFrac = seededRange(i * 37 + 9, 0.25f, 0.55f),
                lengthFrac = seededRange(i * 47 + 13, 0.12f, 0.35f),
                alpha = seededRange(i * 59 + 17, 0.15f, 0.4f),
                phase = seededUnit(i * 71 + 21),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "plasma_nova")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "plasma_nova_t",
    )
    val breatheT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "plasma_nova_breathe",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val minDim = minOf(w, h)
        val center = Offset(w * 0.5f, h * 0.48f)
        drawRect(color = Color(0xFF010106))

        val time = phase01(t)
        val breathe = sin01(phase01(breatheT) * 2f * PI.toFloat())
        // Soft nova cycle: expand → hold → fade (no hard flash)
        val cycle = time
        val expand = when {
            cycle < 0.35f -> (cycle / 0.35f).coerceIn(0f, 1f)
            cycle < 0.7f -> 1f
            else -> (1f - (cycle - 0.7f) / 0.3f).coerceIn(0.35f, 1f)
        }
        val intensity = when {
            cycle < 0.12f -> (cycle / 0.12f)
            cycle > 0.85f -> ((1f - cycle) / 0.15f).coerceIn(0f, 1f)
            else -> 0.85f + 0.15f * breathe
        }.coerceIn(0.35f, 1f)
        val dim = if (isActive) 1f else 0.55f
        // expand starts at 0 each loop — RadialGradient crashes if radius <= 0.
        val reach = (hypot(w, h) * 0.42f * expand).positiveRadius()
        val cyan = Color(0xFF4EFFF8)
        val magenta = Color(0xFFFF2EB8)
        val violet = Color(0xFFB44DFF)
        val ice = Color(0xFFF7FCFF)

        stars.forEach { star ->
            drawCircle(
                color = TonalPalette.withAlpha(ice, star.alpha * dim * 0.7f),
                radius = star.radius.positiveRadius(0.5f),
                center = Offset(star.xFrac * w, star.yFrac * h),
            )
        }

        // Outer plasma cloud
        val outerR = (reach * 1.15f).positiveRadius()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(magenta, 0.35f * intensity * dim),
                    TonalPalette.withAlpha(violet, 0.2f * intensity * dim),
                    TonalPalette.withAlpha(cyan, 0.08f * intensity * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = outerR,
            ),
            radius = outerR,
            center = center,
        )

        filaments.forEach { f ->
            val accent = when {
                f.hueBias > 0.7f -> cyan
                f.hueBias > 0.35f -> magenta
                else -> violet
            }
            val tint = TonalPalette.brightness(
                TonalPalette.mix(accent, TonalPalette.pick(paletteColors, f.colorIdx), 0.22f),
                brightness,
            )
            val a = f.angle + f.wobble * 0.15f * sin(time * 2f * PI.toFloat() + f.angle)
            val len = reach * f.lengthMul
            val start = center
            val mid = Offset(
                center.x + cos(a + f.wobble * 0.3f) * len * 0.45f,
                center.y + sin(a + f.wobble * 0.3f) * len * 0.45f,
            )
            val end = Offset(
                center.x + cos(a + f.wobble) * len,
                center.y + sin(a + f.wobble) * len,
            )
            val aMul = f.alpha * intensity * dim
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(ice, aMul * 0.9f),
                        TonalPalette.withAlpha(tint, aMul * 0.55f),
                        Color.Transparent,
                    ),
                    start = start,
                    end = end,
                ),
                start = start,
                end = mid,
                strokeWidth = f.thickness,
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, aMul * 0.45f),
                        Color.Transparent,
                    ),
                    start = mid,
                    end = end,
                ),
                start = mid,
                end = end,
                strokeWidth = f.thickness * 0.65f,
                cap = StrokeCap.Round,
            )
        }

        drips.forEach { drip ->
            val drift = phase01(time + drip.phase)
            val a = drip.angle
            val r0 = reach * drip.startFrac
            val r1 = r0 + reach * drip.lengthFrac * (0.6f + 0.4f * drift)
            val s = Offset(center.x + cos(a) * r0, center.y + sin(a) * r0)
            val e = Offset(center.x + cos(a) * r1, center.y + sin(a) * r1 + minDim * 0.04f * drift)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(cyan, drip.alpha * intensity * dim),
                        Color.Transparent,
                    ),
                    start = s,
                    end = e,
                ),
                start = s,
                end = e,
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
        }

        // Anamorphic flare
        val flareA = 0.28f * intensity * dim * brightness.coerceAtMost(1.3f)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    TonalPalette.withAlpha(magenta, flareA * 0.5f),
                    TonalPalette.withAlpha(ice, flareA),
                    TonalPalette.withAlpha(cyan, flareA * 0.5f),
                    Color.Transparent,
                ),
            ),
            start = Offset(0f, center.y),
            end = Offset(w, center.y),
            strokeWidth = minDim * 0.014f,
            cap = StrokeCap.Round,
        )

        // White-hot core
        val coreR = (minDim * (0.1f + 0.04f * expand) * (0.9f + 0.1f * breathe) * 2.2f)
            .positiveRadius()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(ice, 0.85f * intensity * dim * brightness.coerceAtMost(1.4f)),
                    TonalPalette.withAlpha(cyan, 0.45f * intensity * dim),
                    TonalPalette.withAlpha(magenta, 0.2f * intensity * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = coreR,
            ),
            radius = coreR,
            center = center,
        )
    }
}

private data class PlasmaNovaFilament(
    val angle: Float,
    val lengthMul: Float,
    val thickness: Float,
    val wobble: Float,
    val alpha: Float,
    val hueBias: Float,
    val colorIdx: Int,
)

private data class PlasmaNovaStar(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val alpha: Float,
)

private data class PlasmaNovaDrip(
    val angle: Float,
    val startFrac: Float,
    val lengthFrac: Float,
    val alpha: Float,
    val phase: Float,
)
