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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Tiny dark ants filing along a shared winding path over a warm sunlit ground. */
@Composable
internal fun AntTrailsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val ants = remember(count) {
        List(count) { i ->
            AntSeed(
                phaseOffset = i / count.toFloat(),
                scale = seededRange(i * 31 + 7, 0.7f, 1.2f),
                colorIndex = i,
            )
        }
    }
    val pathShape = remember {
        AntPathShape(
            centerXFrac = seededRange(3, 0.42f, 0.58f),
            centerYFrac = seededRange(11, 0.5f, 0.62f),
            ampXFrac = seededRange(17, 0.28f, 0.38f),
            ampYFrac = seededRange(23, 0.14f, 0.22f),
            freqX = seededRange(29, 1.6f, 2.4f),
            freqY = seededRange(37, 2.6f, 3.4f),
            phaseX = seededRange(41, 0f, 2f * PI.toFloat()),
        )
    }

    val transition = rememberInfiniteTransition(label = "anttrails")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "anttrails_t",
    )

    val dimFactor = if (isActive) 1f else 0.7f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val groundTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.6f)
        val groundBottom = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.32f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundTop, groundBottom, Color(0xFF120C10)),
            ),
        )

        val cx = pathShape.centerXFrac * w
        val cy = pathShape.centerYFrac * h
        val ampX = pathShape.ampXFrac * w
        val ampY = pathShape.ampYFrac * h

        ants.forEach { ant ->
            val ti = phase01(t + ant.phaseOffset) * (2f * PI.toFloat())
            val x = cx + ampX * sin(pathShape.freqX * ti + pathShape.phaseX)
            val y = cy + ampY * sin(pathShape.freqY * ti)

            val base = TonalPalette.pick(paletteColors, ant.colorIndex)
            val color = TonalPalette.brightness(base, brightness * 0.35f)
            val alpha = (0.55f * dimFactor).coerceIn(0f, 1f)
            val radius = ant.scale * minOf(w, h) * 0.008f

            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class AntSeed(
    val phaseOffset: Float,
    val scale: Float,
    val colorIndex: Int,
)

private data class AntPathShape(
    val centerXFrac: Float,
    val centerYFrac: Float,
    val ampXFrac: Float,
    val ampYFrac: Float,
    val freqX: Float,
    val freqY: Float,
    val phaseX: Float,
)
