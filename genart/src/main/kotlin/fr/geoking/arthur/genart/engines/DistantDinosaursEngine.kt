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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class DinosaurSeed(
    val xFrac: Float,
    val scaleFrac: Float,
    val faceDir: Float,
    val swayFreq: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

private val duskSkyTop = Color(0xFF241736)
private val duskSkyHorizon = Color(0xFF6B4A55)
private val groundColor = Color(0xFF0C0912)

/** Quiet dusk horizon with a couple of long-necked silhouettes swaying their heads. */
@Composable
internal fun DistantDinosaursEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 1, medium = 1, high = 2)
    val dinosaurs = remember(count) {
        List(count) { i ->
            DinosaurSeed(
                xFrac = seededRange(i * 53 + 601, 0.22f, 0.78f),
                scaleFrac = seededRange(i * 61 + 607, 0.8f, 1.15f),
                faceDir = if (seededUnit(i * 67 + 613) < 0.5f) -1f else 1f,
                swayFreq = seededRange(i * 71 + 617, 0.12f, 0.2f),
                phaseOffset = seededRange(i * 79 + 619, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "distant_dinosaurs")
    val timeDriver by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween((60_000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "distant_dinosaurs_time",
    )

    val swayDamp = if (isActive) 1f else 0.25f
    val glowFactor = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val groundY = h * 0.74f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(duskSkyTop, TonalPalette.withAlpha(duskSkyHorizon, glowFactor), groundColor),
            ),
        )
        drawRect(
            color = groundColor,
            topLeft = Offset(0f, groundY),
            size = Size(w, h - groundY),
        )

        dinosaurs.forEach { dino ->
            val bodyWidth = minDim * dino.scaleFrac * 0.34f
            val bodyHeight = bodyWidth * 0.55f
            val bodyCenter = Offset(w * dino.xFrac, groundY - bodyHeight * 0.35f)

            val silhouette = TonalPalette.brightness(
                TonalPalette.mix(groundColor, TonalPalette.pick(paletteColors, dino.colorIndex), 0.14f),
                brightness.coerceIn(0.3f, 1.2f),
            )

            drawOval(
                color = silhouette,
                topLeft = Offset(bodyCenter.x - bodyWidth / 2f, bodyCenter.y - bodyHeight / 2f),
                size = Size(bodyWidth, bodyHeight),
            )

            val neckHeight = bodyHeight * 3.1f
            val headRadius = bodyWidth * 0.16f
            val neckBase = Offset(
                bodyCenter.x + dino.faceDir * bodyWidth * 0.32f,
                bodyCenter.y - bodyHeight * 0.32f,
            )
            val tipDx = dino.faceDir * neckHeight * 0.45f
            val bendDx = dino.faceDir * neckHeight * 0.22f

            val swayAngle = sin(timeDriver * dino.swayFreq + dino.phaseOffset) * 6f * swayDamp

            rotate(degrees = swayAngle, pivot = neckBase) {
                val baseHalfWidth = headRadius * 1.1f
                val tipHalfWidth = headRadius * 0.6f
                val tipCenter = Offset(neckBase.x + tipDx, neckBase.y - neckHeight)
                val neckPath = Path().apply {
                    moveTo(neckBase.x - baseHalfWidth, neckBase.y)
                    cubicTo(
                        neckBase.x - baseHalfWidth + bendDx * 0.3f,
                        neckBase.y - neckHeight * 0.55f,
                        tipCenter.x - tipHalfWidth - bendDx * 0.2f,
                        tipCenter.y + neckHeight * 0.25f,
                        tipCenter.x - tipHalfWidth,
                        tipCenter.y,
                    )
                    lineTo(tipCenter.x + tipHalfWidth, tipCenter.y)
                    cubicTo(
                        tipCenter.x + tipHalfWidth - bendDx * 0.2f,
                        tipCenter.y + neckHeight * 0.25f,
                        neckBase.x + baseHalfWidth + bendDx * 0.3f,
                        neckBase.y - neckHeight * 0.55f,
                        neckBase.x + baseHalfWidth,
                        neckBase.y,
                    )
                    close()
                }
                drawPath(path = neckPath, color = silhouette, style = Fill)
                drawCircle(color = silhouette, radius = headRadius, center = tipCenter)
            }
        }
    }
}
