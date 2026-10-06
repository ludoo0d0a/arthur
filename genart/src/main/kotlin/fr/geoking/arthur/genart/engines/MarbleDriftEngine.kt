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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.fbm2D
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange

/**
 * Domain-warped fbm stone / onyx / jade veins as a coarse soft-circle grid —
 * very slow Ambient loop with a soft vignette. Distinct from MarbleCaustics
 * (glass orbs casting floor caustics).
 */
@Composable
internal fun MarbleDriftEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val gridSide = qualityCount(quality, low = 6, medium = 8, high = 10)
    val cells = remember(gridSide) {
        List(gridSide * gridSide) { i ->
            val col = i % gridSide
            val row = i / gridSide
            MarbleDriftCell(
                gx = (col + 0.5f) / gridSide,
                gy = (row + 0.5f) / gridSide,
                jitterX = seededRange(i * 17 + 3, -0.04f, 0.04f),
                jitterY = seededRange(i * 29 + 7, -0.04f, 0.04f),
                radiusFrac = seededRange(i * 41 + 11, 0.14f, 0.28f),
                alphaBase = seededRange(i * 53 + 13, 0.22f, 0.55f),
                seedOffset = i * 733 + 19,
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "marble_drift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "marble_drift_t",
    )

    val dim = if (isActive) 1f else 0.5f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)

        val stoneBase = TonalPalette.brightness(Color(0xFF1A1614), brightness * 0.55f)
        val onyxDeep = TonalPalette.brightness(Color(0xFF0C0A0E), brightness * 0.4f)
        drawRect(
            brush = Brush.verticalGradient(colors = listOf(stoneBase, onyxDeep)),
        )

        val warpDrift = loopedFbm(time, radius = 1.4f, seedOffset = 11) * 0.35f
        val veinDrift = loopedFbm(time, radius = 1.8f, seedOffset = 47) * 0.25f

        cells.forEach { cell ->
            val nx = cell.gx * 2.8f + warpDrift
            val ny = cell.gy * 2.8f + veinDrift
            val warpX = fbm2D(nx, ny, octaves = 3, seedOffset = cell.seedOffset) * 0.85f
            val warpY = fbm2D(nx + 5.1f, ny + 3.7f, octaves = 3, seedOffset = cell.seedOffset + 101) * 0.85f
            val vein = fbm2D(
                nx * 1.6f + warpX,
                ny * 1.6f + warpY,
                octaves = 4,
                seedOffset = cell.seedOffset + 211,
            )
            val veinSharp = (vein * vein * (3f - 2f * vein)).coerceIn(0f, 1f)

            val paletteTint = TonalPalette.pick(paletteColors, cell.colorIndex)
            val stone = TonalPalette.mix(Color(0xFF6B5E52), paletteTint, 0.2f)
            val jade = TonalPalette.mix(Color(0xFF3A6B5A), paletteTint, 0.35f)
            val onyx = TonalPalette.mix(Color(0xFF2A2430), paletteTint, 0.15f)
            val veinColor = when {
                veinSharp > 0.62f -> TonalPalette.mix(jade, Color(0xFF8ECFB0), (veinSharp - 0.62f) / 0.38f)
                veinSharp > 0.38f -> TonalPalette.mix(stone, jade, (veinSharp - 0.38f) / 0.24f)
                else -> TonalPalette.mix(onyx, stone, veinSharp / 0.38f)
            }
            val tint = TonalPalette.brightness(veinColor, brightness)

            val breathe = 0.92f + 0.08f * loopedFbm(time, radius = 1.1f, seedOffset = cell.seedOffset + 7)
            val cx = phase01(cell.gx + cell.jitterX) * w
            val cy = phase01(cell.gy + cell.jitterY) * h
            val radius = cell.radiusFrac * minDim * breathe * (0.85f + 0.3f * veinSharp)
            val alpha = cell.alphaBase * (0.45f + 0.55f * veinSharp) * dim

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, alpha),
                        TonalPalette.withAlpha(tint, alpha * 0.35f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        // Soft vignette
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    TonalPalette.withAlpha(Color(0xFF050408), 0.55f * dim),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = minDim * 0.78f,
            ),
            radius = minDim * 0.95f,
            center = Offset(w * 0.5f, h * 0.5f),
        )
    }
}

private data class MarbleDriftCell(
    val gx: Float,
    val gy: Float,
    val jitterX: Float,
    val jitterY: Float,
    val radiusFrac: Float,
    val alphaBase: Float,
    val seedOffset: Int,
    val colorIndex: Int,
)
