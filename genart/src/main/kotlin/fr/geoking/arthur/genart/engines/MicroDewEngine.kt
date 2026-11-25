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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount

private const val MICRO_DEW_LOOP_MS = 11_000f

private data class DewDrop(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Int,
)

private val DEW_POSITIONS = listOf(
    Triple(0.24f, 0.30f, 0.075f), Triple(0.56f, 0.23f, 0.105f),
    Triple(0.77f, 0.38f, 0.064f), Triple(0.40f, 0.54f, 0.052f),
    Triple(0.69f, 0.66f, 0.086f), Triple(0.18f, 0.72f, 0.048f),
    Triple(0.88f, 0.80f, 0.04f), Triple(0.51f, 0.84f, 0.057f),
    Triple(0.34f, 0.41f, 0.032f), Triple(0.84f, 0.56f, 0.037f),
    Triple(0.12f, 0.48f, 0.03f),
)

/** Macro dew on a deep green leaf with a soft, seamless shimmer. */
@Composable
internal fun MicroDewEngine(
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
            val p = DEW_POSITIONS[i]
            DewDrop(p.first, p.second, p.third, i * 37 + 9)
        }
    }
    val transition = rememberInfiniteTransition(label = "microdew")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                (MICRO_DEW_LOOP_MS / speed.coerceAtLeast(0.2f)).toInt().coerceAtLeast(8_000),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "microdew_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val minDim = minOf(w, h).coerceAtLeast(1f)
        val time = phase01(t)
        val pulseWobble = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 11) * 2f - 1f
        val pulse = pulseWobble * 0.5f + 0.5f
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFA0E8C8), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        val deep = TonalPalette.brightness(Color(0xFF0E1814), brightness * dim)
        val green = TonalPalette.brightness(Color(0xFF1A2820), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.mix(green, Color(0xFF314B39), 0.24f + pulse * 0.08f),
                0.52f to green,
                1f to deep,
            ),
        )

        val leaf = Path().apply {
            moveTo(-w * 0.12f, h * 0.78f)
            cubicTo(w * 0.08f, h * 0.42f, w * 0.56f, h * 0.10f, w * 1.10f, h * 0.22f)
            cubicTo(w * 0.88f, h * 0.57f, w * 0.39f, h * 0.91f, -w * 0.12f, h * 0.78f)
            close()
        }
        drawPath(
            path = leaf,
            brush = Brush.linearGradient(
                0f to TonalPalette.brightness(Color(0xFF355B3D), brightness * dim),
                0.55f to TonalPalette.brightness(Color(0xFF1E3B2B), brightness * dim),
                1f to deep,
                start = Offset(w * 0.12f, h * 0.28f),
                end = Offset(w * 0.88f, h * 0.80f),
            ),
        )

        val midrib = Path().apply {
            moveTo(-w * 0.04f, h * 0.80f)
            cubicTo(w * 0.28f, h * 0.62f, w * 0.60f, h * 0.38f, w * 1.02f, h * 0.24f)
        }
        drawPath(
            path = midrib,
            color = TonalPalette.withAlpha(TonalPalette.brightness(Color(0xFF8FC29A), brightness * dim), 0.34f * dim),
            style = Stroke(width = (minDim * 0.008f).coerceAtLeast(0.5f), cap = StrokeCap.Round),
        )
        for (i in 0 until 7) {
            val v = (i + 1) / 8f
            val x = w * (0.04f + v * 0.88f)
            val y = h * (0.77f - v * 0.48f)
            val side = if (i % 2 == 0) -1f else 1f
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFF80AC88), 0.18f * dim),
                start = Offset(x, y),
                end = Offset(x + side * w * 0.15f, y - h * 0.12f),
                strokeWidth = (minDim * 0.003f).coerceAtLeast(0.4f),
                cap = StrokeCap.Round,
            )
        }

        val bounceRadius = (minDim * 0.72f).coerceAtLeast(1f)
        val bounceCenter = Offset(
            w * (0.55f + 0.04f * pulseWobble),
            h * (0.44f + 0.02f * pulseWobble),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, (0.12f + 0.08f * pulse) * dim),
                1f to Color.Transparent,
                center = bounceCenter,
                radius = bounceRadius,
            ),
            radius = bounceRadius,
            center = bounceCenter,
        )

        drops.forEach { drop ->
            val shimmerWobble = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = drop.phase) * 2f - 1f
            val cx = w * drop.x
            val cy = h * drop.y
            val r = (minDim * drop.radius * (1f + shimmerWobble * 0.06f)).coerceAtLeast(1f)
            val lightAlpha = (0.55f + shimmerWobble * 0.22f).coerceIn(0.2f, 0.95f) * dim

            drawCircle(
                color = Color(0xFF06110C).copy(alpha = 0.26f * dim),
                radius = (r * 0.96f).coerceAtLeast(1f),
                center = Offset(cx + r * 0.12f, cy + r * 0.22f),
            )
            val beadShaderRadius = (r * 1.42f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFE9FFF5).copy(alpha = 0.18f * dim),
                    0.48f to accent.copy(alpha = 0.16f * dim),
                    0.82f to Color(0xFF8BCFB0).copy(alpha = 0.32f * dim),
                    1f to Color(0xFFD9FFF0).copy(alpha = 0.68f * dim),
                    center = Offset(cx - r * 0.25f, cy - r * 0.28f),
                    radius = beadShaderRadius,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color(0xFFE8FFF5).copy(alpha = lightAlpha),
                radius = (r * 0.13f).coerceAtLeast(1f),
                center = Offset(cx - r * 0.34f, cy - r * 0.39f),
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.32f + shimmerWobble * 0.28f).coerceIn(0.08f, 0.85f) * dim),
                radius = (r * 0.045f).coerceAtLeast(1f),
                center = Offset(cx - r * 0.08f, cy - r * 0.54f),
            )
        }
    }
}