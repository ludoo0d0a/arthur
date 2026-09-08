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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A glowing sun with 3-5 small planets sliding along flattened elliptical orbit lines, over the
 * [StarFieldEngine] backdrop. Each planet completes a whole number of revolutions per loop
 * ([PlanetSeed.revolutionsPerLoop], decreasing with orbit radius) — a cheap stand-in for Kepler's
 * third law (inner planets orbit faster) that also keeps the animation loop seamless, since every
 * planet is back at its starting angle exactly when the shared clock wraps.
 */
@Composable
internal fun SolarSystemEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val planetCount = qualityCount(quality, low = 3, medium = 4, high = 5)
    val revolutionPool = floatArrayOf(11f, 7f, 5f, 3f, 2f)
    val planets = remember(planetCount) {
        List(planetCount) { i ->
            PlanetSeed(
                orbitRadiusFrac = 0.16f + i * 0.085f,
                orbitFlatten = seededRange(i * 131 + 7, 0.32f, 0.42f),
                revolutionsPerLoop = revolutionPool[i.coerceIn(0, revolutionPool.lastIndex)],
                initialPhase = seededUnit(i * 149 + 11),
                planetRadiusFrac = seededRange(i * 163 + 13, 0.028f, 0.05f),
                colorIndex = i,
                hasRing = planetCount >= 2 && i == 1,
                cloudBandCount = 2 + (seededUnit(i * 173 + 17) * 2f).toInt(),
                cloudSeedBase = i * 733 + 4001,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "solarsystem")
    val orbitT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "solarsystem_orbit",
    )
    val cloudT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "solarsystem_clouds",
    )
    val sunPulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "solarsystem_sunpulse",
    )

    val dim = if (isActive) 1f else 0.6f

    Box(modifier = modifier) {
        StarFieldEngine(isActive, paletteColors, quality, brightness, speed, Modifier.fillMaxSize())

        // Crisp layer: orbit lines, sun core, planet body shading (lit hemisphere + terminator), rings.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.52f

            planets.forEach { p ->
                val a = p.orbitRadiusFrac * minDim
                val b = a * p.orbitFlatten
                drawOval(
                    color = Color.White.copy(alpha = 0.06f * dim),
                    topLeft = Offset(cx - a, cy - b),
                    size = Size(a * 2f, b * 2f),
                    style = Stroke(width = 1f),
                )
            }

            val sunRadius = minDim * 0.05f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF3D6), Color(0xFFFFC978)),
                    center = Offset(cx, cy),
                    radius = sunRadius,
                ),
                radius = sunRadius,
                center = Offset(cx, cy),
            )

            planets.forEach { p ->
                val a = p.orbitRadiusFrac * minDim
                val b = a * p.orbitFlatten
                val angle = 2f * PI.toFloat() * (orbitT * p.revolutionsPerLoop + p.initialPhase)
                val px = cx + cos(angle) * a
                val py = cy + sin(angle) * b
                val radius = p.planetRadiusFrac * minDim
                val tint = TonalPalette.brightness(TonalPalette.pick(paletteColors, p.colorIndex), brightness)

                if (p.hasRing) {
                    drawOval(
                        color = TonalPalette.withAlpha(tint, 0.35f * dim),
                        topLeft = Offset(px - radius * 2.1f, py - radius * 0.55f),
                        size = Size(radius * 4.2f, radius * 1.1f),
                        style = Stroke(width = radius * 0.22f),
                    )
                }

                drawCircle(color = Color(0xFF10131A), radius = radius, center = Offset(px, py))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TonalPalette.withAlpha(tint, 0.95f), Color.Transparent),
                        center = Offset(px - radius * 0.4f, py - radius * 0.4f),
                        radius = radius * 1.3f,
                    ),
                    radius = radius,
                    center = Offset(px, py),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF03040A).copy(alpha = 0.85f)),
                        center = Offset(px + radius * 0.5f, py + radius * 0.5f),
                        radius = radius * 1.4f,
                    ),
                    radius = radius,
                    center = Offset(px, py),
                )
            }
        }

        // Blurred layer: sun corona, atmosphere rims, cloud bands (clipped to each planet disc).
        Canvas(modifier = Modifier.fillMaxSize().blur(16.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val cx = w * 0.5f
            val cy = h * 0.52f
            val sunRadius = minDim * 0.05f
            val pulse = 0.85f + 0.15f * sin01(phase01(sunPulseT) * 2f * PI.toFloat())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFECC2).copy(alpha = 0.55f * dim * pulse), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = sunRadius * 3.2f,
                ),
                radius = sunRadius * 3.2f,
                center = Offset(cx, cy),
            )

            val cloudDrift = phase01(cloudT)
            planets.forEach { p ->
                val a = p.orbitRadiusFrac * minDim
                val b = a * p.orbitFlatten
                val angle = 2f * PI.toFloat() * (orbitT * p.revolutionsPerLoop + p.initialPhase)
                val px = cx + cos(angle) * a
                val py = cy + sin(angle) * b
                val radius = p.planetRadiusFrac * minDim
                val tint = TonalPalette.brightness(TonalPalette.pick(paletteColors, p.colorIndex), brightness)

                drawCircle(
                    color = TonalPalette.withAlpha(tint, 0.3f * dim),
                    radius = radius * 1.18f,
                    center = Offset(px, py),
                    style = Stroke(width = radius * 0.2f),
                )

                clipPath(Path().apply { addOval(Rect(Offset(px - radius, py - radius), Size(radius * 2f, radius * 2f))) }) {
                    for (band in 0 until p.cloudBandCount) {
                        val bandSeed = p.cloudSeedBase + band * 61
                        val bandY = py + seededRange(bandSeed, -radius * 0.6f, radius * 0.6f)
                        val bandDrift = phase01(cloudDrift + seededUnit(bandSeed))
                        val bandX = px + (bandDrift - 0.5f) * radius * 2.6f
                        drawOval(
                            color = TonalPalette.withAlpha(Color.White, 0.18f * dim),
                            topLeft = Offset(bandX - radius, bandY - radius * 0.12f),
                            size = Size(radius * 2f, radius * 0.24f),
                        )
                    }
                }
            }
        }
    }
}

private data class PlanetSeed(
    val orbitRadiusFrac: Float,
    val orbitFlatten: Float,
    val revolutionsPerLoop: Float,
    val initialPhase: Float,
    val planetRadiusFrac: Float,
    val colorIndex: Int,
    val hasRing: Boolean,
    val cloudBandCount: Int,
    val cloudSeedBase: Int,
)
