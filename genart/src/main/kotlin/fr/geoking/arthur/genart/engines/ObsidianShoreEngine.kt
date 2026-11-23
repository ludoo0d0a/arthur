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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

private data class ShoreRock(val x: Float, val width: Float, val height: Float, val facet: Float)

/** A moonlit volcanic shore with glassy black stones and slow, cool tides. */
@Composable
internal fun ObsidianShoreEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rockCount = qualityCount(quality, low = 5, medium = 8, high = 12)
    val rocks = remember(rockCount) {
        List(rockCount) { i ->
            ShoreRock(
                x = (i + seededRange(i * 13 + 5, 0.1f, 0.9f)) / rockCount,
                width = seededRange(i * 23 + 9, 0.045f, 0.14f),
                height = seededRange(i * 31 + 11, 0.035f, 0.12f),
                facet = seededRange(i * 43 + 17, 0.28f, 0.72f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "obsidianshore")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "obsidianshore_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 11)
        val tide = 0.5f + 0.5f * sin(time * (2f * PI.toFloat()))
        val horizon = h * 0.57f
        val shoreLine = h * (0.79f + 0.012f * (wash - 0.5f))
        val night = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)
        val stone = TonalPalette.brightness(Color(0xFF1A1E28), brightness * dim)
        val moon = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF6080A0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF07090D),
                0.52f to night,
                0.78f to Color(0xFF11151D),
                1f to Color(0xFF080A0E),
            ),
        )

        // Distant moonlit haze sits low over the horizon.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(moon, 0.2f * dim),
                0.42f to TonalPalette.withAlpha(moon, 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.72f, horizon + h * 0.01f),
                radius = minDim * 0.47f,
            ),
            radius = minDim * 0.47f,
            center = Offset(w * 0.72f, horizon + h * 0.01f),
        )

        // A dark sea plane with subtle reflected moonlight.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF101722),
                0.42f to Color(0xFF0B1119),
                1f to Color(0xFF080B10),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = androidx.compose.ui.geometry.Size(w, h - horizon),
        )
        val reflectionX = w * 0.72f
        for (i in 0 until 8) {
            val y = horizon + h * (0.05f + i * 0.035f) + h * 0.006f * sin(time * 2f * PI.toFloat() + i)
            val halfWidth = minDim * (0.06f + 0.045f * (1f - i / 8f)) * (0.8f + 0.2f * tide)
            val centerShift = minDim * 0.035f * sin(time * 2f * PI.toFloat() + i * 0.7f)
            drawLine(
                color = TonalPalette.withAlpha(moon, (0.16f - i * 0.012f) * dim),
                start = Offset(reflectionX + centerShift - halfWidth, y),
                end = Offset(reflectionX + centerShift + halfWidth, y),
                strokeWidth = minDim * 0.003f,
            )
        }

        // Uneven foamy edge where the tide reaches the black volcanic beach.
        val foam = Path().apply {
            moveTo(0f, shoreLine)
            for (i in 0..12) {
                val x = w * i / 12f
                val n = loopedFbm(time, radius = 1.6f, octaves = 2, seedOffset = 30 + i * 17)
                val y = shoreLine - h * (0.018f + 0.024f * n) * (0.65f + 0.35f * tide)
                lineTo(x, y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = foam,
            brush = Brush.verticalGradient(
                0f to Color(0xFF141923),
                0.18f to stone,
                1f to night,
                startY = shoreLine - h * 0.04f,
                endY = h,
            ),
        )
        drawLine(
            color = TonalPalette.withAlpha(moon, 0.2f * dim),
            start = Offset(0f, shoreLine - h * 0.025f),
            end = Offset(w, shoreLine - h * 0.025f),
            strokeWidth = minDim * 0.0025f,
        )

        // Angular obsidian cobbles, each with one cool glassy facet.
        rocks.forEachIndexed { index, rock ->
            val x = (rock.x * w + w) % w
            val baseY = shoreLine + h * 0.11f + (index % 3) * h * 0.025f
            val rw = minDim * rock.width
            val rh = minDim * rock.height
            val path = Path().apply {
                moveTo(x - rw * 0.5f, baseY)
                lineTo(x - rw * 0.36f, baseY - rh * 0.62f)
                lineTo(x - rw * 0.05f, baseY - rh)
                lineTo(x + rw * 0.32f, baseY - rh * 0.78f)
                lineTo(x + rw * 0.5f, baseY - rh * 0.22f)
                lineTo(x + rw * 0.28f, baseY + rh * 0.08f)
                close()
            }
            drawPath(path, color = if (index % 2 == 0) Color(0xFF080A0E) else Color(0xFF11151C))
            val facet = Path().apply {
                moveTo(x - rw * 0.05f, baseY - rh)
                lineTo(x + rw * 0.32f, baseY - rh * 0.78f)
                lineTo(x + rw * 0.5f, baseY - rh * 0.22f)
                lineTo(x + rw * (rock.facet - 0.5f), baseY - rh * 0.12f)
                close()
            }
            drawPath(facet, color = TonalPalette.withAlpha(moon, 0.14f * dim))
            drawPath(path, color = Color(0xFF252C38), style = Stroke(width = minDim * 0.002f))
        }
    }
}
