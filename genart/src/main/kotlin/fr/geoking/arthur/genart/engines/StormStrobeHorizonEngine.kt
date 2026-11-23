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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.exp

private data class HorizonCloud(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val shade: Float,
    val phase: Float,
)

/** Distant storm horizon with soft, occasional lightning illumination. */
@Composable
internal fun StormStrobeHorizonEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 5)
    val clouds = remember(count) {
        List(count) { i ->
            HorizonCloud(
                x = seededRange(i * 19 + 5, -0.08f, 0.68f),
                y = seededRange(i * 31 + 9, 0.40f, 0.62f),
                width = seededRange(i * 43 + 13, 0.42f, 0.82f),
                height = seededRange(i * 53 + 17, 0.10f, 0.22f),
                shade = seededRange(i * 61 + 23, 0.68f, 1.08f),
                phase = seededRange(i * 71 + 29, 0f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "stormstrobehorizon")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobehorizon_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 7)
        val cloudPulse = loopedFbm(time, radius = 1.45f, octaves = 2, seedOffset = 19)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim),
                0.52f to TonalPalette.brightness(Color(0xFF151D2A), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF1A2230), brightness * dim * (0.88f + 0.12f * cloudPulse)),
            ),
        )

        // The flash is a brief atmospheric pulse under the cloud shelf, not a full-screen strobe.
        val flashPhase = (time + 0.41f) % 1f
        val flash = exp(-((flashPhase - 0.31f) * (flashPhase - 0.31f)) / 0.0018f)
        val secondary = exp(-((flashPhase - 0.76f) * (flashPhase - 0.76f)) / 0.0013f)
        val flashAmt = (flash + 0.38f * secondary) * dim
        val glowCenter = Offset(w * (0.57f + (drift - 0.5f) * 0.10f), h * 0.67f)
        val glowRadius = (minDim * 0.64f).coerceAtLeast(1f)
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFC9D9F2), (0.15f + 0.32f * flashAmt) * brightness),
                0.32f to TonalPalette.withAlpha(Color(0xFF879DBC), (0.10f + 0.17f * flashAmt) * brightness),
                1f to Color.Transparent,
                center = glowCenter,
                radius = glowRadius,
            ),
            topLeft = Offset(glowCenter.x - glowRadius, glowCenter.y - glowRadius * 0.55f),
            size = Size(glowRadius * 2f, glowRadius * 1.1f),
        )

        // Far cloud shelf, with a gently drifting uneven silhouette.
        val farPath = Path().apply {
            moveTo(0f, h * 0.59f)
            cubicTo(w * 0.18f, h * 0.54f, w * 0.27f, h * 0.61f, w * 0.43f, h * 0.57f)
            cubicTo(w * 0.63f, h * 0.52f, w * 0.78f, h * 0.60f, w, h * (0.55f + 0.025f * drift))
            lineTo(w, h * 0.84f)
            lineTo(0f, h * 0.84f)
            close()
        }
        drawPath(
            path = farPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFF384354), (0.30f + 0.10f * flashAmt) * dim),
                    TonalPalette.withAlpha(Color(0xFF18202D), 0.80f * dim),
                ),
                startY = h * 0.53f,
                endY = h * 0.84f,
            ),
        )

        clouds.forEach { cloud ->
            val sway = (loopedFbm(time, radius = 1.0f + cloud.phase, octaves = 2, seedOffset = (cloud.phase * 1000).toInt() + 37) - 0.5f)
            val cx = ((cloud.x + 1f + sway * 0.045f) % 1.25f - 0.12f) * w
            val cy = cloud.y * h + (drift - 0.5f) * h * 0.012f
            val cw = cloud.width * w
            val ch = cloud.height * h
            val path = Path().apply {
                moveTo(cx - cw * 0.55f, cy + ch * 0.16f)
                cubicTo(cx - cw * 0.48f, cy - ch * 0.08f, cx - cw * 0.32f, cy - ch * 0.10f, cx - cw * 0.23f, cy - ch * 0.02f)
                cubicTo(cx - cw * 0.12f, cy - ch * 0.28f, cx + cw * 0.06f, cy - ch * 0.24f, cx + cw * 0.15f, cy - ch * 0.04f)
                cubicTo(cx + cw * 0.28f, cy - ch * 0.16f, cx + cw * 0.43f, cy - ch * 0.06f, cx + cw * 0.50f, cy + ch * 0.16f)
                lineTo(cx + cw * 0.55f, cy + ch * 0.44f)
                lineTo(cx - cw * 0.55f, cy + ch * 0.44f)
                close()
            }
            val cloudColor = TonalPalette.brightness(Color(0xFF273140), brightness * dim * cloud.shade)
            drawPath(path = path, color = TonalPalette.withAlpha(cloudColor, 0.84f * dim))
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(Color(0xFFB8C8E0), (0.06f + 0.20f * flashAmt) * cloud.shade),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.006f),
            )
        }

        // Low distant land mass anchors the glowing horizon.
        val land = Path().apply {
            moveTo(0f, h * 0.76f)
            cubicTo(w * 0.18f, h * 0.72f, w * 0.32f, h * 0.78f, w * 0.47f, h * 0.75f)
            cubicTo(w * 0.66f, h * 0.71f, w * 0.80f, h * 0.79f, w, h * 0.74f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = land, color = TonalPalette.withAlpha(Color(0xFF0A0E18), 0.85f * dim))
    }
}
