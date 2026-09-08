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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun AuroraEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ribbonCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val ribbons = remember(ribbonCount) {
        List(ribbonCount) { i ->
            RibbonSeed(
                baseYFrac = seededRange(i * 17 + 3, 0.12f, 0.5f),
                amplitudeFrac = seededRange(i * 29 + 7, 0.02f, 0.06f),
                cycles = seededRange(i * 41 + 11, 1f, 2.2f),
                phaseOffset = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                thicknessFrac = seededRange(i * 67 + 19, 0.07f, 0.14f),
                colorIndex = i,
                speedMul = seededRange(i * 71 + 23, 0.6f, 1.3f),
                bottomPhaseShift = seededRange(i * 83 + 29, 0.3f, 0.9f),
            )
        }
    }
    val starCount = qualityCount(quality, low = 20, medium = 34, high = 48)
    val stars = remember(starCount) {
        List(starCount) { j ->
            AuroraStarSeed(
                xFrac = seededUnit(j * 13 + 5),
                yFrac = seededUnit(j * 19 + 9),
                alpha = seededRange(j * 31 + 11, 0.15f, 0.6f),
                radius = seededRange(j * 37 + 17, 0.8f, 2.2f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora_t",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF040A1A), Color(0xFF000000)),
                ),
            )
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha * 0.9f),
                    radius = star.radius,
                    center = Offset(star.xFrac * w, star.yFrac * h),
                )
            }
        }
        // Real gaussian blur on the ribbons only — keeps the star pinpoints crisp above.
        Canvas(modifier = Modifier.fillMaxSize().blur(12.dp)) {
            val w = size.width
            val h = size.height
            val timeAngle = phase01(t) * 2f * PI.toFloat()
            val activeFactor = if (isActive) 1f else 0.55f
            val segments = 32
            ribbons.forEach { ribbon ->
                val freqPerPixel = ribbon.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
                val amplitudePx = ribbon.amplitudeFrac * h * activeFactor
                val baseYPx = ribbon.baseYFrac * h
                val thicknessPx = ribbon.thicknessFrac * h
                val ribbonTime = timeAngle * ribbon.speedMul

                val topPoints = ArrayList<Offset>(segments + 1)
                val bottomPoints = ArrayList<Offset>(segments + 1)
                for (k in 0..segments) {
                    val x = (k / segments.toFloat()) * w
                    val topY = baseYPx + amplitudePx * sin(x * freqPerPixel + ribbonTime + ribbon.phaseOffset)
                    topPoints.add(Offset(x, topY))
                    val bottomY = baseYPx + thicknessPx +
                        amplitudePx * sin(
                            x * freqPerPixel + ribbonTime + ribbon.phaseOffset + ribbon.bottomPhaseShift,
                        )
                    bottomPoints.add(Offset(x, bottomY))
                }

                val path = Path()
                path.moveTo(topPoints[0].x, topPoints[0].y)
                for (k in 1 until topPoints.size) {
                    val prev = topPoints[k - 1]
                    val curr = topPoints[k]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    path.quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
                path.lineTo(topPoints.last().x, topPoints.last().y)
                path.lineTo(bottomPoints.last().x, bottomPoints.last().y)
                val bottomReversed = bottomPoints.asReversed()
                for (k in 1 until bottomReversed.size) {
                    val prev = bottomReversed[k - 1]
                    val curr = bottomReversed[k]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    path.quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
                path.lineTo(bottomReversed.last().x, bottomReversed.last().y)
                path.close()

                val baseColor = TonalPalette.brightness(
                    TonalPalette.pick(paletteColors, ribbon.colorIndex),
                    brightness,
                )
                val alpha = seededRange(ribbon.colorIndex * 89 + 31, 0.15f, 0.35f) * activeFactor
                val glowColor = TonalPalette.withAlpha(baseColor, alpha)
                val topExtent = (baseYPx - amplitudePx).coerceAtLeast(0f)
                val bottomExtent = (baseYPx + thicknessPx + amplitudePx).coerceAtMost(h)

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        startY = topExtent,
                        endY = bottomExtent,
                    ),
                    style = Fill,
                )
            }
        }
    }
}

private data class RibbonSeed(
    val baseYFrac: Float,
    val amplitudeFrac: Float,
    val cycles: Float,
    val phaseOffset: Float,
    val thicknessFrac: Float,
    val colorIndex: Int,
    val speedMul: Float,
    val bottomPhaseShift: Float,
)

private data class AuroraStarSeed(
    val xFrac: Float,
    val yFrac: Float,
    val alpha: Float,
    val radius: Float,
)
