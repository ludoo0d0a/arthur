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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount

/** An empty perspective road receding to a vanishing point, with sliding dashed lane markers. */
@Composable
internal fun RoadsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val markerCount = qualityCount(quality, low = 8, medium = 12, high = 18)
    val transition = rememberInfiniteTransition(label = "roads")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((2400 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "roads_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vanishX = w * 0.5f
        val vanishY = h * 0.34f

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.4f * dim)
        val skyBottom = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.55f * dim)
        drawRect(
            brush = Brush.verticalGradient(colors = listOf(skyTop, skyBottom)),
            topLeft = Offset(0f, 0f),
            size = Size(w, vanishY),
        )

        val landColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness * 0.35f * dim)
        drawRect(
            color = landColor,
            topLeft = Offset(0f, vanishY),
            size = Size(w, h - vanishY),
        )

        val roadFar = TonalPalette.brightness(Color(0xFF2E3138), brightness * dim)
        val roadNear = TonalPalette.brightness(Color(0xFF16181C), brightness * dim)
        val roadPath = Path().apply {
            moveTo(vanishX - w * 0.02f, vanishY)
            lineTo(vanishX + w * 0.02f, vanishY)
            lineTo(w * 0.94f, h)
            lineTo(w * 0.06f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(roadFar, roadNear),
                startY = vanishY,
                endY = h,
            ),
        )

        val shoulderColor = TonalPalette.withAlpha(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 3), brightness * dim),
            0.55f,
        )
        drawPath(
            path = Path().apply {
                moveTo(vanishX - w * 0.02f, vanishY)
                lineTo(w * 0.06f, h)
                lineTo(w * 0.02f, h)
                close()
            },
            color = shoulderColor,
        )
        drawPath(
            path = Path().apply {
                moveTo(vanishX + w * 0.02f, vanishY)
                lineTo(w * 0.98f, h)
                lineTo(w * 0.94f, h)
                close()
            },
            color = shoulderColor,
        )

        val scroll = phase01(t)
        val markerColor = TonalPalette.brightness(Color(0xFFE8E4D8), brightness * dim)
        val k = 0.55f
        val step = 1f / markerCount
        for (i in 0 until markerCount) {
            val depth = phase01((i * step) + scroll * step)
            val nearness = 1f / (1f + depth * markerCount * k)
            val y = vanishY + (h - vanishY) * (1f - nearness)
            val roadHalfWidthAtY = (w * 0.02f) + (w * 0.46f) * (1f - nearness)
            val markerWidth = roadHalfWidthAtY * 0.12f
            val markerHeight = markerWidth * 3.2f
            val alpha = (0.85f * (0.25f + 0.75f * (1f - nearness))).coerceIn(0.1f, 0.85f)
            drawRect(
                color = TonalPalette.withAlpha(markerColor, alpha * dim),
                topLeft = Offset(vanishX - markerWidth / 2f, y - markerHeight / 2f),
                size = Size(markerWidth, markerHeight),
            )
        }
    }
}
