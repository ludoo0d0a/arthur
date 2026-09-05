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
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Multi-blob radial gradients with slow center/color morph — Tapet wallpaper feel. */
@Composable
internal fun GradientMeshEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 8)
    val nodes = remember(count) {
        List(count) { i ->
            MeshNode(
                xA = seededUnit(i * 17 + 3),
                yA = seededUnit(i * 29 + 7),
                xB = seededUnit(i * 41 + 11),
                yB = seededUnit(i * 53 + 13),
                radiusFrac = seededRange(i * 67 + 19, 0.28f, 0.55f),
                morphPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 89 + 29, 0.22f, 0.42f),
                colorIndex = i,
                colorIndexB = i + 2,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "gradient_mesh")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradient_mesh_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF05040A))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        nodes.forEach { node ->
            val morph = sin01(time * 2f * PI.toFloat() + node.morphPhase)
            val x = lerp(node.xA, node.xB, morph) * w
            val y = lerp(node.yA, node.yB, morph) * h
            val radius = node.radiusFrac * minDim * (0.85f + 0.15f * morph)
            val cA = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, node.colorIndex),
                brightness,
            )
            val cB = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, node.colorIndexB),
                brightness,
            )
            val tint = TonalPalette.mix(cA, cB, morph)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, node.alphaBase * dim),
                        TonalPalette.withAlpha(tint, node.alphaBase * 0.45f * dim),
                        TonalPalette.withAlpha(tint, 0.05f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class MeshNode(
    val xA: Float,
    val yA: Float,
    val xB: Float,
    val yB: Float,
    val radiusFrac: Float,
    val morphPhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
    val colorIndexB: Int,
)
