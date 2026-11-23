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
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class SpringPetal(
    val x: Float,
    val y: Float,
    val size: Float,
    val drift: Float,
    val phase: Float,
    val color: Int,
)

/** Soft spring greens, sunlit blossom haze and gently drifting petals. */
@Composable
internal fun SeasonSpringWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val petals = remember(count) {
        List(count) { i ->
            SpringPetal(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                size = seededRange(i * 41 + 11, 0.012f, 0.028f),
                drift = seededRange(i * 53 + 13, 0.035f, 0.085f),
                phase = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                color = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "seasonspringwash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seasonspringwash_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 29)
        val springLight = TonalPalette.brightness(Color(0xFFF2F7C8), brightness * dim)
        val mint = TonalPalette.brightness(Color(0xFFB8E0C8), brightness * dim)
        val leaf = TonalPalette.brightness(Color(0xFF6FAE8A), brightness * dim)
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.mix(mint, Color(0xFFE8F3D8), 0.24f + 0.10f * wash),
                0.52f to mint,
                1f to TonalPalette.mix(leaf, Color(0xFF4D9275), 0.22f),
            ),
        )

        // Broad, soft canopy-like washes keep the scene atmospheric rather than geometric.
        listOf(
            Triple(0.24f + 0.08f * drift, 0.25f, 0.48f),
            Triple(0.78f - 0.07f * drift, 0.64f, 0.55f),
        ).forEachIndexed { i, (x, y, radius) ->
            val center = Offset(w * x, h * y)
            val r = minDim * radius
            val glow = if (i == 0) springLight else TonalPalette.brightness(Color(0xFFD6F0D8), brightness * dim)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(glow, (0.30f + 0.06f * wash) * dim),
                    0.58f to TonalPalette.withAlpha(glow, 0.10f * dim),
                    1f to Color.Transparent,
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }

        // Fine vertical stems and soft leaf shapes emerge from the green wash.
        for (i in 0 until 5) {
            val x = w * (0.08f + i * 0.22f)
            val sway = sin(time * 2f * PI.toFloat() + i * 0.9f) * minDim * 0.025f
            val baseY = h * (0.92f - (i % 2) * 0.08f)
            val tipY = h * (0.52f + (i % 3) * 0.06f)
            val stem = TonalPalette.withAlpha(leaf, 0.38f * dim)
            drawLine(stem, Offset(x, baseY), Offset(x + sway, tipY), minDim * 0.005f)
            for (side in listOf(-1f, 1f)) {
                val ly = baseY - (baseY - tipY) * (0.34f + (i % 2) * 0.18f)
                val leafCenter = Offset(x + sway * 0.55f + side * minDim * 0.065f, ly)
                rotate(degrees = side * -22f + 8f * sin(time * 2f * PI.toFloat() + i), pivot = leafCenter) {
                    drawOval(
                        brush = Brush.radialGradient(
                            listOf(TonalPalette.withAlpha(Color(0xFFC6E8B6), 0.7f * dim), TonalPalette.withAlpha(leaf, 0.25f * dim)),
                            center = leafCenter,
                            radius = minDim * 0.07f,
                        ),
                        topLeft = Offset(leafCenter.x - minDim * 0.07f, leafCenter.y - minDim * 0.023f),
                        size = Size(minDim * 0.14f, minDim * 0.046f),
                    )
                }
            }
        }

        petals.forEach { petal ->
            val angle = time * 2f * PI.toFloat() + petal.phase
            val n = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = petal.color * 97 + 5)
            val x = (petal.x + petal.drift * sin(angle) + 1f) % 1f * w
            val y = ((petal.y + 0.10f * time + 0.035f * sin(angle * 0.7f) + 1f) % 1f) * h
            val r = minDim * petal.size * (0.85f + 0.25f * n)
            val petalColor = TonalPalette.mix(Color(0xFFFFF4F1), TonalPalette.pick(paletteColors, petal.color), 0.25f)
            val center = Offset(x, y)
            rotate(degrees = 360f * time + petal.phase * 57.3f, pivot = center) {
                drawOval(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(TonalPalette.brightness(petalColor, brightness * dim), 0.72f * dim),
                        1f to TonalPalette.withAlpha(Color(0xFFE9AFC0), 0.12f * dim),
                        center = center,
                        radius = r,
                    ),
                    topLeft = Offset(x - r * 0.55f, y - r),
                    size = Size(r * 1.1f, r * 2f),
                )
            }
        }
    }
}
