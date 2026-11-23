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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class DinerRainDrop(val x: Float, val y: Float, val length: Float, val speed: Float)

/** Soft Ambient POV US Diner Neon — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun UsDinerNeonEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val drops = remember(count) {
        List(count) { i ->
            DinerRainDrop(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                length = seededRange(i * 41 + 11, 0.035f, 0.10f),
                speed = seededRange(i * 53 + 13, 0.06f, 0.14f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "usdinerneon")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "usdinerneon_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val pulse = 0.92f + 0.08f * loopedFbm(time, radius = 1.1f, seedOffset = 23)
        val pink = TonalPalette.brightness(TonalPalette.mix(Color(0xFFFF40A0), TonalPalette.pick(paletteColors, 0), 0.2f), brightness * dim)
        val cyan = TonalPalette.brightness(Color(0xFF4DEBFF), brightness * dim)
        val dark = TonalPalette.brightness(Color(0xFF100818), brightness * dim)
        val horizon = h * 0.60f
        val dinerLeft = w * 0.17f
        val dinerRight = w * 0.83f
        val dinerTop = h * 0.32f
        val dinerBottom = h * 0.71f

        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF07060D), dark, Color(0xFF080A10))))
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(pink, 0.22f * pulse * dim),
                0.58f to TonalPalette.withAlpha(Color(0xFF50143C), 0.10f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.50f, h * 0.57f),
                radius = unit * 0.72f,
            ),
            radius = unit * 0.72f,
            center = Offset(w * 0.50f, h * 0.57f),
        )

        // Low dark building against a hazy night sky.
        drawRoundRect(
            color = Color(0xFF100D18),
            topLeft = Offset(dinerLeft, dinerTop),
            size = Size(dinerRight - dinerLeft, dinerBottom - dinerTop),
            cornerRadius = CornerRadius(unit * 0.025f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF211326),
                0.36f to Color(0xFF16101E),
                1f to Color(0xFF0C0B12),
                startY = dinerTop,
                endY = dinerBottom,
            ),
            topLeft = Offset(dinerLeft + unit * 0.012f, dinerTop + unit * 0.012f),
            size = Size(dinerRight - dinerLeft - unit * 0.024f, dinerBottom - dinerTop - unit * 0.012f),
        )

        // Neon fascia: rounded sign with a double pink/cyan tube border.
        val signLeft = w * 0.30f
        val signTop = h * 0.24f
        val signSize = Size(w * 0.40f, h * 0.15f)
        drawRoundRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(pink, 0.20f * pulse * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, signTop + signSize.height * 0.5f),
                radius = unit * 0.47f,
            ),
            topLeft = Offset(w * 0.22f, signTop - unit * 0.14f),
            size = Size(w * 0.56f, unit * 0.44f),
            cornerRadius = CornerRadius(unit * 0.18f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(pink, 0.22f * pulse * dim),
            topLeft = Offset(signLeft, signTop),
            size = signSize,
            cornerRadius = CornerRadius(unit * 0.018f),
            style = Stroke(width = unit * 0.018f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(pink, 0.90f * pulse * dim),
            topLeft = Offset(signLeft, signTop),
            size = signSize,
            cornerRadius = CornerRadius(unit * 0.018f),
            style = Stroke(width = unit * 0.006f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(cyan, 0.78f * dim),
            topLeft = Offset(signLeft + unit * 0.012f, signTop + unit * 0.012f),
            size = Size(signSize.width - unit * 0.024f, signSize.height - unit * 0.024f),
            cornerRadius = CornerRadius(unit * 0.012f),
            style = Stroke(width = unit * 0.003f),
        )
        // Abstract glowing letter strokes, legible as a retro sign without hard typography.
        val letterY = signTop + signSize.height * 0.28f
        val letterH = signSize.height * 0.45f
        for (i in 0..4) {
            val x = signLeft + signSize.width * (0.16f + i * 0.15f)
            drawLine(pink.copy(alpha = 0.92f * dim), Offset(x, letterY), Offset(x, letterY + letterH), unit * 0.007f)
            drawLine(cyan.copy(alpha = 0.76f * dim), Offset(x, letterY), Offset(x + signSize.width * 0.065f, letterY), unit * 0.004f)
            drawLine(pink.copy(alpha = 0.70f * dim), Offset(x, letterY + letterH * 0.50f), Offset(x + signSize.width * 0.055f, letterY + letterH * 0.50f), unit * 0.004f)
            drawLine(cyan.copy(alpha = 0.72f * dim), Offset(x, letterY + letterH), Offset(x + signSize.width * 0.065f, letterY + letterH), unit * 0.004f)
        }

        // Front windows and door with alternating colored neon edges.
        val windowTop = h * 0.45f
        val windowBottom = h * 0.65f
        for (i in 0..2) {
            val x = w * (0.23f + i * 0.20f)
            val ww = w * 0.15f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF27172D),
                    0.45f to Color(0xFF120F1B),
                    1f to Color(0xFF090C13),
                    startY = windowTop,
                    endY = windowBottom,
                ),
                topLeft = Offset(x, windowTop),
                size = Size(ww, windowBottom - windowTop),
                cornerRadius = CornerRadius(unit * 0.009f),
            )
            drawRoundRect(
                color = TonalPalette.withAlpha(if (i == 1) cyan else pink, 0.76f * dim),
                topLeft = Offset(x, windowTop),
                size = Size(ww, windowBottom - windowTop),
                cornerRadius = CornerRadius(unit * 0.009f),
                style = Stroke(width = unit * 0.003f),
            )
            drawRect(color = Color(0xFF080A10), topLeft = Offset(x + ww * 0.48f, windowTop), size = Size(unit * 0.004f, windowBottom - windowTop))
            drawLine(Color(0xFF191323), Offset(x + ww * 0.08f, windowTop + unit * 0.014f), Offset(x + ww * 0.92f, windowTop + unit * 0.014f), unit * 0.003f)
        }
        drawRect(color = Color(0xFF0B0A11), topLeft = Offset(dinerLeft - unit * 0.02f, dinerBottom), size = Size(dinerRight - dinerLeft + unit * 0.04f, h * 0.055f))

        // Wet street, broad soft reflections under the neon facade.
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF090A11), Color(0xFF07080D)), startY = horizon, endY = h), topLeft = Offset(0f, horizon), size = Size(w, h - horizon))
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(pink, 0.16f * pulse * dim),
                0.55f to TonalPalette.withAlpha(pink, 0.035f * dim),
                1f to Color.Transparent,
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(w * 0.20f, horizon),
            size = Size(w * 0.60f, h - horizon),
        )
        for (i in 0..4) {
            val y = horizon + (i + 1) * h * 0.065f
            val shrink = 1f - i * 0.12f
            drawLine(
                color = TonalPalette.withAlpha(if (i % 2 == 0) pink else cyan, (0.16f - i * 0.022f) * pulse * dim),
                start = Offset(w * (0.5f - 0.27f * shrink), y),
                end = Offset(w * (0.5f + 0.27f * shrink), y + h * 0.008f),
                strokeWidth = unit * (0.003f - i * 0.0003f),
            )
        }

        // Fine rain streaks on the viewing glass and haze particles.
        drops.forEachIndexed { i, drop ->
            val n = loopedFbm(time, radius = 1.2f, seedOffset = 91 + i * 13)
            val x = drop.x * w + sin(time * 2f * PI.toFloat() + i) * w * 0.006f
            val y = ((drop.y + time * drop.speed) % 1f) * h
            drawLine(
                color = TonalPalette.withAlpha(cyan, (0.09f + n * 0.09f) * dim),
                start = Offset(x, y),
                end = Offset(x - w * 0.004f, y + h * drop.length),
                strokeWidth = unit * 0.0015f,
            )
        }
    }
}
