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
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** One or two tangled tumbleweed silhouettes rolling across a Dunes-style desert backdrop. */
@Composable
internal fun TumbleweedDriftEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ballCount = qualityCount(quality, low = 1, medium = 1, high = 2)
    val lobeCount = qualityCount(quality, low = 7, medium = 9, high = 12)

    val balls = remember(ballCount) {
        List(ballCount) { i ->
            TumbleweedSeed(
                startXFrac = seededRange(i * 71 + 3, -0.2f, 0f),
                endXFrac = seededRange(i * 73 + 7, 1f, 1.2f),
                yFrac = seededRange(i * 79 + 11, 0.72f, 0.9f),
                radiusFrac = seededRange(i * 83 + 13, 0.05f, 0.08f),
                driftSpeed = seededRange(i * 89 + 17, 0.6f, 1.1f),
                phaseOffset = seededUnit(i * 97 + 19),
                colorIndex = i,
                lobes = List(lobeCount) { j ->
                    TumbleweedLobe(
                        angle = (j / lobeCount.toFloat()) * 2f * PI.toFloat() +
                            seededRange(i * 401 + j * 11 + 23, -0.25f, 0.25f),
                        distanceFrac = seededRange(i * 409 + j * 13 + 29, 0.45f, 1f),
                        sizeFrac = seededRange(i * 419 + j * 17 + 31, 0.35f, 0.65f),
                    )
                },
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "tumbleweed")
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tumbleweed_drift",
    )

    val dim = if (isActive) 1f else 0.5f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.55f)
        val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.3f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, Color(0xFF05040A)),
            ),
        )

        balls.forEach { ball ->
            val t = phase01(driftT * ball.driftSpeed + ball.phaseOffset)
            val drift = ball.startXFrac + (ball.endXFrac - ball.startXFrac) * t
            val x = drift * w
            val y = ball.yFrac * h
            val radius = ball.radiusFrac * w.coerceAtMost(h * 2f)
            val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val rotationDeg = drift * 360f * 6f

            val base = TonalPalette.pick(paletteColors, ball.colorIndex)
            val silhouette = TonalPalette.brightness(base, brightness * 0.22f)
            val alpha = (0.55f * edgeFade * dim).coerceIn(0f, 0.55f)
            if (alpha <= 0.01f) return@forEach

            val fill = TonalPalette.withAlpha(silhouette, alpha)
            rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
                drawCircle(color = fill, radius = radius * 0.55f, center = Offset(x, y))
                ball.lobes.forEach { lobe ->
                    val lobeDistance = radius * lobe.distanceFrac
                    val lobeRadius = radius * lobe.sizeFrac * 0.55f
                    val lobeCenter = Offset(
                        x + lobeDistance * kotlin.math.cos(lobe.angle),
                        y + lobeDistance * sin(lobe.angle),
                    )
                    drawCircle(color = fill, radius = lobeRadius, center = lobeCenter)
                }
            }
        }
    }
}

private data class TumbleweedLobe(
    val angle: Float,
    val distanceFrac: Float,
    val sizeFrac: Float,
)

private data class TumbleweedSeed(
    val startXFrac: Float,
    val endXFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val driftSpeed: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
    val lobes: List<TumbleweedLobe>,
)
