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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * Kanizsa triangle — three pac-man discs and a soft glowing illusory triangle outline.
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
    val transition = rememberInfiniteTransition(label = "kanizsa_triangle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((38000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kanizsa_triangle_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF1C1F26), brightness * 0.48f),
                    Color(0xFF0E1016),
                    Color(0xFF050608),
                ),
                center = Offset(w * 0.5f, h * 0.52f),
                radius = maxOf(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val breath = 0.92f + 0.08f * sin01(time)
        val dim = if (isActive) 1f else 0.6f
        val cx = w * 0.5f
        val cy = h * 0.54f
        val r = minDim * 0.11f * breath
        val spread = minDim * 0.21f
        val discFill = TonalPalette.brightness(Color(0xFFF3F4F6), brightness * 0.9f)
        val wedgeOpen = 0.78f + 0.06f * sin01(time + 0.3f)
        val centers = listOf(
            Offset(cx, cy - spread),
            Offset(cx - spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
            Offset(cx + spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
        )
        val aimAngles = listOf(PI.toFloat() / 2f, -PI.toFloat() / 6f, -5f * PI.toFloat() / 6f)
        centers.forEachIndexed { i, center ->
            val openRad = (1f - wedgeOpen) * PI.toFloat()
            val aim = aimAngles[i]
            val startA = aim - openRad * 0.5f
            val endA = aim + openRad * 0.5f
            val discPath = Path().apply {
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(
                        center.x - r,
                        center.y - r,
                        center.x + r,
                        center.y + r,
                    ),
                    startAngleDegrees = Math.toDegrees(startA.toDouble()).toFloat(),
                    sweepAngleDegrees = Math.toDegrees((2f * PI.toFloat() - openRad).toDouble()).toFloat(),
                )
                lineTo(center.x, center.y)
                close()
            }
            drawPath(discPath, color = TonalPalette.withAlpha(discFill, 0.96f * dim))
        }
        val tri = Path().apply {
            moveTo(centers[0].x, centers[0].y + r * 0.15f)
            lineTo(centers[1].x + r * 0.2f, centers[1].y - r * 0.35f)
            lineTo(centers[2].x - r * 0.2f, centers[2].y - r * 0.35f)
            close()
        }
        val glow = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness)
        val glowAlpha = 0.12f + 0.1f * sin01(time + 1.1f)
        drawPath(
            path = tri,
            color = TonalPalette.withAlpha(glow, glowAlpha * dim),
        )
        drawPath(
            path = tri,
            color = TonalPalette.withAlpha(TonalPalette.mix(glow, Color.White, 0.4f), (glowAlpha * 1.8f).coerceAtMost(0.28f) * dim),
            style = Stroke(width = r * 0.12f),
        )
    }
}