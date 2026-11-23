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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Train Countryside Wash — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun TrainCountrysideEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "traincountryside")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "traincountryside_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val angle = time * 2f * PI.toFloat()
        val drift = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 17)
        val dawn = TonalPalette.brightness(Color(0xFFC8D080), brightness * dim)
        val deep = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)
        val farField = TonalPalette.brightness(Color(0xFF5A7850), brightness * dim)
        val nearField = TonalPalette.brightness(Color(0xFF2A4030), brightness * dim)
        val frame = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val horizonY = h * (0.48f + 0.025f * sin(angle))

        drawRect(
            brush = Brush.verticalGradient(
                0f to deep,
                0.48f to TonalPalette.mix(deep, farField, 0.48f),
                0.70f to nearField,
                1f to frame,
            ),
        )

        // Muted dawn light softly opens the distant countryside.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(dawn, 0.12f * dim),
                1f to Color.Transparent,
                startY = horizonY - h * 0.16f,
                endY = horizonY + h * 0.16f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(dawn, 0.20f * dim),
                0.45f to TonalPalette.withAlpha(dawn, 0.07f * dim),
                1f to Color.Transparent,
                center = Offset(w * (0.68f + 0.04f * sin(angle)), horizonY),
                radius = (minDim * 0.62f),
            ),
            radius = minDim * 0.62f,
            center = Offset(w * (0.68f + 0.04f * sin(angle)), horizonY),
        )

        // A few soft hills break the skyline without sharp edges.
        val hills = Path().apply {
            moveTo(0f, horizonY + h * 0.07f)
            cubicTo(w * 0.18f, horizonY - h * 0.05f, w * 0.30f, horizonY + h * 0.11f, w * 0.47f, horizonY + h * 0.035f)
            cubicTo(w * 0.65f, horizonY - h * 0.04f, w * 0.77f, horizonY + h * 0.08f, w, horizonY + h * 0.015f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(hills, TonalPalette.withAlpha(farField, 0.76f * dim))

        // Middle-distance field bands move slowly across the window.
        for (layer in 0..2) {
            val top = horizonY + h * (0.10f + layer * 0.105f)
            val shift = ((time * (0.025f + layer * 0.02f) + layer * 0.31f) % 1f) * w
            val bandColor = when (layer) {
                0 -> TonalPalette.mix(farField, dawn, 0.10f)
                1 -> TonalPalette.mix(nearField, farField, 0.22f)
                else -> nearField
            }
            val band = Path().apply {
                moveTo(0f, top)
                cubicTo(w * 0.30f, top - h * 0.025f, w * 0.66f, top + h * 0.035f, w, top + h * 0.005f)
                lineTo(w, top + h * 0.14f)
                lineTo(0f, top + h * 0.14f)
                close()
            }
            drawPath(band, TonalPalette.withAlpha(bandColor, (0.66f - layer * 0.08f) * dim))

            // Scattered tree clumps in each band create a gentle sideways parallax.
            val treeCount = when (quality) {
                GenartQuality.Low -> 7
                GenartQuality.Medium -> 11
                GenartQuality.High -> 16
            }
            repeat(treeCount) { index ->
                val spacing = w / treeCount
                val x = (index * spacing + shift + layer * spacing * 0.43f) % (w + spacing) - spacing * 0.5f
                val rhythm = sin(index * 12.9898f + layer * 5.37f)
                val treeHeight = h * (0.045f + 0.035f * (0.5f + 0.5f * rhythm)) * (1f - layer * 0.16f)
                val baseY = top + h * 0.04f
                val treeWidth = treeHeight * 0.52f
                val treeColor = TonalPalette.withAlpha(
                    if (layer == 0) nearField else frame,
                    (0.52f - layer * 0.09f) * dim,
                )
                val tree = Path().apply {
                    moveTo(x, baseY - treeHeight)
                    lineTo(x - treeWidth * 0.5f, baseY)
                    lineTo(x + treeWidth * 0.5f, baseY)
                    close()
                    moveTo(x, baseY - treeHeight * 0.73f)
                    lineTo(x - treeWidth * 0.68f, baseY - treeHeight * 0.25f)
                    lineTo(x + treeWidth * 0.68f, baseY - treeHeight * 0.25f)
                    close()
                }
                drawPath(tree, treeColor)
            }
        }

        // A dark lower sill and slim side edges establish the passenger window POV.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(frame, 0.62f * dim),
                startY = h * 0.82f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.82f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.18f),
        )
        drawRect(color = TonalPalette.withAlpha(frame, 0.24f * dim), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.025f, h))
        drawRect(color = TonalPalette.withAlpha(frame, 0.24f * dim), topLeft = Offset(w * 0.975f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.025f, h))

        // Subtle glass reflection bands.
        val glint = (0.5f + 0.5f * sin(angle + drift * 0.4f)) * dim
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.48f to TonalPalette.withAlpha(Color.White, 0.012f * glint),
                0.52f to TonalPalette.withAlpha(Color.White, 0.02f * glint),
                1f to Color.Transparent,
            ),
        )
    }
}
