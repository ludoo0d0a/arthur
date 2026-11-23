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
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange

private data class RoadsideTree(val x: Float, val width: Float, val height: Float, val layer: Int)

/** Soft Ambient POV Velo Parallax Trees — seamless loop via loopedFbm. */
@Composable
internal fun VeloParallaxTreesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val trees = remember(count) {
        List(count * 2) { i ->
            val layer = i % 2
            RoadsideTree(
                x = seededRange(i * 17 + 3, 0f, 1f),
                width = seededRange(i * 29 + 7, 0.055f, 0.11f),
                height = seededRange(i * 41 + 11, 0.12f, 0.25f) * if (layer == 0) 0.72f else 1f,
                layer = layer,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "veloparallaxtrees")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "veloparallaxtrees_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val sky = TonalPalette.brightness(Color(0xFF2A4030), brightness * dim)
        val ground = TonalPalette.brightness(Color(0xFF1A281C), brightness * dim)
        val horizonGlow = TonalPalette.brightness(Color(0xFF80A070), brightness * dim)
        val horizon = h * (0.53f + 0.025f * (wash - 0.5f))

        drawRect(brush = Brush.verticalGradient(listOf(sky, ground)))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.48f to TonalPalette.withAlpha(horizonGlow, 0.24f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.13f,
                endY = horizon + h * 0.13f,
            ),
        )

        // Far tree line moves slowly against the warm distant horizon.
        trees.filter { it.layer == 0 }.forEach { tree ->
            val x = ((tree.x - time * 0.12f + 1f) % 1f) * w
            val baseY = horizon + h * 0.065f
            val treeH = h * tree.height * 0.55f
            val treeW = minDim * tree.width * 0.78f
            drawPath(
                path = Path().apply {
                    moveTo(x - treeW * 0.5f, baseY)
                    lineTo(x, baseY - treeH)
                    lineTo(x + treeW * 0.5f, baseY)
                    close()
                },
                color = TonalPalette.withAlpha(Color(0xFF1A2A20), 0.72f * dim),
            )
        }

        // Low fields and a perspective path draw the rider's direction of travel.
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF263326), 0f),
                1f to TonalPalette.withAlpha(Color(0xFF182018), 0.85f * dim),
                startY = horizon + h * 0.12f,
                endY = h,
            ),
            topLeft = Offset(0f, horizon + h * 0.12f),
            size = Size(w, h - (horizon + h * 0.12f)),
        )
        val path = Path().apply {
            moveTo(w * 0.43f, horizon + h * 0.13f)
            lineTo(w * 0.57f, horizon + h * 0.13f)
            lineTo(w * 0.86f, h)
            lineTo(w * 0.14f, h)
            close()
        }
        drawPath(path, color = TonalPalette.withAlpha(Color(0xFF101712), 0.64f * dim))
        drawLine(
            color = TonalPalette.withAlpha(horizonGlow, 0.38f * dim),
            start = Offset(w * 0.5f, horizon + h * 0.17f),
            end = Offset(w * 0.5f, h),
            strokeWidth = minDim * 0.003f,
        )

        // Near trees sweep past faster, creating a clear second parallax plane.
        trees.filter { it.layer == 1 }.forEach { tree ->
            val x = ((tree.x - time * 0.34f + 1f) % 1f) * w
            val baseY = horizon + h * 0.24f + tree.height * h * 0.2f
            val treeH = h * tree.height
            val treeW = minDim * tree.width
            val trunkW = treeW * 0.11f
            drawRect(
                color = TonalPalette.withAlpha(Color(0xFF111912), 0.76f * dim),
                topLeft = Offset(x - trunkW * 0.5f, baseY - treeH * 0.18f),
                size = Size(trunkW, treeH * 0.18f),
            )
            for (branch in 0..2) {
                val top = baseY - treeH * (0.42f + branch * 0.18f)
                val halfW = treeW * (0.5f - branch * 0.10f)
                drawPath(
                    path = Path().apply {
                        moveTo(x - halfW, top + treeH * 0.16f)
                        lineTo(x, top - treeH * 0.30f)
                        lineTo(x + halfW, top + treeH * 0.16f)
                        close()
                    },
                    color = TonalPalette.withAlpha(Color(0xFF101A13), 0.84f * dim),
                )
            }
        }
    }
}
