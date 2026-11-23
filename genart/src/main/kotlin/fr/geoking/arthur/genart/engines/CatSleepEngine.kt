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
import androidx.compose.runtime.remember
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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class CatSleepMote(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
)

/** Curled sleeping cat silhouette with pointed ears, breathing slowly on a warm ambient rug. */
@Composable
internal fun CatSleepEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val moteCount = qualityCount(quality, low = 2, medium = 4, high = 7)
    val motes = remember(moteCount) {
        List(moteCount) { i ->
            CatSleepMote(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                driftX = seededRange(i * 43 + 13, 0.015f, 0.04f),
                driftY = seededRange(i * 47 + 17, 0.01f, 0.03f),
                phaseOffset = seededRange(i * 53 + 21, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "cat_sleep_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5600 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_sleep_breath",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_sleep_drift",
    )
    val restingDim = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.58f

        // Warm dark ambient background
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2C221C),
                0.55f to Color(0xFF1A1410),
                1f to Color(0xFF0C0908),
                center = Offset(cx, cy),
                radius = w.coerceAtLeast(h) * 0.85f,
            ),
        )

        // Cozy rug / soft pool under sleeping cat
        val rugRx = minDim * 0.44f
        val rugRy = minDim * 0.22f
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 2), 0.28f * restingDim),
                0.65f to TonalPalette.withAlpha(Color(0xFF38291F), 0.35f * restingDim),
                1f to Color.Transparent,
                center = Offset(cx, cy + minDim * 0.04f),
                radius = rugRx,
            ),
            topLeft = Offset(cx - rugRx, cy + minDim * 0.04f - rugRy),
            size = Size(rugRx * 2f, rugRy * 2f),
        )

        val breathT = phase01(breathAnim)
        val breath = sin(breathT * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.03f else 0.01f) * breath
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF241810), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * restingDim,
        )

        // Curled cat body
        val bodyRx = minDim * 0.28f * scale
        val bodyRy = minDim * 0.18f * scale
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )

        // Head nestled into the curled flank
        val headR = minDim * 0.13f * scale
        val head = Offset(cx - bodyRx * 0.55f, cy - bodyRy * 0.45f)
        drawCircle(color = bodyColor, radius = headR, center = head)

        // Tucked paws curve
        val tuckR = minDim * 0.10f * scale
        drawCircle(color = bodyColor, radius = tuckR, center = Offset(cx + bodyRx * 0.4f, cy - bodyRy * 0.2f))

        // Cute ears
        val ears = Path().apply {
            moveTo(head.x - headR * 0.7f, head.y - headR * 0.15f)
            lineTo(head.x - headR * 0.95f, head.y - headR * 1.4f)
            lineTo(head.x - headR * 0.1f, head.y - headR * 0.55f)
            close()
            moveTo(head.x + headR * 0.05f, head.y - headR * 0.5f)
            lineTo(head.x + headR * 0.85f, head.y - headR * 1.35f)
            lineTo(head.x + headR * 0.65f, head.y - headR * 0.1f)
            close()
        }
        drawPath(ears, color = bodyColor)

        // Tail curled around the sleeping body
        val tail = Path().apply {
            val base = Offset(cx + bodyRx * 0.78f, cy - bodyRy * 0.1f)
            val tip = Offset(cx - bodyRx * 0.1f, cy + bodyRy * 0.82f)
            moveTo(base.x, base.y)
            cubicTo(
                base.x + minDim * 0.14f,
                base.y + bodyRy * 1.1f,
                cx + bodyRx * 0.35f,
                cy + bodyRy * 1.05f,
                tip.x,
                tip.y,
            )
        }
        drawPath(
            path = tail,
            color = bodyColor,
            style = Stroke(width = minDim * 0.038f, cap = StrokeCap.Round),
        )

        // Sleeping eye curve on the nestled face
        val eyePath = Path().apply {
            val ex = head.x - headR * 0.2f
            val ey = head.y + headR * 0.15f
            moveTo(ex - headR * 0.25f, ey - headR * 0.05f)
            quadraticTo(ex, ey + headR * 0.12f, ex + headR * 0.25f, ey - headR * 0.05f)
        }
        drawPath(
            path = eyePath,
            color = TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 1), 0.35f * restingDim),
            style = Stroke(width = minDim * 0.008f, cap = StrokeCap.Round),
        )

        // Floating ambient motes
        val time = phase01(driftAnim)
        motes.forEach { mote ->
            val mx = phase01(mote.x0 + time * mote.driftX) * w
            val my = phase01(mote.y0 + time * mote.driftY) * h
            val glow = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
            val alpha = (0.15f + 0.10f * (0.5f + 0.5f * sin(time * 2f * PI.toFloat() + mote.phaseOffset))) * restingDim
            val r = minDim * 0.045f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(glow, alpha),
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
