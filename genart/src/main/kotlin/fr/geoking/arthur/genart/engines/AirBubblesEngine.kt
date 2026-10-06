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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Underwater air bubbles: glassy beads with offset highlights (Rain-on-Glass refraction),
 * horizontal wobble, size growth near the surface, soft depth gradient + light shafts.
 */
@Composable
internal fun AirBubblesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bubbleCount = qualityCount(quality, low = 18, medium = 30, high = 44)
    val shaftCount = qualityCount(quality, low = 3, medium = 4, high = 6)
    val bubbles = remember(bubbleCount) {
        List(bubbleCount) { i ->
            AirBubblesSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                riseSpeed = seededRange(i * 41 + 11, 0.2f, 0.7f),
                baseRadius = seededRange(i * 53 + 13, 4f, 14f),
                wobbleAmp = seededRange(i * 67 + 19, 0.015f, 0.05f),
                wobbleFreq = seededRange(i * 79 + 23, 0.4f, 1.6f),
                wobblePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                highlightAngle = seededRange(i * 97 + 31, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val shafts = remember(shaftCount) {
        List(shaftCount) { i ->
            AirBubblesShaftSeed(
                angle = seededRange(i * 17 + 5, -0.35f, 0.35f),
                width = seededRange(i * 29 + 11, 0.03f, 0.07f),
                swayAmp = seededRange(i * 41 + 13, 0.01f, 0.025f),
                swayFreq = seededRange(i * 53 + 17, 0.1f, 0.25f),
                alpha = seededRange(i * 67 + 19, 0.06f, 0.14f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "airbubbles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "airbubbles_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val aqua = TonalPalette.mix(Color(0xFF3EC8E8), TonalPalette.pick(paletteColors, 0), 0.25f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TonalPalette.brightness(Color(0xFF0A3A52), brightness * dim),
                        TonalPalette.brightness(Color(0xFF041820), brightness * dim),
                        Color(0xFF020A10),
                    ),
                    startY = 0f,
                    endY = h,
                ),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(8.dp)) {
            val w = size.width
            val h = size.height
            val originX = w * 0.5f
            val originY = -h * 0.05f
            val reach = h * 1.35f
            val time = phase01(t) * 2f * PI.toFloat()
            shafts.forEach { shaft ->
                val a = shaft.angle + shaft.swayAmp * sin(time * shaft.swayFreq)
                val leftX = originX + sin(a - shaft.width) * reach
                val leftY = originY + cos(a - shaft.width) * reach
                val rightX = originX + sin(a + shaft.width) * reach
                val rightY = originY + cos(a + shaft.width) * reach
                val path = Path().apply {
                    moveTo(originX, originY)
                    lineTo(leftX, leftY)
                    lineTo(rightX, rightY)
                    close()
                }
                val tint = TonalPalette.mix(aqua, TonalPalette.pick(paletteColors, shaft.colorIndex), 0.2f)
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, shaft.alpha * dim),
                            TonalPalette.withAlpha(tint, shaft.alpha * 0.3f * dim),
                            Color.Transparent,
                        ),
                        start = Offset(originX, originY),
                        end = Offset((leftX + rightX) * 0.5f, (leftY + rightY) * 0.5f),
                    ),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            bubbles.forEach { b ->
                val life = phase01(b.y0 + b.riseSpeed * time)
                val depth = 1f - life // 1 = deep, 0 = surface
                val y = depth * h
                val wobble = sin(time * 2f * PI.toFloat() * b.wobbleFreq + b.wobblePhase) * b.wobbleAmp
                val x = phase01(b.x0 + wobble) * w
                // Grow near surface
                val grow = 1f + (1f - depth) * 0.85f
                val radius = b.baseRadius * grow
                val fade = (life / 0.08f).coerceIn(0f, 1f) * ((1f - life) / 0.15f).coerceIn(0f, 1f)
                val depthFade = 0.45f + 0.55f * (1f - depth * 0.65f)
                val base = TonalPalette.mix(Color(0xFFB8E8F8), TonalPalette.pick(paletteColors, b.colorIndex), 0.25f)
                val color = TonalPalette.brightness(base, brightness)
                val alpha = 0.42f * fade * depthFade * dim

                // Glassy fill
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color.White, alpha * 0.22f),
                            TonalPalette.withAlpha(color, alpha * 0.28f),
                            TonalPalette.withAlpha(color, alpha * 0.08f),
                        ),
                        center = Offset(x - radius * 0.2f, y - radius * 0.25f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha),
                    radius = radius,
                    center = Offset(x, y),
                    style = Stroke(width = 1.3f),
                )
                // Offset highlight (Rain-on-Glass refraction trick)
                val hx = x + cos(b.highlightAngle) * radius * 0.35f
                val hy = y + sin(b.highlightAngle) * radius * 0.35f
                drawCircle(
                    color = TonalPalette.withAlpha(Color.White, alpha * 0.7f),
                    radius = radius * 0.22f,
                    center = Offset(hx, hy),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(Color.White, alpha * 0.35f),
                    radius = radius * 0.1f,
                    center = Offset(
                        x + cos(b.highlightAngle + 2.2f) * radius * 0.4f,
                        y + sin(b.highlightAngle + 2.2f) * radius * 0.4f,
                    ),
                )
            }
        }
    }
}

private data class AirBubblesSeed(
    val x0: Float,
    val y0: Float,
    val riseSpeed: Float,
    val baseRadius: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float,
    val highlightAngle: Float,
    val colorIndex: Int,
)

private data class AirBubblesShaftSeed(
    val angle: Float,
    val width: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alpha: Float,
    val colorIndex: Int,
)
