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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branching magenta/violet energy tendrils rising from a hot core over a cyan horizon haze.
 * Soft bloom pulses; no hard flashes.
 */
@Composable
internal fun EnergyTendrilsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val tendrilCount = qualityCount(quality, low = 5, medium = 7, high = 10)
    val tendrils = remember(tendrilCount) {
        List(tendrilCount) { i ->
            EnergyTendrilSeed(
                baseAngle = seededRange(i * 19 + 3, -1.1f, 1.1f) - PI.toFloat() * 0.5f,
                lengthFrac = seededRange(i * 31 + 7, 0.45f, 0.85f),
                curve = seededRange(i * 43 + 11, -0.55f, 0.55f),
                thickness = seededRange(i * 53 + 13, 3.5f, 8.5f),
                alpha = seededRange(i * 67 + 17, 0.35f, 0.7f),
                branchCount = 2 + (seededUnit(i * 79 + 19) * 3f).toInt(),
                hueBias = seededUnit(i * 89 + 23),
                colorIdx = i,
                phase = seededUnit(i * 97 + 29),
            )
        }
    }
    val sparkCount = qualityCount(quality, low = 24, medium = 40, high = 60)
    val sparks = remember(sparkCount) {
        List(sparkCount) { i ->
            val s = i * 61 + 3001
            EnergyTendrilSpark(
                xFrac = seededRange(s, 0.1f, 0.9f),
                yFrac = seededRange(s + 5, 0.05f, 0.75f),
                radius = seededRange(s + 11, 0.8f, 2.4f),
                alpha = seededRange(s + 17, 0.2f, 0.65f),
                phase = seededUnit(s + 23),
            )
        }
    }
    val glyphCount = qualityCount(quality, low = 8, medium = 12, high = 16)
    val glyphs = remember(glyphCount) {
        List(glyphCount) { i ->
            EnergyTendrilGlyph(
                xFrac = seededRange(i * 27 + 5, 0.08f, 0.92f),
                widthFrac = seededRange(i * 37 + 9, 0.02f, 0.06f),
                alpha = seededRange(i * 47 + 13, 0.12f, 0.28f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "energy_tendrils")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "energy_tendrils_t",
    )
    val pulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "energy_tendrils_pulse",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val dim = if (isActive) 1f else 0.55f
        val time = phase01(t)
        val pulse = sin01(phase01(pulseT) * 2f * PI.toFloat())
        val cyan = Color(0xFF3DFFF0)
        val magenta = Color(0xFFFF2EC8)
        val violet = Color(0xFFA855FF)
        val ice = Color(0xFFF4FBFF)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF01020A), Color(0xFF050818), Color(0xFF0A1A28)),
            ),
        )

        // Cyan horizon wash
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    TonalPalette.withAlpha(cyan, 0.08f * dim),
                    TonalPalette.withAlpha(cyan, 0.32f * dim * (0.85f + 0.15f * pulse)),
                    TonalPalette.withAlpha(Color(0xFF0A3040), 0.55f * dim),
                ),
                startY = h * 0.45f,
                endY = h,
            ),
        )

        val origin = Offset(w * 0.28f, h * 0.72f)

        // Core bloom
        val coreR = minDim * (0.18f + 0.03f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(ice, 0.7f * dim * brightness.coerceAtMost(1.35f)),
                    TonalPalette.withAlpha(magenta, 0.4f * dim),
                    TonalPalette.withAlpha(violet, 0.15f * dim),
                    Color.Transparent,
                ),
                center = origin,
                radius = coreR * 2.4f,
            ),
            radius = coreR * 2.4f,
            center = origin,
        )

        tendrils.forEach { tendril ->
            val sway = 0.08f * sin(time * 2f * PI.toFloat() + tendril.phase * 2f * PI.toFloat())
            val accent = if (tendril.hueBias > 0.5f) magenta else violet
            val tint = TonalPalette.brightness(
                TonalPalette.mix(accent, TonalPalette.pick(paletteColors, tendril.colorIdx), 0.25f),
                brightness,
            )
            val len = minDim * tendril.lengthFrac * (0.92f + 0.08f * pulse)
            val path = Path()
            val steps = 28
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val ang = tendril.baseAngle + sway + tendril.curve * frac * frac
                val r = len * frac
                val noise = sin(frac * 6f + time * 2f * PI.toFloat() + tendril.phase) * minDim * 0.012f * frac
                val px = origin.x + cos(ang) * r + cos(ang + PI.toFloat() * 0.5f) * noise
                val py = origin.y + sin(ang) * r + sin(ang + PI.toFloat() * 0.5f) * noise
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val aMul = tendril.alpha * dim * (0.8f + 0.2f * pulse)
            // Outer glow stroke
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, aMul * 0.35f),
                style = Stroke(width = tendril.thickness * 2.2f, cap = StrokeCap.Round),
            )
            // Hot core stroke
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(ice, aMul),
                        TonalPalette.withAlpha(tint, aMul * 0.75f),
                        TonalPalette.withAlpha(violet, aMul * 0.35f),
                    ),
                ),
                style = Stroke(width = tendril.thickness * 0.55f, cap = StrokeCap.Round),
            )

            // Soft branches near tip
            for (b in 0 until tendril.branchCount) {
                val tipFrac = 0.55f + b * 0.12f
                val tipAng = tendril.baseAngle + sway + tendril.curve * tipFrac * tipFrac
                val tipR = len * tipFrac
                val tip = Offset(origin.x + cos(tipAng) * tipR, origin.y + sin(tipAng) * tipR)
                val branchAng = tipAng + (if (b % 2 == 0) 0.55f else -0.55f)
                val branchLen = len * (0.18f + b * 0.04f)
                val bend = Offset(
                    tip.x + cos(branchAng) * branchLen,
                    tip.y + sin(branchAng) * branchLen,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, aMul * 0.55f),
                            Color.Transparent,
                        ),
                        start = tip,
                        end = bend,
                    ),
                    start = tip,
                    end = bend,
                    strokeWidth = tendril.thickness * 0.35f,
                    cap = StrokeCap.Round,
                )
            }
        }

        sparks.forEach { spark ->
            val twinkle = 0.6f + 0.4f * sin01((time + spark.phase) * 2f * PI.toFloat())
            drawCircle(
                color = TonalPalette.withAlpha(ice, spark.alpha * twinkle * dim),
                radius = spark.radius,
                center = Offset(spark.xFrac * w, spark.yFrac * h),
            )
        }

        // Soft sci-fi readout dashes along the horizon (abstract, not HUD text)
        glyphs.forEach { g ->
            val y = h * 0.92f
            val x = g.xFrac * w
            val gw = g.widthFrac * w
            drawLine(
                color = TonalPalette.withAlpha(cyan, g.alpha * dim),
                start = Offset(x, y),
                end = Offset(x + gw, y),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class EnergyTendrilSeed(
    val baseAngle: Float,
    val lengthFrac: Float,
    val curve: Float,
    val thickness: Float,
    val alpha: Float,
    val branchCount: Int,
    val hueBias: Float,
    val colorIdx: Int,
    val phase: Float,
)

private data class EnergyTendrilSpark(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
)

private data class EnergyTendrilGlyph(
    val xFrac: Float,
    val widthFrac: Float,
    val alpha: Float,
)
