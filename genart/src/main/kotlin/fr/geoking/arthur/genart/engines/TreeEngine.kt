package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** A single tree whose canopy sways as one rigid body, pivoting from the trunk's top. */
@Composable
internal fun TreeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val blobCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val canopyGreen = Color(0xFF3D8B4E)
    val canopyHighlight = Color(0xFF9CCB6B)
    val trunkBrown = Color(0xFF4A3222)

    val blobs = remember(blobCount) {
        List(blobCount) { i ->
            CanopyBlobSeed(
                dx = seededRange(i * 31 + 7, -0.5f, 0.5f),
                dy = seededRange(i * 43 + 11, -0.42f, 0.12f),
                radiusFrac = seededRange(i * 59 + 13, 0.3f, 0.56f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "tree")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tree_t",
    )

    val swayScale = if (isActive) 1f else 0.3f
    val alphaScale = if (isActive) 1f else 0.7f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.45f)
            val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.28f)
            val skyBot = Color(0xFF0A1018)
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(skyTop, skyMid, skyBot)),
            )

            val trunkBaseX = w * 0.5f
            val trunkBaseY = h
            val trunkTopY = h * 0.5f
            val trunkBottomHalfWidth = w * 0.035f
            val trunkTopHalfWidth = w * 0.012f
            val trunkColor = TonalPalette.brightness(trunkBrown, brightness)
            val trunkPath = Path().apply {
                moveTo(trunkBaseX - trunkBottomHalfWidth, trunkBaseY)
                lineTo(trunkBaseX - trunkTopHalfWidth, trunkTopY)
                lineTo(trunkBaseX + trunkTopHalfWidth, trunkTopY)
                lineTo(trunkBaseX + trunkBottomHalfWidth, trunkBaseY)
                close()
            }
            drawPath(path = trunkPath, color = trunkColor)
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(14.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val pivotX = w * 0.5f
            val pivotY = h * 0.5f
            val time = phase01(t) * 2f * PI.toFloat()
            val swayDeg = sin(time) * 7f * swayScale
            val canopyCenterY = pivotY - minDim * 0.16f
            val canopyRadius = minDim * 0.34f

            rotate(degrees = swayDeg, pivot = Offset(pivotX, pivotY)) {
                blobs.forEach { blob ->
                    val cx = pivotX + blob.dx * canopyRadius * 1.5f
                    val cy = canopyCenterY + blob.dy * canopyRadius * 1.5f
                    val radius = blob.radiusFrac * canopyRadius
                    val base = TonalPalette.pick(paletteColors, blob.colorIndex)
                    val tint = TonalPalette.mix(base, canopyGreen, 0.6f)
                    val haloColor = TonalPalette.brightness(tint, brightness * 0.8f)
                    val coreColor = TonalPalette.brightness(
                        TonalPalette.mix(tint, canopyHighlight, 0.5f),
                        brightness,
                    )
                    drawCircle(
                        color = TonalPalette.withAlpha(haloColor, 0.28f * alphaScale),
                        radius = radius * 1.35f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = TonalPalette.withAlpha(coreColor, 0.55f * alphaScale),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }
            }
        }
    }
}

private data class CanopyBlobSeed(
    val dx: Float,
    val dy: Float,
    val radiusFrac: Float,
    val colorIndex: Int,
)
