package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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

/** Dog silhouette in a slow play-bow / stretch pose on a warm sunlit floor. */
@Composable
internal fun DogStretchEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dog_stretch_loop")
    val stretchAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_stretch_t",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((50000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_stretch_drift",
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
        val cx = w * 0.5f
        val cy = h * 0.58f
        val dt = phase01(driftAnim)
        val haze = loopedFbm(dt, radius = 1.2f, octaves = 2, seedOffset = 42)

        // Warm indoor ambient background
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2C261C),
                0.5f to Color(0xFF201A14),
                1f to Color(0xFF100E0A),
                center = Offset(cx, cy),
                radius = w.coerceAtLeast(h) * 0.8f,
            ),
        )

        // Warm sunbeam diagonal from upper-left
        val sunColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8C898), TonalPalette.pick(paletteColors, 1), 0.2f),
            brightness * restingDim,
        )
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(sunColor, 0.2f + 0.06f * haze),
                0.5f to TonalPalette.withAlpha(sunColor, 0.08f),
                1f to Color.Transparent,
                center = Offset(w * 0.25f, h * 0.15f),
                radius = w * 0.6f,
            ),
        )

        // Warm floor pool where the dog stretches
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(
                    TonalPalette.mix(Color(0xFF3A2E20), TonalPalette.pick(paletteColors, 2), 0.12f),
                    0.35f * restingDim,
                ),
                1f to Color.Transparent,
                center = Offset(cx, h * 0.74f),
                radius = minDim * 0.48f,
            ),
            topLeft = Offset(cx - minDim * 0.48f, h * 0.64f),
            size = Size(minDim * 0.96f, minDim * 0.2f),
        )

        val stretch = sin(phase01(stretchAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val amount = (if (isActive) 1f else 0.4f) * stretch
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A1C12), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * restingDim,
        )

        // Elongating body in play-bow
        val bodyRx = minDim * (0.26f + 0.04f * amount)
        val bodyRy = minDim * (0.10f - 0.02f * amount)
        val bodyY = cy + minDim * 0.02f * amount
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, bodyY - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )

        // Head
        val headR = minDim * 0.09f
        val head = Offset(cx - bodyRx * 0.95f, bodyY - minDim * 0.04f - minDim * 0.03f * amount)
        drawCircle(color = bodyColor, radius = headR, center = head)
        // Snout
        drawOval(
            color = bodyColor,
            topLeft = Offset(head.x - headR * 1.2f, head.y - headR * 0.25f),
            size = Size(headR * 0.7f, headR * 0.5f),
        )
        // Ear
        val ear = Path().apply {
            moveTo(head.x + headR * 0.1f, head.y - headR * 0.4f)
            lineTo(head.x + headR * 0.55f, head.y - headR * 1.1f)
            lineTo(head.x + headR * 0.55f, head.y - headR * 0.1f)
            close()
        }
        drawPath(ear, color = bodyColor)

        // Eye (Medium/High)
        if (detail > 0) {
            val eyeColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFF5A3A1E), TonalPalette.pick(paletteColors, 1), 0.3f),
                brightness * restingDim,
            )
            drawCircle(
                color = TonalPalette.withAlpha(eyeColor, 0.65f * restingDim),
                radius = headR * 0.09f,
                center = Offset(head.x - headR * 0.35f, head.y - headR * 0.1f),
            )
        }

        // Nose
        drawCircle(
            color = TonalPalette.brightness(Color(0xFF0E0A08), brightness * restingDim),
            radius = headR * 0.07f,
            center = Offset(head.x - headR * 1.0f, head.y + headR * 0.02f),
        )

        // Front legs lowered, rear up
        val legStroke = minDim * 0.028f
        val front = Path().apply {
            moveTo(cx - bodyRx * 0.55f, bodyY)
            lineTo(cx - bodyRx * 0.7f, bodyY + minDim * (0.16f + 0.04f * amount))
            moveTo(cx - bodyRx * 0.25f, bodyY)
            lineTo(cx - bodyRx * 0.35f, bodyY + minDim * (0.15f + 0.035f * amount))
        }
        drawPath(front, color = bodyColor, style = Stroke(width = legStroke, cap = StrokeCap.Round))
        val rear = Path().apply {
            moveTo(cx + bodyRx * 0.45f, bodyY - bodyRy * 0.3f)
            lineTo(cx + bodyRx * 0.55f, bodyY + minDim * 0.08f)
            moveTo(cx + bodyRx * 0.75f, bodyY - bodyRy * 0.2f)
            lineTo(cx + bodyRx * 0.85f, bodyY + minDim * 0.07f)
        }
        drawPath(rear, color = bodyColor, style = Stroke(width = legStroke, cap = StrokeCap.Round))

        // Wagging tail
        if (quality != GenartQuality.Low) {
            val wagPhase = sin(phase01(stretchAnim) * 6f * PI.toFloat()) * (if (isActive) 1f else 0.4f)
            val tail = Path().apply {
                val base = Offset(cx + bodyRx * 0.95f, bodyY - bodyRy)
                moveTo(base.x, base.y)
                quadraticTo(
                    base.x + minDim * (0.05f + 0.03f * wagPhase),
                    base.y - minDim * 0.08f,
                    base.x + minDim * (0.02f + 0.04f * wagPhase),
                    base.y - minDim * 0.14f,
                )
            }
            drawPath(tail, color = bodyColor, style = Stroke(width = legStroke * 0.9f, cap = StrokeCap.Round))
        }

        // Floating dust motes in the sunbeam
        if (detail > 0) {
            val moteCount = if (detail == 2) 5 else 3
            for (i in 0 until moteCount) {
                val mx = w * (0.15f + 0.1f * i + 0.025f * sin(dt * 2f * PI.toFloat() + i * 1.3f))
                val my = h * (0.15f + 0.08f * i + 0.03f * sin(dt * 3f * PI.toFloat() + i * 2.2f))
                val alpha = (0.1f + 0.08f * sin(dt * 4f * PI.toFloat() + i * 1.9f)) * restingDim
                val r = minDim * 0.028f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(sunColor, alpha),
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
