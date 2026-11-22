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
 * Checker-shadow illusion board — soft drifting cylinder shadow; matched tile luminance in shade.
 */
@Composable
internal fun CheckerShadowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grid = qualityCount(quality, low = 8, medium = 10, high = 12)
    val transition = rememberInfiniteTransition(label = "checker_shadow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((44000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "checker_shadow_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = TonalPalette.brightness(Color(0xFF14161C), brightness * 0.42f))
        val time = phase01(t) * 2f * PI.toFloat()
        val shadowCenterX = w * (0.38f + 0.24f * sin01(time))
        val dim = if (isActive) 1f else 0.65f
        val cellW = w / grid
        val cellH = h / grid
        val lightA = Color(0xFFFAFAFA)
        val darkB = Color(0xFF141414)
        val matchedGray = Color(0xFF6E6E6E)
        val shadowRadius = w * 0.34f
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val x = col * cellW
                val y = row * cellH
                val cx = x + cellW * 0.5f
                val inShadow = kotlin.math.abs(cx - shadowCenterX) < shadowRadius
                val shadowT = if (inShadow) {
                    val d = kotlin.math.abs(cx - shadowCenterX) / shadowRadius
                    (1f - d * d).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val checkerLight = (row + col) % 2 == 0
                val color = when {
                    inShadow && ((checkerLight && col < grid / 2) || (!checkerLight && col >= grid / 2)) ->
                        matchedGray
                    checkerLight -> TonalPalette.mix(lightA, Color(0xFFE5E7EB), 0.08f)
                    else -> TonalPalette.mix(darkB, TonalPalette.pick(paletteColors, 3), 0.12f)
                }
                val shaded = if (shadowT > 0.01f && color != matchedGray) {
                    TonalPalette.mix(color, Color(0xFF2A2A2A), shadowT * 0.55f)
                } else {
                    color
                }
                drawRect(
                    color = TonalPalette.withAlpha(shaded, 0.95f * dim),
                    topLeft = Offset(x, y),
                    size = Size(cellW - 1f, cellH - 1f),
                )
            }
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.Black, 0.38f * dim),
                    TonalPalette.withAlpha(Color.Black, 0.12f * dim),
                    Color.Transparent,
                ),
                center = Offset(shadowCenterX, h * 0.52f),
                radius = shadowRadius,
            ),
        )
    }
}