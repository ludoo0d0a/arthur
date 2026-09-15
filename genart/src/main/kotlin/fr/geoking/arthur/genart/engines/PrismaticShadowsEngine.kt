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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Dynamic floating rounded geometry casting multi-angle soft shadow projections with mixed palettes. */
@Composable
internal fun PrismaticShadowsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 9)
    val shapes = remember(count) {
        List(count) { i ->
            PrismaticShape(
                xA = seededUnit(i * 17 + 5),
                yA = seededUnit(i * 23 + 11),
                xB = seededUnit(i * 37 + 13),
                yB = seededUnit(i * 47 + 19),
                wFrac = seededRange(i * 53 + 23, 0.25f, 0.45f),
                hFrac = seededRange(i * 61 + 29, 0.2f, 0.4f),
                rotationA = seededRange(i * 71 + 31, 0f, 180f),
                rotationB = seededRange(i * 79 + 37, 180f, 360f),
                shadowDist = seededRange(i * 89 + 41, 0.04f, 0.1f),
                colorIdx = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "prismatic_shadows")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "prismatic_shadows_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF04060A))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        shapes.forEach { shape ->
            val morph = sin01(time * 2f * PI.toFloat() + shape.xA * 10f)
            val cx = lerp(shape.xA, shape.xB, morph) * w
            val cy = lerp(shape.yA, shape.yB, morph) * h
            val sw = shape.wFrac * minDim
            val sh = shape.hFrac * minDim
            val rot = lerp(shape.rotationA, shape.rotationB, morph)
            val shadowOff = shape.shadowDist * minDim

            val c = TonalPalette.brightness(TonalPalette.pick(paletteColors, shape.colorIdx), brightness)

            // Multi-colored soft shadow projection
            rotate(rot, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f * dim),
                            TonalPalette.withAlpha(c, 0.2f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(cx + shadowOff, cy + shadowOff),
                        radius = maxOf(sw, sh) * 0.9f,
                    ),
                    topLeft = Offset(cx - sw / 2f + shadowOff, cy - sh / 2f + shadowOff),
                    size = Size(sw, sh),
                    cornerRadius = CornerRadius(minDim * 0.05f),
                )

                // Main shape
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(c, 0.65f * dim),
                            TonalPalette.withAlpha(c, 0.25f * dim),
                        ),
                        start = Offset(cx - sw / 2f, cy - sh / 2f),
                        end = Offset(cx + sw / 2f, cy + sh / 2f),
                    ),
                    topLeft = Offset(cx - sw / 2f, cy - sh / 2f),
                    size = Size(sw, sh),
                    cornerRadius = CornerRadius(minDim * 0.05f),
                )
            }
        }
    }
}

private data class PrismaticShape(
    val xA: Float,
    val yA: Float,
    val xB: Float,
    val yB: Float,
    val wFrac: Float,
    val hFrac: Float,
    val rotationA: Float,
    val rotationB: Float,
    val shadowDist: Float,
    val colorIdx: Int,
)
