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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ConstellationEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 22, high = 34)
    val stars = remember(count) {
        List(count) { i ->
            ConstellationStar(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 1.2f, 3.2f),
                twinkleFreq = seededRange(i * 53 + 13, 0.4f, 1.6f),
                phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val shootingStars = remember {
        List(2) { i ->
            ShootingStarSeed(
                startX = seededRange(i * 101 + 3, 0.05f, 0.55f),
                startY = seededRange(i * 103 + 7, 0.05f, 0.35f),
                lengthFrac = seededRange(i * 107 + 11, 0.18f, 0.32f),
                angle = seededRange(i * 109 + 13, 0.35f, 0.75f),
                phaseOffset = seededUnit(i * 113 + 17),
                duration = seededRange(i * 127 + 19, 0.12f, 0.22f),
                thickness = seededRange(i * 131 + 23, 1.8f, 3.2f),
                colorIndex = i + 5,
            )
        }
    }
    val planet = remember {
        ConstellationPlanetSeed(
            xFrac = seededRange(401, 0.62f, 0.82f),
            yFrac = seededRange(409, 0.55f, 0.78f),
            radiusFrac = seededRange(419, 0.055f, 0.085f),
            ring = seededUnit(421) > 0.35f,
            colorIndex = 3,
        )
    }
    val transition = rememberInfiniteTransition(label = "constellation")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "constellation_t",
    )
    val planetT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((75000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "constellation_planet",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF07040F), Color(0xFF050914)),
            ),
        )
        val amplitude = if (isActive) 0.5f else 0.25f
        val dim = if (isActive) 1f else 0.6f
        val positions = stars.map { s -> Offset(s.x * w, s.y * h) }
        for (i in stars.indices) {
            if (i % 2 != 0) continue
            val j = (i + 1) % stars.size
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), brightness)
            drawLine(
                color = TonalPalette.withAlpha(color, 0.09f * dim),
                start = positions[i],
                end = positions[j],
                strokeWidth = 0.75f,
            )
        }
        stars.forEachIndexed { i, s ->
            val twinkle = 0.5f + amplitude * sin(t * 2f * PI.toFloat() * s.twinkleFreq + s.phaseOffset)
            val base = TonalPalette.pick(paletteColors, s.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            drawCircle(
                color = TonalPalette.withAlpha(color, (0.25f + 0.65f * twinkle).coerceIn(0f, 1f) * brightness.coerceAtMost(1f) * dim),
                radius = s.radius,
                center = positions[i],
            )
        }

        // Shooting stars.
        val time = phase01(t)
        shootingStars.forEach { m ->
            val local = phase01(time + m.phaseOffset)
            if (local > m.duration) return@forEach
            val life = local / m.duration
            val travel = life * m.lengthFrac * 2.4f
            val x = (m.startX + cos(m.angle) * travel) * w
            val y = (m.startY + sin(m.angle) * travel) * h
            val trail = m.lengthFrac * minDim
            val start = Offset(x - cos(m.angle) * trail, y - sin(m.angle) * trail)
            val end = Offset(x, y)
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.12f).coerceIn(0f, 1f)
            val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, m.colorIndex), 0.3f)
            val color = TonalPalette.brightness(base, brightness)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(color, 0.8f * fade * dim),
                    ),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = m.thickness,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, 0.9f * fade * dim),
                radius = m.thickness * 1.3f,
                center = end,
            )
        }

        // Rotating planet with terminator + optional ring.
        val px = planet.xFrac * w
        val py = planet.yFrac * h
        val pr = planet.radiusFrac * minDim
        val planetTint = TonalPalette.brightness(
            TonalPalette.pick(paletteColors, planet.colorIndex),
            brightness,
        )
        val spin = phase01(planetT) * 360f
        rotate(degrees = spin, pivot = Offset(px, py)) {
            if (planet.ring) {
                drawOval(
                    color = TonalPalette.withAlpha(planetTint, 0.35f * dim),
                    topLeft = Offset(px - pr * 1.7f, py - pr * 0.28f),
                    size = androidx.compose.ui.geometry.Size(pr * 3.4f, pr * 0.56f),
                    style = Stroke(width = pr * 0.12f),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(TonalPalette.brightness(planetTint, 1.15f), 0.95f * dim),
                        TonalPalette.withAlpha(planetTint, 0.85f * dim),
                        TonalPalette.withAlpha(Color(0xFF1A1028), 0.9f * dim),
                    ),
                    center = Offset(px - pr * 0.25f, py - pr * 0.2f),
                    radius = pr * 1.15f,
                ),
                radius = pr,
                center = Offset(px, py),
            )
            // Soft terminator shadow on the trailing limb.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(Color.Black, 0.45f * dim),
                    ),
                    center = Offset(px + pr * 0.35f, py + pr * 0.1f),
                    radius = pr * 0.95f,
                ),
                radius = pr,
                center = Offset(px, py),
            )
        }
    }
}

private data class ConstellationStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val twinkleFreq: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

private data class ShootingStarSeed(
    val startX: Float,
    val startY: Float,
    val lengthFrac: Float,
    val angle: Float,
    val phaseOffset: Float,
    val duration: Float,
    val thickness: Float,
    val colorIndex: Int,
)

private data class ConstellationPlanetSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val ring: Boolean,
    val colorIndex: Int,
)
