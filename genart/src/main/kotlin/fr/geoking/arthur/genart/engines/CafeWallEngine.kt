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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Café wall illusion — staggered soft brick rows; mortar lines drift slowly (ambient-safe).
 */
@Composable
internal fun CafeWallEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rows = qualityCount(quality, low = 10, medium = 14, high = 18)
    val cols = qualityCount(quality, low = 8, medium = 11, high = 14)
    val transition = rememberInfiniteTransition(label = "cafe_wall")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((45000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cafe_wall_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF2A2D35), brightness * 0.45f),
                    Color(0xFF12141A),
                ),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val mortarPhase = sin01(time) * 0.42f
        val dim = if (isActive) 1f else 0.65f
        val mortarH = h / (rows * 2.15f)
        val brickH = mortarH * 1.55f
        val brickW = w / cols
        val lightBrick = TonalPalette.mix(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness),
            Color(0xFFE8EAED),
            0.62f,
        )
        val darkBrick = TonalPalette.mix(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 4), brightness * 0.7f),
            Color(0xFF101218),
            0.55f,
        )
        val mortar = TonalPalette.mix(Color(0xFF9CA3AF), Color(0xFF6B7280), 0.35f)
        for (row in 0 until rows) {
            val y = row * (brickH + mortarH) + mortarH * 0.5f
            val stagger = if (row % 2 == 0) 0f else brickW * (0.5f + mortarPhase * 0.08f)
            drawRect(
                color = TonalPalette.withAlpha(mortar, 0.85f * dim),
                topLeft = Offset(0f, y + brickH),
                size = Size(w, mortarH),
            )
            for (col in -1..cols) {
                val x = col * brickW + stagger - mortarPhase * brickW * 0.06f
                val brick = if ((row + col) % 2 == 0) lightBrick else darkBrick
                drawRect(
                    color = TonalPalette.withAlpha(brick, 0.92f * dim),
                    topLeft = Offset(x, y),
                    size = Size(brickW * 0.96f, brickH),
                )
            }
        }
    }
}