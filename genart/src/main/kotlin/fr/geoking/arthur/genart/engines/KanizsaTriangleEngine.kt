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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Kanizsa triangle — three pac-man inducers + soft illusory white triangle;
 * inducers gently rotate / contours breathe.
 */
@Composable
internal fun KanizsaTriangleEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val strokeMul = when (quality) {
        GenartQuality.Low -> 0.85f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.2f
    }
    val transition = rememberInfiniteTransition(label = "kanizsa_triangle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kanizsa_triangle_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val bgMid = TonalPalette.brightness(Color(0xFF9AA0A8), brightness * 0.55f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.mix(bgMid, Color(0xFFB8BDC6), 0.25f),
                    bgMid,
                    TonalPalette.mix(bgMid, Color(0xFF6B7280), 0.35f),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.85f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val breath = 0.96f + 0.04f * sin01(time)
        val dim = if (isActive) 1f else 0.6f
        val cx = w * 0.5f
        val cy = h * 0.54f
        val r = minDim * 0.125f * breath
        val spread = minDim * 0.235f
        val discFill = TonalPalette.brightness(Color(0xFF111318), brightness * 0.85f)
        // Mouth opening ~60° (classic pac-man inducer), soft wobble
        val openFrac = 0.18f + 0.025f * sin01(time + 0.4f)
        val rotWobble = 0.08f * sin(time * 0.6f)
        val centers = listOf(
            Offset(cx, cy - spread),
            Offset(cx - spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
            Offset(cx + spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
        )
        // Aim mouths toward triangle center
        val aimAngles = listOf(
            PI.toFloat() / 2f + rotWobble,
            -PI.toFloat() / 6f + rotWobble,
            -5f * PI.toFloat() / 6f + rotWobble,
        )
        centers.forEachIndexed { i, center ->
            val openRad = openFrac * 2f * PI.toFloat()
            val aim = aimAngles[i]
            val startA = aim - openRad * 0.5f
            val sweep = 2f * PI.toFloat() - openRad
            val discPath = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r),
                    startAngleDegrees = Math.toDegrees(startA.toDouble()).toFloat(),
                    sweepAngleDegrees = Math.toDegrees(sweep.toDouble()).toFloat(),
                    forceMoveTo = false,
                )
                close()
            }
            drawPath(discPath, color = TonalPalette.withAlpha(discFill, 0.96f * dim))
        }
        // Illusory upright triangle (soft fill + contour)
        val tri = listOf(
            Offset(centers[0].x, centers[0].y + r * 0.08f),
            Offset(centers[1].x + r * 0.18f, centers[1].y - r * 0.28f),
            Offset(centers[2].x - r * 0.18f, centers[2].y - r * 0.28f),
        )
        val triPath = Path().apply {
            moveTo(tri[0].x, tri[0].y)
            lineTo(tri[1].x, tri[1].y)
            lineTo(tri[2].x, tri[2].y)
            close()
        }
        val illusory = TonalPalette.brightness(Color(0xFFF5F6F8), brightness * 0.95f)
        val glow = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
        val pulseA = 0.1f + 0.1f * sin01(time + 1.2f)
        drawPath(path = triPath, color = TonalPalette.withAlpha(illusory, (0.14f + pulseA) * dim))
        drawPath(
            path = triPath,
            color = TonalPalette.withAlpha(TonalPalette.mix(glow, Color.White, 0.55f), (0.16f + pulseA) * dim),
            style = Stroke(
                width = r * 0.07f * strokeMul,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        // Incomplete inverted outline inducers (classic Kanizsa companion)
        val invSpread = spread * 0.72f
        val inv = listOf(
            Offset(cx, cy + invSpread * 0.72f),
            Offset(cx - invSpread * cos(PI.toFloat() / 6f), cy - invSpread * 0.38f),
            Offset(cx + invSpread * cos(PI.toFloat() / 6f), cy - invSpread * 0.38f),
        )
        val outlineColor = TonalPalette.withAlpha(discFill, 0.88f * dim)
        val strokeW = r * 0.09f * strokeMul
        for (i in 0..2) {
            val a = inv[i]
            val b = inv[(i + 1) % 3]
            val dx = b.x - a.x
            val dy = b.y - a.y
            // Leave a gap in the middle so the illusory edge completes it
            drawLine(
                color = outlineColor,
                start = a,
                end = Offset(a.x + dx * 0.28f, a.y + dy * 0.28f),
                strokeWidth = strokeW,
                cap = StrokeCap.Butt,
            )
            drawLine(
                color = outlineColor,
                start = Offset(a.x + dx * 0.72f, a.y + dy * 0.72f),
                end = b,
                strokeWidth = strokeW,
                cap = StrokeCap.Butt,
            )
        }
    }
}
