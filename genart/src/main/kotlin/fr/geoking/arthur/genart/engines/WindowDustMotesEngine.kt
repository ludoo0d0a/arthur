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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class DustMote(
    val x: Float,
    val y: Float,
    val radius: Float,
    val drift: Float,
    val phase: Float,
)

/** Warm window light with slow, suspended dust drifting through diagonal rays. */
@Composable
internal fun WindowDustMotesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = remember(count) {
        List(count) { i ->
            DustMote(
                x = seededRange(i * 17 + 3, 0.20f, 0.92f),
                y = seededRange(i * 29 + 7, 0.10f, 0.92f),
                radius = seededRange(i * 41 + 11, 0.0025f, 0.0075f),
                drift = seededRange(i * 53 + 13, 0.008f, 0.025f),
                phase = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "windowdustmotes")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "windowdustmotes_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val sourceX = w * (0.16f + 0.025f * (loopedFbm(time, radius = 1.2f, seedOffset = 29) - 0.5f))
        val sourceY = h * 0.28f
        val warm = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFE8C0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF1A1714), brightness * dim),
                0.58f to TonalPalette.brightness(Color(0xFF2A241C), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF4A4030), brightness * dim * (0.92f + 0.08f * wash)),
            ),
        )

        // A bright, diffuse window source just beyond the left edge of the room.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(warm, 0.58f * dim),
                0.38f to TonalPalette.withAlpha(Color(0xFFFFDFA8), 0.28f * dim),
                1f to Color.Transparent,
                center = Offset(sourceX, sourceY),
                radius = minDim * 0.72f,
            ),
            radius = minDim * 0.72f,
            center = Offset(sourceX, sourceY),
        )

        // Broad diagonal shafts, softened by low-alpha gradients.
        val rayDrift = (loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = 47) - 0.5f) * minDim * 0.025f
        for (i in 0..2) {
            val topX = w * (0.13f + i * 0.13f) + rayDrift
            val bottomX = w * (0.56f + i * 0.18f) + rayDrift
            val spread = w * (0.14f + i * 0.025f)
            val ray = Path().apply {
                moveTo(topX, h * 0.12f)
                lineTo(topX + spread, h * 0.12f)
                lineTo(bottomX + spread * 1.8f, h * 0.92f)
                lineTo(bottomX, h * 0.92f)
                close()
            }
            drawPath(
                path = ray,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, TonalPalette.withAlpha(warm, (0.055f - i * 0.01f) * dim), Color.Transparent),
                    startX = topX,
                    endX = bottomX + spread,
                ),
            )
        }

        // Window frame and mullions are kept subdued behind the haze.
        val frameColor = TonalPalette.withAlpha(Color(0xFF17130F), 0.58f * dim)
        val frameW = w * 0.07f
        drawRect(color = frameColor, topLeft = Offset(0f, h * 0.06f), size = Size(frameW, h * 0.52f))
        drawRect(color = frameColor, topLeft = Offset(w * 0.25f, h * 0.06f), size = Size(w * 0.018f, h * 0.52f))
        drawRect(color = frameColor, topLeft = Offset(0f, h * 0.30f), size = Size(w * 0.27f, h * 0.018f))

        motes.forEachIndexed { index, mote ->
            val a = time * 2f * PI.toFloat() + mote.phase
            val field = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = index * 97 + 5)
            val x = (mote.x + mote.drift * sin(a) + (field - 0.5f) * 0.025f) * w
            val y = (mote.y + mote.drift * cos(a * 0.7f)) * h
            val radius = minDim * mote.radius * (0.8f + 0.4f * field)
            val nearRay = (1f - (x / w - y / h * 0.35f).coerceIn(0f, 1f)).coerceIn(0.25f, 1f)
            val moteColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFE6B8), TonalPalette.pick(paletteColors, index), 0.24f),
                brightness * dim,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(moteColor, (0.72f * nearRay * dim).coerceIn(0f, 0.72f)),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = radius * 2.8f,
                ),
                radius = radius * 2.8f,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(moteColor, 0.78f * nearRay * dim),
                radius = radius * 0.45f,
                center = Offset(x, y),
            )
        }
    }
}
