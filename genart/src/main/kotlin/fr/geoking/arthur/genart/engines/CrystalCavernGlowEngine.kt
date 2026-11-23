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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange

private data class Crystal(
    val x: Float,
    val height: Float,
    val width: Float,
    val lean: Float,
    val phase: Float,
)

/** A quiet blue cavern with softly glowing crystal formations. */
@Composable
internal fun CrystalCavernGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val crystals = remember(count) {
        List(count + 3) { i ->
            Crystal(
                x = (i + 0.5f) / (count + 3f),
                height = seededRange(i * 17 + 3, 0.16f, 0.42f),
                width = seededRange(i * 29 + 7, 0.055f, 0.12f),
                lean = seededRange(i * 41 + 11, -0.07f, 0.07f),
                phase = seededRange(i * 53 + 13, 0f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "crystalcavernglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crystalcavernglow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.2f, octaves = 3, seedOffset = 13)
        val pulse = 0.78f + 0.22f * loopedFbm(time, radius = 1.5f, octaves = 2, seedOffset = 31)
        val deep = TonalPalette.brightness(Color(0xFF0C1220), brightness * dim)
        val stone = TonalPalette.brightness(Color(0xFF1A2840), brightness * dim)
        val glow = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFA0E0FF), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * dim,
        )
        drawRect(brush = Brush.verticalGradient(listOf(deep, stone, Color(0xFF101A2C))))

        // Cave depth and pooled cyan light.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(glow, 0.24f * pulse * dim),
                0.5f to TonalPalette.withAlpha(Color(0xFF355E82), 0.12f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.49f + (drift - 0.5f) * 0.12f), h * 0.72f),
                radius = minDim * 0.66f,
            ),
            radius = minDim * 0.66f,
            center = Offset(w * (0.49f + (drift - 0.5f) * 0.12f), h * 0.72f),
        )

        // Dark overhangs frame the cavern opening.
        val ceiling = Path().apply {
            moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h * 0.18f)
            lineTo(w * 0.78f, h * 0.24f); lineTo(w * 0.62f, h * 0.15f)
            lineTo(w * 0.48f, h * 0.22f); lineTo(w * 0.30f, h * 0.14f)
            lineTo(w * 0.14f, h * 0.23f); lineTo(0f, h * 0.18f); close()
        }
        drawPath(ceiling, Color(0xFF080D18))
        val floor = Path().apply {
            moveTo(0f, h * 0.86f); lineTo(w * 0.18f, h * 0.81f)
            lineTo(w * 0.35f, h * 0.88f); lineTo(w * 0.55f, h * 0.83f)
            lineTo(w * 0.77f, h * 0.89f); lineTo(w, h * 0.82f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(floor, Color(0xFF0A1120))

        // Large foreground crystals and smaller distant shards.
        crystals.forEachIndexed { index, crystal ->
            val baseY = h * (0.84f + 0.045f * loopedFbm(time, radius = 1.1f, seedOffset = index + 71))
            val sway = (loopedFbm(time, radius = 1.35f, seedOffset = index * 37 + 19) - 0.5f) * minDim * 0.012f
            val x = w * crystal.x + sway
            val height = h * crystal.height
            val half = minDim * crystal.width * 0.5f
            val tipX = x + crystal.lean * minDim
            val tipY = baseY - height
            val shimmer = 0.78f + 0.22f * loopedFbm(time, radius = 1.5f, seedOffset = index * 43 + 5)
            drawCrystal(x, baseY, tipX, tipY, half, glow, shimmer * pulse * dim, index)
        }

        // Thin mineral seams catch the light along the cave walls.
        for (i in 0 until 4) {
            val side = if (i % 2 == 0) 1f else -1f
            val x0 = if (side > 0f) 0f else w
            val x1 = if (side > 0f) w * (0.12f + i * 0.045f) else w * (0.88f - i * 0.045f)
            drawLine(
                color = TonalPalette.withAlpha(glow, (0.10f + 0.05f * pulse) * dim),
                start = Offset(x0, h * (0.42f + i * 0.08f)),
                end = Offset(x1, h * (0.36f + i * 0.11f)),
                strokeWidth = minDim * 0.003f,
            )
        }
    }
}

private fun DrawScope.drawCrystal(
    x: Float,
    baseY: Float,
    tipX: Float,
    tipY: Float,
    halfWidth: Float,
    glow: Color,
    intensity: Float,
    index: Int,
) {
    val shoulder = tipY + (baseY - tipY) * 0.34f
    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(x + halfWidth, shoulder)
        lineTo(x + halfWidth * 0.72f, baseY)
        lineTo(x - halfWidth * 0.72f, baseY)
        lineTo(x - halfWidth, shoulder)
        close()
    }
    val shade = if (index % 2 == 0) Color(0xFF244264) else Color(0xFF1B3450)
    drawPath(path, TonalPalette.withAlpha(shade, 0.96f))
    val facet = Path().apply {
        moveTo(tipX, tipY)
        lineTo(x + halfWidth, shoulder)
        lineTo(x, baseY)
        close()
    }
    drawPath(facet, TonalPalette.withAlpha(glow, 0.18f * intensity))
    val seam = Path().apply { moveTo(tipX, tipY); lineTo(x, baseY) }
    drawPath(seam, TonalPalette.withAlpha(glow, 0.32f * intensity), style = Stroke(width = halfWidth * 0.055f))
    drawPath(path, TonalPalette.withAlpha(glow, 0.38f * intensity), style = Stroke(width = halfWidth * 0.035f))
}
