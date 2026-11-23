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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Sitting cat silhouette with a slowly swaying tail in a moody twilight room. */
@Composable
internal fun CatTailEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cat_tail_loop")
    val swayAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_tail_sway",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((55000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_tail_drift",
    )
    val restingDim = if (isActive) 1f else 0.65f
    val detail = when (quality) {
        GenartQuality.Low -> 0
        GenartQuality.Medium -> 1
        GenartQuality.High -> 2
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.48f
        val cy = h * 0.58f
        val t = phase01(swayAnim)
        val dt = phase01(driftAnim)
        val haze = loopedFbm(dt, radius = 1.3f, octaves = 2, seedOffset = 31)

        // Deep moody twilight room background
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2A2430),
                0.5f to Color(0xFF1C1822),
                1f to Color(0xFF100E14),
                center = Offset(cx, cy),
                radius = w.coerceAtLeast(h) * 0.85f,
            ),
        )

        // Soft ambient light pool on the floor
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(
                    TonalPalette.mix(Color(0xFF3A2E3A), TonalPalette.pick(paletteColors, 1), 0.15f),
                    0.3f * restingDim,
                ),
                1f to Color.Transparent,
                center = Offset(cx, h * 0.78f),
                radius = minDim * 0.42f,
            ),
            topLeft = Offset(cx - minDim * 0.42f, h * 0.68f),
            size = Size(minDim * 0.84f, minDim * 0.2f),
        )

        // Faint window glow behind (suggesting twilight outside)
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF4A3858), 0.22f * restingDim + 0.05f * haze),
                0.6f to TonalPalette.withAlpha(Color(0xFF2A1E30), 0.08f),
                1f to Color.Transparent,
                center = Offset(w * 0.72f, h * 0.2f),
                radius = w * 0.35f,
            ),
        )

        // Cat silhouette
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF1C1614), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * restingDim,
        )
        val bodyRx = minDim * 0.16f
        val bodyRy = minDim * 0.20f
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )

        // Head
        val headR = minDim * 0.11f
        val head = Offset(cx, cy - bodyRy * 1.15f)
        drawCircle(color = bodyColor, radius = headR, center = head)

        // Pointed ears
        val ears = Path().apply {
            moveTo(head.x - headR * 0.7f, head.y - headR * 0.2f)
            lineTo(head.x - headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x - headR * 0.15f, head.y - headR * 0.5f)
            close()
            moveTo(head.x + headR * 0.15f, head.y - headR * 0.5f)
            lineTo(head.x + headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x + headR * 0.7f, head.y - headR * 0.2f)
            close()
        }
        drawPath(ears, color = bodyColor)

        // Eye glints (Medium/High)
        if (detail > 0) {
            val eyeColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFF8A9AB0), TonalPalette.pick(paletteColors, 1), 0.3f),
                brightness * restingDim,
            )
            val eyeR = headR * 0.08f
            val blink = sin(dt * 6f * PI.toFloat()) * 0.5f + 0.5f
            val eyeAlpha = if (blink > 0.92f) 0f else (0.55f + 0.15f * haze) * restingDim
            drawCircle(
                color = TonalPalette.withAlpha(eyeColor, eyeAlpha),
                radius = eyeR,
                center = Offset(head.x - headR * 0.3f, head.y + headR * 0.05f),
            )
            drawCircle(
                color = TonalPalette.withAlpha(eyeColor, eyeAlpha),
                radius = eyeR,
                center = Offset(head.x + headR * 0.3f, head.y + headR * 0.05f),
            )
        }

        // Front paws
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx * 0.55f, cy + bodyRy * 0.55f),
            size = Size(minDim * 0.065f, minDim * 0.055f),
        )
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx + bodyRx * 0.15f, cy + bodyRy * 0.55f),
            size = Size(minDim * 0.065f, minDim * 0.055f),
        )

        // Swaying tail
        val sway = sin(t * 2f * PI.toFloat()) * (if (isActive) 1f else 0.35f)
        val tail = Path().apply {
            val base = Offset(cx + bodyRx * 0.75f, cy + bodyRy * 0.15f)
            val mid = Offset(
                base.x + minDim * (0.12f + 0.04f * sway),
                base.y - minDim * 0.18f,
            )
            val tip = Offset(
                base.x + minDim * (0.08f + 0.12f * sway),
                base.y - minDim * 0.38f,
            )
            moveTo(base.x, base.y)
            quadraticTo(mid.x, mid.y, tip.x, tip.y)
        }
        val strokeW = minDim * (if (quality == GenartQuality.Low) 0.028f else 0.032f)
        drawPath(tail, color = bodyColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

        // Floating dust motes in the twilight room
        if (detail > 0) {
            val moteCount = if (detail == 2) 5 else 3
            val moteGlow = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
            for (i in 0 until moteCount) {
                val mx = w * (0.2f + 0.14f * i + 0.03f * sin(dt * 2f * PI.toFloat() + i * 1.4f))
                val my = h * (0.2f + 0.1f * i + 0.04f * sin(dt * 3f * PI.toFloat() + i * 2.1f))
                val alpha = (0.12f + 0.08f * sin(dt * 4f * PI.toFloat() + i * 1.8f)) * restingDim
                val r = minDim * 0.03f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(moteGlow, alpha),
                        1f to Color.Transparent,
                        center = Offset(mx, my),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(mx, my),
                )
            }
        }
    }
}
