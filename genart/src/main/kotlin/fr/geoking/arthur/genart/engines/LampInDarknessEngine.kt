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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single lamp (or small spot cluster) in deep darkness — pendant, floor, desk, ceiling
 * spots, or wall sconce — chosen at random, with a volumetric cone and dust motes.
 */
@Composable
internal fun LampInDarknessEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lampKind = remember { (seededUnit(11) * LampKind.entries.size).toInt().coerceIn(0, LampKind.entries.lastIndex) }
    val kind = LampKind.entries[lampKind]
    val dustCount = qualityCount(quality, low = 55, medium = 90, high = 140)
    val beamCount = when (kind) {
        LampKind.CeilingSpots -> qualityCount(quality, low = 2, medium = 3, high = 4)
        else -> 1
    }
    val beams = remember(kind, beamCount) { buildLampBeams(kind, beamCount) }
    val dust = remember(dustCount, beamCount) {
        List(dustCount) { j ->
            LampDust(
                beamIndex = (seededUnit(j * 17 + 3) * beamCount).toInt().coerceIn(0, beamCount - 1),
                along = seededRange(j * 19 + 7, 0.08f, 0.96f),
                side = seededRange(j * 23 + 11, -0.9f, 0.9f),
                sizeFrac = seededRange(j * 29 + 13, 0.0012f, 0.005f),
                twinklePhase = seededRange(j * 31 + 17, 0f, 2f * PI.toFloat()),
                twinkleFreq = seededRange(j * 37 + 19, 0.5f, 2.0f),
                star = seededUnit(j * 41 + 23) > 0.92f,
            )
        }
    }
    val warmBias = remember { seededRange(47, 0.55f, 0.85f) }
    val transition = rememberInfiniteTransition(label = "lamp_dark")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lamp_dark_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF14160E), Color(0xFF070806), Color(0xFF010201)),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = minDim * 1.15f,
            ),
        )
        // Soft fog wisps in the dark
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1C2214).copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.72f, h * 0.55f),
                radius = minDim * 0.45f,
            ),
            radius = minDim * 0.45f,
            center = Offset(w * 0.72f, h * 0.55f),
        )

        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val paletteTint = TonalPalette.pick(paletteColors, 0)
        val beamColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFF5D66A), paletteTint, 1f - warmBias),
            brightness,
        )
        val fixtureColor = Color(0xFF121210)

        beams.forEach { beam ->
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val origin = Offset(beam.ox * w, beam.oy * h)
            val reach = beam.reach * h
            val left = Offset(
                origin.x + sin(a - beam.halfWidth) * reach,
                origin.y + cos(a - beam.halfWidth) * reach,
            )
            val right = Offset(
                origin.x + sin(a + beam.halfWidth) * reach,
                origin.y + cos(a + beam.halfWidth) * reach,
            )
            val mid = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f)
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
                        TonalPalette.withAlpha(beamColor, 0.42f * dim),
                        TonalPalette.withAlpha(beamColor, 0.18f * dim),
                        Color.Transparent,
                    ),
                    start = origin,
                    end = mid,
                ),
            )
            // Soft radial rays inside the cone
            val rayCount = qualityCount(quality, low = 5, medium = 8, high = 12)
            for (r in 0 until rayCount) {
                val u = (r + 0.5f) / rayCount
                val rayA = a + lerp(-beam.halfWidth, beam.halfWidth, u) * 0.92f
                val tip = Offset(
                    origin.x + sin(rayA) * reach,
                    origin.y + cos(rayA) * reach,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(beamColor, 0.12f * dim),
                            Color.Transparent,
                        ),
                        start = origin,
                        end = tip,
                    ),
                    start = origin,
                    end = tip,
                    strokeWidth = minDim * 0.004f,
                )
            }
            val poolR = minDim * (0.14f + beam.halfWidth * 0.7f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.55f * dim),
                        TonalPalette.withAlpha(beamColor, 0.35f * dim),
                        Color.Transparent,
                    ),
                    center = mid,
                    radius = poolR,
                ),
                radius = poolR,
                center = mid,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.7f * dim),
                        Color.Transparent,
                    ),
                    center = origin,
                    radius = minDim * 0.045f,
                ),
                radius = minDim * 0.045f,
                center = origin,
            )
            drawLampFixture(kind, beam, origin, fixtureColor, minDim, h)
        }

        dust.forEach { mote ->
            val beam = beams[mote.beamIndex]
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val origin = Offset(beam.ox * w, beam.oy * h)
            val reach = beam.reach * h
            val along = mote.along
            val sideA = a + beam.halfWidth * along * mote.side
            val px = origin.x + sin(sideA) * reach * along
            val py = origin.y + cos(sideA) * reach * along
            val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(time * mote.twinkleFreq + mote.twinklePhase))
            val r = mote.sizeFrac * minDim * (0.7f + 0.6f * twinkle)
            val moteColor = TonalPalette.withAlpha(
                TonalPalette.mix(beamColor, Color.White, 0.65f),
                0.6f * twinkle * dim,
            )
            drawCircle(
                color = moteColor,
                radius = r.coerceAtLeast(0.5f),
                center = Offset(px, py),
            )
            if (mote.star) {
                val arm = r * 3.2f
                drawLine(
                    color = moteColor,
                    start = Offset(px - arm, py),
                    end = Offset(px + arm, py),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = moteColor,
                    start = Offset(px, py - arm),
                    end = Offset(px, py + arm),
                    strokeWidth = 1f,
                )
            }
        }
    }
}

private enum class LampKind { Pendant, Floor, Desk, CeilingSpots, WallSconce }

private data class LampBeam(
    val ox: Float,
    val oy: Float,
    val angle: Float,
    val halfWidth: Float,
    val reach: Float,
    val swayAmp: Float,
    val swayFreq: Float,
)

private data class LampDust(
    val beamIndex: Int,
    val along: Float,
    val side: Float,
    val sizeFrac: Float,
    val twinklePhase: Float,
    val twinkleFreq: Float,
    val star: Boolean,
)

private fun buildLampBeams(kind: LampKind, count: Int): List<LampBeam> = when (kind) {
    LampKind.Pendant -> listOf(
        LampBeam(
            ox = 0.42f + seededUnit(3) * 0.16f,
            oy = 0.06f + seededUnit(5) * 0.08f,
            angle = 0.0f + seededRange(7, -0.12f, 0.12f),
            halfWidth = seededRange(9, 0.14f, 0.22f),
            reach = seededRange(11, 0.95f, 1.2f),
            swayAmp = seededRange(13, 0.01f, 0.03f),
            swayFreq = seededRange(15, 0.15f, 0.4f),
        ),
    )
    LampKind.Floor -> listOf(
        LampBeam(
            ox = 0.28f + seededUnit(3) * 0.2f,
            oy = 0.55f + seededUnit(5) * 0.12f,
            angle = PI.toFloat() + seededRange(7, -0.35f, 0.35f),
            halfWidth = seededRange(9, 0.16f, 0.26f),
            reach = seededRange(11, 0.7f, 1.0f),
            swayAmp = seededRange(13, 0.01f, 0.025f),
            swayFreq = seededRange(15, 0.12f, 0.35f),
        ),
    )
    LampKind.Desk -> listOf(
        LampBeam(
            ox = 0.18f + seededUnit(3) * 0.2f,
            oy = 0.28f + seededUnit(5) * 0.15f,
            angle = 0.55f + seededRange(7, -0.2f, 0.35f),
            halfWidth = seededRange(9, 0.12f, 0.2f),
            reach = seededRange(11, 0.75f, 1.05f),
            swayAmp = seededRange(13, 0.015f, 0.04f),
            swayFreq = seededRange(15, 0.2f, 0.45f),
        ),
    )
    LampKind.CeilingSpots -> List(count) { i ->
        LampBeam(
            ox = 0.18f + i * (0.64f / (count - 1).coerceAtLeast(1)) + seededRange(i * 17 + 3, -0.04f, 0.04f),
            oy = 0.04f + seededRange(i * 19 + 5, 0f, 0.06f),
            angle = seededRange(i * 23 + 7, -0.2f, 0.2f),
            halfWidth = seededRange(i * 29 + 9, 0.08f, 0.14f),
            reach = seededRange(i * 31 + 11, 0.85f, 1.15f),
            swayAmp = seededRange(i * 37 + 13, 0.008f, 0.025f),
            swayFreq = seededRange(i * 41 + 15, 0.15f, 0.4f),
        )
    }
    LampKind.WallSconce -> listOf(
        LampBeam(
            ox = if (seededUnit(3) > 0.5f) 0.08f else 0.92f,
            oy = 0.32f + seededUnit(5) * 0.2f,
            angle = if (seededUnit(3) > 0.5f) {
                PI.toFloat() * 0.5f + seededRange(7, -0.25f, 0.25f)
            } else {
                -PI.toFloat() * 0.5f + seededRange(7, -0.25f, 0.25f)
            },
            halfWidth = seededRange(9, 0.18f, 0.28f),
            reach = seededRange(11, 0.7f, 1.05f),
            swayAmp = seededRange(13, 0.01f, 0.03f),
            swayFreq = seededRange(15, 0.15f, 0.4f),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLampFixture(
    kind: LampKind,
    beam: LampBeam,
    origin: Offset,
    color: Color,
    minDim: Float,
    h: Float,
) {
    when (kind) {
        LampKind.Pendant -> {
            drawLine(
                color = color,
                start = Offset(origin.x, 0f),
                end = origin,
                strokeWidth = minDim * 0.006f,
                cap = StrokeCap.Round,
            )
            val shadeR = minDim * 0.045f
            drawCircle(color = color, radius = shadeR, center = origin)
            drawArc(
                color = color,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = true,
                topLeft = Offset(origin.x - shadeR * 1.35f, origin.y - shadeR * 0.2f),
                size = androidx.compose.ui.geometry.Size(shadeR * 2.7f, shadeR * 1.6f),
            )
        }
        LampKind.Floor -> {
            val baseY = h * 0.92f
            drawLine(
                color = color,
                start = Offset(origin.x, origin.y),
                end = Offset(origin.x, baseY),
                strokeWidth = minDim * 0.012f,
                cap = StrokeCap.Round,
            )
            drawCircle(color = color, radius = minDim * 0.035f, center = Offset(origin.x, baseY))
            drawCircle(color = color, radius = minDim * 0.05f, center = origin)
        }
        LampKind.Desk -> {
            val joint = Offset(origin.x - minDim * 0.08f, origin.y + minDim * 0.06f)
            val base = Offset(joint.x - minDim * 0.04f, h * 0.78f)
            drawLine(color = color, start = origin, end = joint, strokeWidth = minDim * 0.01f, cap = StrokeCap.Round)
            drawLine(color = color, start = joint, end = base, strokeWidth = minDim * 0.012f, cap = StrokeCap.Round)
            drawCircle(color = color, radius = minDim * 0.04f, center = origin)
            drawCircle(color = color, radius = minDim * 0.028f, center = base)
        }
        LampKind.CeilingSpots -> {
            drawCircle(color = color, radius = minDim * 0.028f, center = origin)
            drawCircle(
                color = Color(0xFF2A2A28),
                radius = minDim * 0.018f,
                center = origin,
                style = Stroke(width = minDim * 0.006f),
            )
        }
        LampKind.WallSconce -> {
            val inward = if (beam.ox < 0.5f) 1f else -1f
            drawRoundRect(
                color = color,
                topLeft = Offset(origin.x - minDim * 0.02f * inward - minDim * 0.02f, origin.y - minDim * 0.04f),
                size = androidx.compose.ui.geometry.Size(minDim * 0.055f, minDim * 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(minDim * 0.012f),
            )
        }
    }
}
