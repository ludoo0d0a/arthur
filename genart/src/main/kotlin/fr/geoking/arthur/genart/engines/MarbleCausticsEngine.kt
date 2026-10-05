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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A few large glass orbs casting soft colored caustic pools onto a dark floor —
 * refraction blooms shimmer slowly under each sphere.
 */
@Composable
internal fun MarbleCausticsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val orbCount = qualityCount(quality, low = 3, medium = 4, high = 5)
    val causticPerOrb = qualityCount(quality, low = 3, medium = 5, high = 7)
    val orbs = remember(orbCount) {
        List(orbCount) { i ->
            CausticOrbSeed(
                x0 = seededRange(i * 17 + 3, 0.18f, 0.82f),
                y0 = seededRange(i * 29 + 7, 0.28f, 0.52f),
                radiusFrac = seededRange(i * 41 + 11, 0.1f, 0.18f),
                bobAmp = seededRange(i * 53 + 13, 0.008f, 0.02f),
                bobFreq = seededRange(i * 67 + 19, 0.2f, 0.5f),
                bobPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                highlightAngle = seededRange(i * 89 + 29, -1.1f, -0.4f),
                colorIndex = i,
            )
        }
    }
    val caustics = remember(orbCount, causticPerOrb) {
        List(orbCount * causticPerOrb) { j ->
            val orbIndex = j % orbCount
            CausticBloomSeed(
                orbIndex = orbIndex,
                angle = seededRange(j * 19 + 5, 0f, 2f * PI.toFloat()),
                lengthFrac = seededRange(j * 31 + 11, 0.08f, 0.22f),
                curve = seededRange(j * 43 + 13, 0.02f, 0.07f),
                thicknessFrac = seededRange(j * 59 + 17, 0.008f, 0.02f),
                shimmerFreq = seededRange(j * 71 + 19, 0.3f, 0.9f),
                shimmerPhase = seededRange(j * 83 + 23, 0f, 2f * PI.toFloat()),
                alpha = seededRange(j * 97 + 29, 0.12f, 0.32f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "marble_caustics")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((40000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "marble_caustics_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF12161C), Color(0xFF070A0E), Color(0xFF030508)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f

        // Floor plane hint
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1C2430).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.82f),
                radius = minDim * 0.75f,
            ),
            radius = minDim * 0.75f,
            center = Offset(w * 0.5f, h * 0.82f),
        )

        // Caustic pools under orbs (drawn first)
        caustics.forEach { bloom ->
            val orb = orbs[bloom.orbIndex]
            val bob = orb.bobAmp * sin(time * orb.bobFreq + orb.bobPhase)
            val cx = orb.x0 * w
            val cy = (orb.y0 + bob) * h
            val r = orb.radiusFrac * minDim
            val floorY = cy + r * 1.15f
            val shimmer = 0.55f + 0.45f * sin01(time * bloom.shimmerFreq + bloom.shimmerPhase)
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, orb.colorIndex),
                brightness,
            )
            val a = bloom.alpha * shimmer * dim
            val path = Path()
            val steps = 10
            val start = Offset(cx, floorY)
            path.moveTo(start.x, start.y)
            for (s in 1..steps) {
                val u = s / steps.toFloat()
                val len = bloom.lengthFrac * minDim * u
                val bend = bloom.curve * minDim * sin(u * PI.toFloat())
                val px = cx + cos(bloom.angle) * len + cos(bloom.angle + PI.toFloat() / 2f) * bend
                val py = floorY + sin(bloom.angle) * len * 0.35f + bend * 0.2f
                path.lineTo(px, py)
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, a),
                        TonalPalette.withAlpha(Color.White, a * 0.35f),
                        Color.Transparent,
                    ),
                    center = start,
                    radius = bloom.lengthFrac * minDim,
                ),
                style = Stroke(width = bloom.thicknessFrac * minDim * (0.7f + 0.5f * shimmer)),
            )
            // Soft pool disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, a * 0.55f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, floorY),
                    radius = r * 1.4f,
                ),
                radius = r * 1.4f,
                center = Offset(cx, floorY),
            )
        }

        orbs.forEach { orb ->
            val bob = orb.bobAmp * sin(time * orb.bobFreq + orb.bobPhase)
            val cx = orb.x0 * w
            val cy = (orb.y0 + bob) * h
            val r = orb.radiusFrac * minDim
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    TonalPalette.pick(paletteColors, orb.colorIndex),
                    Color(0xFFDDE8F8),
                    0.22f,
                ),
                brightness,
            )
            val a = dim

            // Soft contact shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.35f * dim), Color.Transparent),
                    center = Offset(cx, cy + r * 1.05f),
                    radius = r * 1.2f,
                ),
                topLeft = Offset(cx - r * 0.9f, cy + r * 0.75f),
                size = Size(r * 1.8f, r * 0.45f),
            )

            // Glass sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.5f * a),
                        TonalPalette.withAlpha(tint, 0.55f * a),
                        TonalPalette.withAlpha(tint, 0.18f * a),
                        TonalPalette.withAlpha(Color(0xFF080C12), 0.5f * a),
                    ),
                    center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                    radius = r * 1.2f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, 0.28f * a),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = (r * 0.035f).coerceAtLeast(0.8f)),
            )
            val hx = cx + cos(orb.highlightAngle) * r * 0.4f
            val hy = cy + sin(orb.highlightAngle) * r * 0.4f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.9f * a),
                        Color.Transparent,
                    ),
                    center = Offset(hx, hy),
                    radius = r * 0.32f,
                ),
                radius = r * 0.32f,
                center = Offset(hx, hy),
            )
            // Tiny secondary sparkle
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, 0.5f * a),
                radius = r * 0.05f,
                center = Offset(cx + r * 0.48f, cy - r * 0.1f),
            )
        }
    }
}

private data class CausticOrbSeed(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val bobAmp: Float,
    val bobFreq: Float,
    val bobPhase: Float,
    val highlightAngle: Float,
    val colorIndex: Int,
)

private data class CausticBloomSeed(
    val orbIndex: Int,
    val angle: Float,
    val lengthFrac: Float,
    val curve: Float,
    val thicknessFrac: Float,
    val shimmerFreq: Float,
    val shimmerPhase: Float,
    val alpha: Float,
)
