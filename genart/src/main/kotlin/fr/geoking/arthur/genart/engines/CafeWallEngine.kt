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
 * Café wall illusion — offset black/white brick rows with continuous thin mortar;
 * very slow horizontal drift + soft mortar brightness pulse.
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
    val cols = qualityCount(quality, low = 8, medium = 12, high = 16)
    val transition = rememberInfiniteTransition(label = "cafe_wall")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
                    TonalPalette.brightness(Color(0xFF2A2D35), brightness * 0.42f),
                    Color(0xFF12141A),
                ),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val drift = phase01(t) // seamless horizontal loop
        val mortarPulse = 0.72f + 0.28f * sin01(time)
        val dim = if (isActive) 1f else 0.65f
        val mortarH = (h / rows) * 0.14f
        val brickH = (h / rows) - mortarH
        val brickW = w / cols
        val gap = brickW * 0.045f
        val lightBrick = TonalPalette.mix(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness),
            Color(0xFFF1F3F5),
            0.72f,
        )
        val darkBrick = TonalPalette.mix(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 4), brightness * 0.65f),
            Color(0xFF0E1014),
            0.62f,
        )
        val mortarBase = TonalPalette.mix(Color(0xFFB0B6C0), Color(0xFF8B919C), 0.35f)
        val mortar = TonalPalette.mix(mortarBase, Color.White, (mortarPulse - 0.72f) * 0.55f)
        for (row in 0 until rows) {
            val y = row * (brickH + mortarH)
            val stagger = if (row % 2 == 0) 0f else brickW * 0.5f
            val driftX = drift * brickW
            // Continuous mortar strip (the wedge-looking line)
            drawRect(
                color = TonalPalette.withAlpha(mortar, 0.92f * dim * mortarPulse),
                topLeft = Offset(0f, y + brickH),
                size = Size(w, mortarH),
            )
            for (col in -1..cols) {
                val x = col * brickW + stagger + driftX
                val isLight = (col + if (row % 2 == 0) 0 else 1) % 2 == 0
                val brick = if (isLight) lightBrick else darkBrick
                drawRect(
                    color = TonalPalette.withAlpha(brick, 0.94f * dim),
                    topLeft = Offset(x + gap * 0.5f, y),
                    size = Size(brickW - gap, brickH),
                )
            }
        }
    }
}
