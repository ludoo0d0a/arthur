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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.genartSoftBlur
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hubble-palette emission nebula inspired by IC 410 (Tadpole Nebula):
 * teal OIII / orange Ha / magenta SII clouds, elongated tadpole pillars,
 * dense starfield + open cluster, optional foreground planets.
 * Positions, sizes and hues drift seamlessly via [loopedFbm].
 */
@Composable
internal fun TadpoleNebulaEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 10, medium = 16, high = 24)
    val clouds = remember(cloudCount) {
        List(cloudCount) { i ->
            EmissionCloud(
                x0 = seededRange(i * 17 + 3, 0.08f, 0.92f),
                y0 = seededRange(i * 29 + 7, 0.08f, 0.92f),
                radiusFrac = seededRange(i * 41 + 11, 0.18f, 0.48f),
                driftAmp = seededRange(i * 53 + 13, 0.02f, 0.07f),
                pulseFreq = seededRange(i * 67 + 19, 0.6f, 1.4f),
                alphaBase = seededRange(i * 79 + 23, 0.10f, 0.28f),
                toneKind = i % 3, // 0 OIII teal, 1 Ha orange, 2 SII magenta
                colorIndex = i,
            )
        }
    }
    val tadpoleCount = qualityCount(quality, low = 2, medium = 2, high = 3)
    val tadpoles = remember(tadpoleCount) {
        List(tadpoleCount) { i ->
            TadpolePillar(
                headX = seededRange(i * 101 + 5, 0.28f, 0.62f),
                headY = seededRange(i * 103 + 11, 0.32f, 0.68f),
                headR = seededRange(i * 107 + 17, 0.055f, 0.095f),
                angle = seededRange(i * 109 + 23, 0.15f, 1.15f) + if (i % 2 == 0) 0f else PI.toFloat() * 0.85f,
                lengthFrac = seededRange(i * 113 + 29, 0.18f, 0.32f),
                segments = seededRange(i * 127 + 31, 5f, 9f).toInt(),
                toneKind = (i + 1) % 3,
                colorIndex = i + 4,
            )
        }
    }
    val dustCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val dust = remember(dustCount) {
        List(dustCount) { i ->
            DustLane(
                x0 = seededRange(i * 131 + 3, 0.15f, 0.85f),
                y0 = seededRange(i * 137 + 7, 0.2f, 0.8f),
                radiusFrac = seededRange(i * 139 + 11, 0.08f, 0.22f),
                alpha = seededRange(i * 149 + 13, 0.18f, 0.42f),
                driftAmp = seededRange(i * 151 + 17, 0.01f, 0.04f),
            )
        }
    }
    val starCount = qualityCount(quality, low = 90, medium = 150, high = 220)
    val stars = remember(starCount) {
        List(starCount) { j ->
            NebulaStar(
                x = seededUnit(j * 11 + 5),
                y = seededUnit(j * 23 + 9),
                size = seededRange(j * 37 + 13, 0.0007f, 0.0035f),
                alpha = seededRange(j * 43 + 17, 0.18f, 0.9f),
                twinkleFreq = seededRange(j * 47 + 19, 0.3f, 1.8f),
                phase = seededRange(j * 53 + 23, 0f, 2f * PI.toFloat()),
                colorBias = seededUnit(j * 59 + 29),
                hasSpike = j % 17 == 0,
            )
        }
    }
    val clusterCount = qualityCount(quality, low = 10, medium = 16, high = 24)
    val cluster = remember(clusterCount) {
        List(clusterCount) { i ->
            ClusterStar(
                angle = seededRange(i * 61 + 3, 0f, 2f * PI.toFloat()),
                radiusFrac = seededRange(i * 67 + 7, 0.02f, 0.14f),
                size = seededRange(i * 71 + 11, 0.0016f, 0.0045f),
                alpha = seededRange(i * 73 + 13, 0.45f, 0.95f),
            )
        }
    }
    val planetCount = qualityCount(quality, low = 1, medium = 2, high = 2)
    val planets = remember(planetCount) {
        List(planetCount) { i ->
            ForegroundPlanet(
                x0 = seededRange(i * 201 + 3, 0.12f, 0.88f),
                y0 = seededRange(i * 211 + 7, 0.55f, 0.88f),
                radiusFrac = seededRange(i * 223 + 11, 0.035f, 0.08f),
                ring = seededUnit(i * 227 + 13) > 0.4f,
                colorIndex = i + 2,
                driftAmp = seededRange(i * 233 + 17, 0.015f, 0.04f),
                spinSpeed = seededRange(i * 239 + 19, 0.4f, 1.1f),
            )
        }
    }

    val effectiveSpeed = (if (isActive) speed else speed * 0.45f).coerceAtLeast(0.15f)
    val transition = rememberInfiniteTransition(label = "tadpole_nebula")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tadpole_nebula_t",
    )
    val colorT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tadpole_nebula_color",
    )
    val planetT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tadpole_nebula_planet",
    )

    val dim = if (isActive) 1f else 0.55f

    Box(modifier = modifier) {
        // Crisp starfield + planets (no blur)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            val cTime = phase01(colorT)

            drawRect(color = Color(0xFF02010A))
            // Soft central ionization glow
            val clusterCx = w * (0.48f + 0.03f * (loopedFbm(time, 1.1f, seedOffset = 3) * 2f - 1f))
            val clusterCy = h * (0.42f + 0.03f * (loopedFbm(time, 1.2f, seedOffset = 11) * 2f - 1f))
            val hubTeal = cosmicTone(0, paletteColors, brightness, cTime)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(hubTeal, 0.22f * dim),
                        TonalPalette.withAlpha(hubTeal, 0.06f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(clusterCx, clusterCy),
                    radius = minDim * 0.55f,
                ),
                radius = minDim * 0.55f,
                center = Offset(clusterCx, clusterCy),
            )

            stars.forEach { s ->
                val n = loopedFbm(time, radius = 1.1f + s.twinkleFreq * 0.2f, seedOffset = (s.x * 997).toInt())
                val tw = 0.55f + 0.45f * sin(time * 2f * PI.toFloat() * s.twinkleFreq + s.phase)
                val dx = (n * 2f - 1f) * 0.012f
                val dy = (loopedFbm(time, 1.3f, seedOffset = (s.y * 991).toInt()) * 2f - 1f) * 0.012f
                val x = ((s.x + dx + 1f) % 1f) * w
                val y = ((s.y + dy + 1f) % 1f) * h
                val starTint = when {
                    s.colorBias < 0.25f -> Color(0xFF7DD3FC) // cyan
                    s.colorBias < 0.45f -> Color(0xFFF9A8D4) // pink
                    s.colorBias < 0.65f -> Color(0xFFFDBA74) // warm
                    else -> Color.White
                }
                val tint = TonalPalette.brightness(
                    TonalPalette.mix(starTint, TonalPalette.pick(paletteColors, (s.colorBias * 8).toInt()), 0.25f),
                    brightness,
                )
                val r = (s.size * minDim * (0.85f + 0.25f * tw)).coerceAtLeast(0.7f)
                val a = (s.alpha * tw * dim).coerceIn(0f, 1f)
                if (s.hasSpike) {
                    val spike = r * (4.5f + 2f * tw)
                    drawLine(
                        color = TonalPalette.withAlpha(tint, a * 0.55f),
                        start = Offset(x - spike, y),
                        end = Offset(x + spike, y),
                        strokeWidth = (r * 0.45f).coerceAtLeast(0.8f),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = TonalPalette.withAlpha(tint, a * 0.55f),
                        start = Offset(x, y - spike),
                        end = Offset(x, y + spike),
                        strokeWidth = (r * 0.45f).coerceAtLeast(0.8f),
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    color = TonalPalette.withAlpha(tint, a),
                    radius = r,
                    center = Offset(x, y),
                )
            }

            // Open cluster around ionization center
            cluster.forEach { c ->
                val breath = 0.9f + 0.1f * loopedFbm(time, 1.4f, seedOffset = (c.angle * 100).toInt())
                val rr = c.radiusFrac * minDim * breath
                val x = clusterCx + cos(c.angle + time * 0.15f) * rr
                val y = clusterCy + sin(c.angle + time * 0.15f) * rr
                val tint = TonalPalette.brightness(
                    TonalPalette.mix(Color(0xFFE0F2FE), TonalPalette.pick(paletteColors, 1), 0.2f),
                    brightness,
                )
                val core = (c.size * minDim).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, c.alpha * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = core * 3.2f,
                    ),
                    radius = core * 3.2f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(Color.White, c.alpha * dim),
                    radius = core,
                    center = Offset(x, y),
                )
            }

            // Foreground planets
            val pTime = phase01(planetT)
            planets.forEach { p ->
                val nx = loopedFbm(pTime, 1.2f + p.spinSpeed, seedOffset = p.colorIndex * 41 + 7)
                val ny = loopedFbm(pTime, 1.35f + p.spinSpeed, seedOffset = p.colorIndex * 43 + 11)
                val breath = 0.88f + 0.18f * loopedFbm(pTime, 1.5f, seedOffset = p.colorIndex * 47 + 13)
                val px = ((p.x0 + (nx * 2f - 1f) * p.driftAmp + 1f) % 1f) * w
                val py = ((p.y0 + (ny * 2f - 1f) * p.driftAmp + 1f) % 1f) * h
                val pr = p.radiusFrac * minDim * breath
                val base = TonalPalette.brightness(TonalPalette.pick(paletteColors, p.colorIndex), brightness)
                val spin = pTime * 360f * p.spinSpeed
                rotate(degrees = spin, pivot = Offset(px, py)) {
                    if (p.ring) {
                        drawOval(
                            color = TonalPalette.withAlpha(base, 0.4f * dim),
                            topLeft = Offset(px - pr * 1.85f, py - pr * 0.28f),
                            size = Size(pr * 3.7f, pr * 0.56f),
                            style = Stroke(width = pr * 0.11f),
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TonalPalette.withAlpha(TonalPalette.brightness(base, 1.2f), 0.95f * dim),
                                TonalPalette.withAlpha(base, 0.85f * dim),
                                TonalPalette.withAlpha(Color(0xFF12081C), 0.92f * dim),
                            ),
                            center = Offset(px - pr * 0.28f, py - pr * 0.22f),
                            radius = pr * 1.2f,
                        ),
                        radius = pr,
                        center = Offset(px, py),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color.Black, 0.5f * dim)),
                            center = Offset(px + pr * 0.38f, py + pr * 0.12f),
                            radius = pr * 0.95f,
                        ),
                        radius = pr,
                        center = Offset(px, py),
                    )
                }
            }
        }

        // Soft emission gas + tadpoles + dust (blurred)
        Canvas(modifier = Modifier.fillMaxSize().genartSoftBlur(quality, 28.dp)) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            val cTime = phase01(colorT)

            clouds.forEach { cloud ->
                val n = loopedFbm(time, radius = cloud.pulseFreq, seedOffset = cloud.colorIndex * 619 + 71)
                val nx = loopedFbm(time, 1.15f, seedOffset = cloud.colorIndex * 701 + 3)
                val ny = loopedFbm(time, 1.25f, seedOffset = cloud.colorIndex * 709 + 5)
                val x = ((cloud.x0 + (nx * 2f - 1f) * cloud.driftAmp + 1f) % 1f) * w
                val y = ((cloud.y0 + (ny * 2f - 1f) * cloud.driftAmp + 1f) % 1f) * h
                val pulse = 0.82f + 0.28f * n
                val radius = cloud.radiusFrac * minDim * pulse
                val tint = cosmicTone(cloud.toneKind, paletteColors, brightness, cTime, cloud.colorIndex)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, cloud.alphaBase * dim),
                            TonalPalette.withAlpha(tint, cloud.alphaBase * 0.45f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
            }

            // Tadpole pillars: bright head + trailing chain of soft blobs
            tadpoles.forEach { tp ->
                val sway = (loopedFbm(time, 1.3f, seedOffset = tp.colorIndex * 811 + 9) * 2f - 1f) * 0.04f
                val angle = tp.angle + sway
                val headBreath = 0.9f + 0.15f * loopedFbm(time, 1.4f, seedOffset = tp.colorIndex * 823)
                val hx = ((tp.headX + sway * 0.3f + 1f) % 1f) * w
                val hy = ((tp.headY - sway * 0.2f + 1f) % 1f) * h
                val hr = tp.headR * minDim * headBreath
                val headTint = cosmicTone(tp.toneKind, paletteColors, brightness * 1.08f, cTime, tp.colorIndex)
                // Ionized rim on the head
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(TonalPalette.brightness(headTint, 1.25f), 0.55f * dim),
                            TonalPalette.withAlpha(headTint, 0.22f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(hx, hy),
                        radius = hr * 1.55f,
                    ),
                    radius = hr * 1.55f,
                    center = Offset(hx, hy),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color(0xFFF8FAFC), 0.35f * dim),
                            TonalPalette.withAlpha(headTint, 0.4f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(hx - hr * 0.15f, hy - hr * 0.1f),
                        radius = hr,
                    ),
                    radius = hr,
                    center = Offset(hx, hy),
                )
                val tailLen = tp.lengthFrac * minDim
                for (s in 1..tp.segments) {
                    val u = s / tp.segments.toFloat()
                    val falloff = 1f - u * 0.85f
                    val wobble = sin(time * 2f * PI.toFloat() + s * 0.7f + tp.colorIndex) * hr * 0.18f
                    val tx = hx + cos(angle) * tailLen * u + cos(angle + PI.toFloat() / 2f) * wobble
                    val ty = hy + sin(angle) * tailLen * u + sin(angle + PI.toFloat() / 2f) * wobble
                    val tr = hr * (0.85f - u * 0.55f) * falloff
                    val bodyTint = cosmicTone((tp.toneKind + s) % 3, paletteColors, brightness, cTime, tp.colorIndex + s)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TonalPalette.withAlpha(bodyTint, 0.32f * falloff * dim),
                                TonalPalette.withAlpha(bodyTint, 0.1f * falloff * dim),
                                Color.Transparent,
                            ),
                            center = Offset(tx, ty),
                            radius = tr * 1.6f,
                        ),
                        radius = tr * 1.6f,
                        center = Offset(tx, ty),
                    )
                }
            }

            dust.forEach { d ->
                val n = loopedFbm(time, 1.2f, seedOffset = (d.x0 * 500).toInt() + 17)
                val x = ((d.x0 + (n * 2f - 1f) * d.driftAmp + 1f) % 1f) * w
                val y = ((d.y0 + (loopedFbm(time, 1.3f, seedOffset = 91) * 2f - 1f) * d.driftAmp + 1f) % 1f) * h
                val r = d.radiusFrac * minDim * (0.9f + 0.2f * n)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color(0xFF05030C), d.alpha * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(x, y),
                )
            }
        }
    }
}

/** Hubble SHO tones mixed with the user palette; [toneKind] cycles teal / orange / magenta. */
private fun cosmicTone(
    toneKind: Int,
    paletteColors: List<Color>,
    brightness: Float,
    colorPhase: Float,
    index: Int = 0,
): Color {
    val sho = when (toneKind % 3) {
        0 -> Color(0xFF22D3EE) // OIII teal
        1 -> Color(0xFFFB923C) // Ha orange
        else -> Color(0xFFE879F9) // SII magenta
    }
    val alt = when ((toneKind + 1) % 3) {
        0 -> Color(0xFF38BDF8)
        1 -> Color(0xFFF97316)
        else -> Color(0xFFD946EF)
    }
    val cycle = loopedFbm(colorPhase, radius = 1.2f + (index % 5) * 0.07f, seedOffset = index * 37 + 5)
    val cosmic = TonalPalette.mix(sho, alt, cycle)
    val palette = TonalPalette.pick(paletteColors, index)
    return TonalPalette.brightness(TonalPalette.mix(cosmic, palette, 0.28f), brightness)
}

private data class EmissionCloud(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val driftAmp: Float,
    val pulseFreq: Float,
    val alphaBase: Float,
    val toneKind: Int,
    val colorIndex: Int,
)

private data class TadpolePillar(
    val headX: Float,
    val headY: Float,
    val headR: Float,
    val angle: Float,
    val lengthFrac: Float,
    val segments: Int,
    val toneKind: Int,
    val colorIndex: Int,
)

private data class DustLane(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val alpha: Float,
    val driftAmp: Float,
)

private data class NebulaStar(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleFreq: Float,
    val phase: Float,
    val colorBias: Float,
    val hasSpike: Boolean,
)

private data class ClusterStar(
    val angle: Float,
    val radiusFrac: Float,
    val size: Float,
    val alpha: Float,
)

private data class ForegroundPlanet(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val ring: Boolean,
    val colorIndex: Int,
    val driftAmp: Float,
    val spinSpeed: Float,
)
