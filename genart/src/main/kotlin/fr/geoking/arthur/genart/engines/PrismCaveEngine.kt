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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

/** Crystal facets with additive colored beams and dust motes. */
@Composable
internal fun PrismCaveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val facetCount = qualityCount(quality, low = 8, medium = 12, high = 18)
    val beamCount = qualityCount(quality, low = 3, medium = 4, high = 5)
    val dustCount = qualityCount(quality, low = 35, medium = 55, high = 85)
    val facets = remember(facetCount) {
        List(facetCount) { i ->
            PrismCaveFacet(
                x = seededRange(i * 17 + 3, 0.08f, 0.92f),
                y = seededRange(i * 23 + 7, 0.12f, 0.88f),
                rx = seededRange(i * 29 + 11, 0.05f, 0.12f),
                ry = seededRange(i * 31 + 13, 0.06f, 0.14f),
                skew = seededRange(i * 37 + 17, 0.1f, 0.35f),
                pulsePhase = seededRange(i * 41 + 19, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val beams = remember(beamCount) {
        List(beamCount) { i ->
            PrismCaveBeam(
                ox = seededRange(i * 19 + 5, 0.1f, 0.9f),
                oy = seededRange(i * 29 + 7, -0.05f, 0.35f),
                angle = seededRange(i * 37 + 11, 0.4f, PI.toFloat() - 0.4f),
                halfWidth = seededRange(i * 43 + 13, 0.05f, 0.1f),
                swayAmp = seededRange(i * 47 + 17, 0.015f, 0.04f),
                swayFreq = seededRange(i * 53 + 19, 0.2f, 0.5f),
                alpha = seededRange(i * 59 + 23, 0.14f, 0.28f),
                colorIndex = i,
            )
        }
    }
    val dust = remember(dustCount) {
        List(dustCount) { j ->
            PrismCaveDust(
                x0 = seededUnit(j * 13 + 3),
                y0 = seededUnit(j * 17 + 5),
                sizeFrac = seededRange(j * 19 + 7, 0.0015f, 0.0045f),
                phase = seededRange(j * 23 + 11, 0f, 2f * PI.toFloat()),
                freq = seededRange(j * 29 + 13, 0.2f, 0.7f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "prism_cave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "prism_cave_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF14101C), Color(0xFF07060C), Color(0xFF020208)),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = minDim * 1.1f,
            ),
        )

        beams.forEach { beam ->
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val origin = Offset(beam.ox * w, beam.oy * h)
            val reach = h * 1.2f
            val left = Offset(origin.x + sin(a - beam.halfWidth) * reach, origin.y + cos(a - beam.halfWidth) * reach)
            val right = Offset(origin.x + sin(a + beam.halfWidth) * reach, origin.y + cos(a + beam.halfWidth) * reach)
            val tint = TonalPalette.brightness(TonalPalette.pick(paletteColors, beam.colorIndex), brightness)
            val path = Path().apply {
                moveTo(origin.x, origin.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, beam.alpha * dim),
                        TonalPalette.withAlpha(tint, beam.alpha * 0.2f * dim),
                        Color.Transparent,
                    ),
                    start = origin,
                    end = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f),
                ),
                blendMode = BlendMode.Plus,
            )
        }

        facets.forEach { facet ->
            val pulse = 1f + 0.04f * (sin01(time + facet.pulsePhase) * 2f - 1f)
            val cx = facet.x * w
            val cy = facet.y * h
            val rx = facet.rx * minDim * pulse
            val ry = facet.ry * minDim * pulse
            val base = TonalPalette.brightness(TonalPalette.pick(paletteColors, facet.colorIndex), brightness)
            val light = TonalPalette.mix(base, Color.White, 0.35f)
            val dark = TonalPalette.mix(base, Color(0xFF101018), 0.45f)
            val top = Path().apply {
                moveTo(cx, cy - ry)
                lineTo(cx + rx, cy)
                lineTo(cx + rx * facet.skew, cy)
                lineTo(cx, cy - ry * 0.15f)
                close()
            }
            val bottom = Path().apply {
                moveTo(cx, cy + ry)
                lineTo(cx - rx, cy)
                lineTo(cx + rx, cy)
                close()
            }
            drawPath(top, color = TonalPalette.withAlpha(light, 0.55f * dim))
            drawPath(bottom, color = TonalPalette.withAlpha(dark, 0.5f * dim))
            val mid = Path().apply {
                moveTo(cx - rx, cy)
                lineTo(cx, cy - ry)
                lineTo(cx + rx, cy)
                lineTo(cx, cy + ry)
                close()
            }
            drawPath(
                mid,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(light, 0.4f * dim),
                        TonalPalette.withAlpha(dark, 0.45f * dim),
                    ),
                    start = Offset(cx - rx, cy - ry),
                    end = Offset(cx + rx, cy + ry),
                ),
            )
        }

        val dustTint = TonalPalette.brightness(Color(0xFFE8F0FF), brightness)
        dust.forEach { d ->
            val x = phase01(d.x0 + 0.02f * sin(time * d.freq + d.phase)) * w
            val y = phase01(d.y0 + 0.015f * cos(time * d.freq * 0.8f + d.phase)) * h
            drawCircle(
                color = TonalPalette.withAlpha(dustTint, 0.3f * dim),
                radius = d.sizeFrac * minDim,
                center = Offset(x, y),
            )
        }
    }
}

private data class PrismCaveFacet(
    val x: Float,
    val y: Float,
    val rx: Float,
    val ry: Float,
    val skew: Float,
    val pulsePhase: Float,
    val colorIndex: Int,
)

private data class PrismCaveBeam(
    val ox: Float,
    val oy: Float,
    val angle: Float,
    val halfWidth: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alpha: Float,
    val colorIndex: Int,
)

private data class PrismCaveDust(
    val x0: Float,
    val y0: Float,
    val sizeFrac: Float,
    val phase: Float,
    val freq: Float,
)
