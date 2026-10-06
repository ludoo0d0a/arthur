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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One premium glass sphere on a desk — terminator shading, specular highlight,
 * fake refraction swirl inside, soft elliptical shadow, and floating dust.
 */
@Composable
internal fun GlassOrbEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val dustCount = qualityCount(quality, low = 18, medium = 32, high = 48)
    val orb = remember {
        GlassOrbSeed(
            x0 = 0.5f,
            y0 = 0.48f,
            radiusFrac = 0.28f,
            highlightAngle = -0.72f,
            swirlSpin = 0.35f,
            breathAmp = 0.012f,
        )
    }
    val dust = remember(dustCount) {
        List(dustCount) { j ->
            GlassOrbDustSeed(
                x0 = seededUnit(j * 13 + 5),
                y0 = seededRange(j * 19 + 7, 0.15f, 0.75f),
                sizeFrac = seededRange(j * 31 + 13, 0.0015f, 0.0045f),
                driftAmp = seededRange(j * 37 + 17, 0.01f, 0.04f),
                twinklePhase = seededRange(j * 43 + 23, 0f, 2f * PI.toFloat()),
                twinkleFreq = seededRange(j * 47 + 29, 0.4f, 1.5f),
                seedOffset = j * 619 + 41,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "glass_orb")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((50000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glass_orb_t",
    )

    val dim = if (isActive) 1f else 0.5f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val timeAngle = time * 2f * PI.toFloat()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1C1824), Color(0xFF0E0C12), Color(0xFF060508)),
            ),
        )

        // Desk plane
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2434).copy(alpha = 0.45f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.82f),
                radius = minDim * 0.75f,
            ),
            radius = minDim * 0.75f,
            center = Offset(w * 0.5f, h * 0.82f),
        )

        val breathe = 1f + orb.breathAmp * (loopedFbm(time, radius = 1.3f, seedOffset = 17) * 2f - 1f)
        val cx = orb.x0 * w
        val cy = orb.y0 * h
        val r = orb.radiusFrac * minDim * breathe
        val tint = TonalPalette.brightness(
            TonalPalette.mix(
                TonalPalette.pick(paletteColors, 0),
                Color(0xFFE8F0FF),
                0.22f,
            ),
            brightness,
        )
        val a = dim

        // Soft elliptical floor shadow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFF000000), 0.45f * a),
                    Color.Transparent,
                ),
                center = Offset(cx, cy + r * 1.05f),
                radius = r * 1.35f,
            ),
            topLeft = Offset(cx - r * 1.15f, cy + r * 0.72f),
            size = Size(r * 2.3f, r * 0.55f),
        )

        // Glass body with terminator (lit upper-left → dark lower-right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.White, a * 0.65f),
                    TonalPalette.withAlpha(tint, a * 0.55f),
                    TonalPalette.withAlpha(tint, a * 0.28f),
                    TonalPalette.withAlpha(Color(0xFF0A0A12), a * 0.7f),
                ),
                center = Offset(cx - r * 0.32f, cy - r * 0.38f),
                radius = r * 1.25f,
            ),
            radius = r,
            center = Offset(cx, cy),
        )

        // Fake refraction swirl inside
        val swirl = timeAngle * orb.swirlSpin
        val sx = cx + cos(swirl) * r * 0.2f
        val sy = cy + sin(swirl * 0.85f) * r * 0.16f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(tint, a * 0.5f),
                    TonalPalette.withAlpha(Color(0xFFA8C8E8), a * 0.22f),
                    Color.Transparent,
                ),
                center = Offset(sx, sy),
                radius = r * 0.48f,
            ),
            radius = r * 0.48f,
            center = Offset(sx, sy),
        )
        // Secondary refraction pocket (sine-warp feel)
        val sx2 = cx + cos(swirl + 2.1f) * r * 0.28f
        val sy2 = cy + sin(swirl + 1.4f) * r * 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.White, a * 0.18f),
                    Color.Transparent,
                ),
                center = Offset(sx2, sy2),
                radius = r * 0.32f,
            ),
            radius = r * 0.32f,
            center = Offset(sx2, sy2),
        )

        // Rim light
        drawCircle(
            color = TonalPalette.withAlpha(Color.White, a * 0.4f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = (r * 0.035f).coerceAtLeast(1f)),
        )

        // Specular highlight
        val hx = cx + cos(orb.highlightAngle) * r * 0.4f
        val hy = cy + sin(orb.highlightAngle) * r * 0.4f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.White, a * 0.95f),
                    Color.Transparent,
                ),
                center = Offset(hx, hy),
                radius = r * 0.26f,
            ),
            radius = r * 0.26f,
            center = Offset(hx, hy),
        )
        // Secondary glint
        drawCircle(
            color = TonalPalette.withAlpha(Color.White, a * 0.5f),
            radius = r * 0.055f,
            center = Offset(cx + r * 0.38f, cy - r * 0.12f),
        )

        dust.forEach { mote ->
            val driftX = (loopedFbm(time, radius = 1.2f, seedOffset = mote.seedOffset) * 2f - 1f) * mote.driftAmp
            val driftY = (loopedFbm(time, radius = 1.5f, seedOffset = mote.seedOffset + 3) * 2f - 1f) * mote.driftAmp
            val px = phase01(mote.x0 + driftX) * w
            val py = phase01(mote.y0 + driftY) * h
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(timeAngle * mote.twinkleFreq + mote.twinklePhase))
            val mr = mote.sizeFrac * minDim * (0.7f + 0.5f * twinkle)
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFE8E0F0), 0.4f * twinkle * dim),
                radius = mr.coerceAtLeast(0.5f),
                center = Offset(px, py),
            )
        }
    }
}

private data class GlassOrbSeed(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val highlightAngle: Float,
    val swirlSpin: Float,
    val breathAmp: Float,
)

private data class GlassOrbDustSeed(
    val x0: Float,
    val y0: Float,
    val sizeFrac: Float,
    val driftAmp: Float,
    val twinklePhase: Float,
    val twinkleFreq: Float,
    val seedOffset: Int,
)
