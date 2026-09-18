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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
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
 * A single rock plunging through atmosphere — plasma trail, heat glow, and a soft horizon wash.
 * Distinct from [AsteroidsEngine] (calm tumbling rocks on a starfield).
 */
@Composable
internal fun AtmosphericAsteroidEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val sparkCount = qualityCount(quality, low = 8, medium = 14, high = 22)
    val sparks = remember(sparkCount) {
        List(sparkCount) { i ->
            AtmosphericAsteroidSparkSeed(
                along = seededUnit(i * 19 + 5),
                side = seededRange(i * 31 + 11, -1f, 1f),
                sizeMul = seededRange(i * 43 + 17, 0.4f, 1.2f),
                phase = seededUnit(i * 59 + 23),
            )
        }
    }
    val rock = remember {
        AtmosphericAsteroidRockSeed(
            dx2 = seededRange(101, -0.55f, 0.55f),
            dy2 = seededRange(111, -0.55f, 0.55f),
            r2 = seededRange(121, 0.5f, 0.85f),
            dx3 = seededRange(131, -0.55f, 0.55f),
            dy3 = seededRange(141, -0.55f, 0.55f),
            r3 = seededRange(151, 0.4f, 0.7f),
        )
    }

    val transition = rememberInfiniteTransition(label = "atmospheric_asteroid")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "atmospheric_asteroid_t",
    )
    val spinT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "atmospheric_asteroid_spin",
    )

    val dim = if (isActive) 1f else 0.55f
    val angle = 0.65f
    val dirX = cos(angle)
    val dirY = sin(angle)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1428), Color(0xFF1A0A08), Color(0xFF060308)),
                ),
            )
            // Soft horizon / atmospheric glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF8844).copy(alpha = 0.22f * dim),
                        Color(0xFFFF5522).copy(alpha = 0.08f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.75f, h * 0.9f),
                    radius = w.coerceAtMost(h) * 0.7f,
                ),
                radius = w.coerceAtMost(h) * 0.7f,
                center = Offset(w * 0.75f, h * 0.9f),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(12.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val life = phase01(t)
            val travel = life * 1.35f - 0.15f
            val x = (-0.05f + dirX * travel) * w
            val y = (0.05f + dirY * travel) * h
            val trailLen = minDim * 0.55f
            val trailStart = Offset(x - dirX * trailLen, y - dirY * trailLen)
            val heat = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFAA55), TonalPalette.pick(paletteColors, 0), 0.25f),
                brightness,
            )
            val edgeFade = (sin(life * PI.toFloat()).toFloat()).coerceAtLeast(0f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(heat, 0.15f * dim * edgeFade),
                        TonalPalette.withAlpha(heat, 0.55f * dim * edgeFade),
                    ),
                    start = trailStart,
                    end = Offset(x, y),
                ),
                start = trailStart,
                end = Offset(x, y),
                strokeWidth = minDim * 0.08f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(heat, 0.7f * dim * edgeFade),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = minDim * 0.12f,
                ),
                radius = minDim * 0.12f,
                center = Offset(x, y),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val life = phase01(t)
            val travel = life * 1.35f - 0.15f
            val x = (-0.05f + dirX * travel) * w
            val y = (0.05f + dirY * travel) * h
            val edgeFade = (sin(life * PI.toFloat()).toFloat()).coerceAtLeast(0f)
            if (edgeFade < 0.02f) return@Canvas

            val heat = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFCC88), TonalPalette.pick(paletteColors, 1), 0.2f),
                brightness,
            )
            val trailLen = minDim * 0.42f
            val trailStart = Offset(x - dirX * trailLen, y - dirY * trailLen)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(heat, 0.65f * dim * edgeFade),
                    ),
                    start = trailStart,
                    end = Offset(x, y),
                ),
                start = trailStart,
                end = Offset(x, y),
                strokeWidth = minDim * 0.018f,
                cap = StrokeCap.Round,
            )

            sparks.forEach { spark ->
                val along = spark.along * trailLen
                val sx = x - dirX * along + (-dirY) * spark.side * minDim * 0.03f
                val sy = y - dirY * along + dirX * spark.side * minDim * 0.03f
                val flicker = 0.5f + 0.5f * sin((phase01(t + spark.phase) * 2f * PI.toFloat())).toFloat()
                drawCircle(
                    color = TonalPalette.withAlpha(heat, 0.55f * dim * edgeFade * flicker),
                    radius = minDim * 0.006f * spark.sizeMul,
                    center = Offset(sx, sy),
                )
            }

            val rockSize = minDim * 0.045f
            val rotationDeg = phase01(spinT) * 360f
            val fill = TonalPalette.brightness(Color(0xFF4A3A32), brightness).copy(alpha = 0.95f * dim * edgeFade)
            rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
                drawCircle(color = fill, radius = rock.r2.coerceAtLeast(0.5f) * rockSize * 0.55f + rockSize * 0.45f, center = Offset(x, y))
                drawCircle(color = fill, radius = rock.r2 * rockSize, center = Offset(x + rock.dx2 * rockSize, y + rock.dy2 * rockSize))
                drawCircle(color = fill, radius = rock.r3 * rockSize, center = Offset(x + rock.dx3 * rockSize, y + rock.dy3 * rockSize))
            }
            drawCircle(
                color = TonalPalette.withAlpha(heat, 0.55f * dim * edgeFade),
                radius = rockSize * 0.35f,
                center = Offset(x + dirX * rockSize * 0.35f, y + dirY * rockSize * 0.35f),
            )
        }
    }
}

private data class AtmosphericAsteroidSparkSeed(
    val along: Float,
    val side: Float,
    val sizeMul: Float,
    val phase: Float,
)

private data class AtmosphericAsteroidRockSeed(
    val dx2: Float,
    val dy2: Float,
    val r2: Float,
    val dx3: Float,
    val dy3: Float,
    val r3: Float,
)
