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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min

/**
 * Classic Penrose tribar — three square beams with impossible overlap; face shading
 * and a slow light crawl over a dark radial field.
 */
@Composable
internal fun PenroseTriangleEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "penrose_triangle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "penrose_triangle_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF1A1530), brightness * 0.55f),
                    Color(0xFF0A0814),
                    Color(0xFF030208),
                ),
                center = Offset(w * 0.5f, h * 0.46f),
                radius = maxOf(w, h) * 0.95f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val breath = 1f + 0.018f * (sin01(time) * 2f - 1f)
        val dim = if (isActive) 1f else 0.58f
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.78f * breath / PENROSE_VIEW
        val accentA = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
        val accentB = TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness)
        val accentC = TonalPalette.brightness(TonalPalette.pick(paletteColors, 4), brightness)
        val bases = listOf(accentA, accentB, accentC)
        PENROSE_BEAMS.forEachIndexed { i, beam ->
            val lightPos = phase01((sin01(time + i * 0.33f) + 1f) * 0.5f)
            val crawl = 0.12f + 0.28f * lightPos
            val base = bases[i % bases.size]
            val light = TonalPalette.mix(base, Color.White, 0.22f + crawl * 0.35f)
            val mid = TonalPalette.mix(base, Color.White, crawl * 0.12f)
            val dark = TonalPalette.mix(base, Color.Black, 0.18f + (1f - crawl) * 0.22f)
            val path = Path().apply {
                beam.forEachIndexed { j, pt ->
                    val x = cx + (pt.first - PENROSE_VIEW * 0.5f) * scale
                    val y = cy + (pt.second - PENROSE_VIEW * 0.5f) * scale
                    if (j == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            val shade = when (i % 3) {
                0 -> light
                1 -> mid
                else -> dark
            }
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(shade, 0.94f * dim),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(TonalPalette.mix(shade, Color.White, 0.08f), 0.35f * dim),
            )
        }
    }
}

private const val PENROSE_VIEW = 108f

private val PENROSE_BEAMS: List<List<Pair<Float, Float>>> = listOf(
    listOf(
        19.83f to 78.77f, 30.11f to 78.82f, 58.42f to 29.57f,
        98.96f to 97.12f, 104f to 87.93f, 58.56f to 10.87f,
    ),
    listOf(
        4f to 88.26f, 8.77f to 97.24f, 99f to 97.18f, 58.43f to 29.57f,
        53.34f to 38.43f, 82.71f to 88.2f,
    ),
    listOf(
        47.52f to 10.76f, 4.01f to 88.24f, 82.73f to 88.22f, 77.15f to 78.8f,
        19.78f to 78.81f, 58.61f to 10.87f,
    ),
)