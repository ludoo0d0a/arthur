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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SoftShadowsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val blobs = remember(count) {
        List(count) { i ->
            BlobSeed(
                ox = seededUnit(i * 19 + 2),
                oy = seededUnit(i * 31 + 5),
                ampX = seededRange(i * 43 + 8, 0.08f, 0.22f),
                ampY = seededRange(i * 47 + 9, 0.06f, 0.18f),
                phase = seededUnit(i * 59 + 11) * 2f * PI.toFloat(),
                w = seededRange(i * 61 + 13, 0.16f, 0.34f),
                h = seededRange(i * 67 + 17, 0.12f, 0.28f),
                colorIndex = i,
                corner = seededRange(i * 71 + 19, 0.25f, 0.55f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "softshadows")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "softshadows_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = maxOf(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        blobs.forEach { b ->
            val x = (b.ox + b.ampX * sin(time + b.phase)) * w
            val y = (b.oy + b.ampY * cos(time * 0.85f + b.phase)) * h
            val bw = b.w * w
            val bh = b.h * h
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, b.colorIndex), brightness)
            val shadowAlpha = if (isActive) 0.28f else 0.18f
            val shadowOffset = if (isActive) 18f else 10f
            // Soft shadow disc
            drawRoundRect(
                color = TonalPalette.withAlpha(Color.Black, shadowAlpha * brightness.coerceAtMost(1f)),
                topLeft = Offset(x - bw * 0.5f + shadowOffset, y - bh * 0.5f + shadowOffset),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw * b.corner, bh * b.corner),
            )
            // Glow halo
            drawRoundRect(
                color = TonalPalette.withAlpha(color, 0.18f * brightness.coerceAtMost(1.2f)),
                topLeft = Offset(x - bw * 0.55f, y - bh * 0.55f),
                size = Size(bw * 1.1f, bh * 1.1f),
                cornerRadius = CornerRadius(bw * b.corner, bh * b.corner),
            )
            drawRoundRect(
                color = TonalPalette.withAlpha(color, (0.55f + 0.2f * sin01(time + b.phase)).coerceAtMost(0.9f)),
                topLeft = Offset(x - bw * 0.5f, y - bh * 0.5f),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw * b.corner, bh * b.corner),
            )
        }
    }
}

private data class BlobSeed(
    val ox: Float,
    val oy: Float,
    val ampX: Float,
    val ampY: Float,
    val phase: Float,
    val w: Float,
    val h: Float,
    val colorIndex: Int,
    val corner: Float,
)
