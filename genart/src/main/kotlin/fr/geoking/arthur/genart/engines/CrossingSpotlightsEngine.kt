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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Several colored volumetric spot cones from different origins that cross and additively
 * blend in a dark room, with drifting dust motes inside the beams.
 */
@Composable
internal fun CrossingSpotlightsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val spotCount = qualityCount(quality, low = 3, medium = 4, high = 5)
    val dustCount = qualityCount(quality, low = 40, medium = 70, high = 110)
    val spots = remember(spotCount) {
        List(spotCount) { i ->
            CrossingSpot(
                ox = seededRange(i * 17 + 3, -0.05f, 1.05f),
                oy = seededRange(i * 29 + 7, -0.12f, 0.35f),
                angle = seededRange(i * 41 + 11, 0.15f, PI.toFloat() - 0.15f),
                halfWidth = seededRange(i * 53 + 13, 0.08f, 0.18f),
                reach = seededRange(i * 67 + 19, 0.85f, 1.35f),
                swayAmp = seededRange(i * 79 + 23, 0.02f, 0.06f),
                swayFreq = seededRange(i * 89 + 29, 0.2f, 0.55f),
                alpha = seededRange(i * 97 + 31, 0.18f, 0.38f),
                colorIndex = i,
            )
        }
    }
    val dust = remember(dustCount, spotCount) {
        List(dustCount) { j ->
            CrossingDust(
                spotIndex = (seededUnit(j * 13 + 5) * spotCount).toInt().coerceIn(0, spotCount - 1),
                along = seededRange(j * 19 + 7, 0.12f, 0.95f),
                side = seededRange(j * 23 + 11, -0.85f, 0.85f),
                sizeFrac = seededRange(j * 31 + 13, 0.0015f, 0.0055f),
                twinklePhase = seededRange(j * 37 + 17, 0f, 2f * PI.toFloat()),
                twinkleFreq = seededRange(j * 41 + 19, 0.6f, 2.2f),
                drift = seededRange(j * 43 + 23, 0.01f, 0.04f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "crossing_spots")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crossing_spots_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A1C12), Color(0xFF080A06), Color(0xFF020302)),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = minDim * 1.1f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f

        spots.forEach { spot ->
            val sway = spot.swayAmp * sin(time * spot.swayFreq)
            val a = spot.angle + sway
            val origin = Offset(spot.ox * w, spot.oy * h)
            val reach = spot.reach * h
            val left = Offset(
                origin.x + sin(a - spot.halfWidth) * reach,
                origin.y + cos(a - spot.halfWidth) * reach,
            )
            val right = Offset(
                origin.x + sin(a + spot.halfWidth) * reach,
                origin.y + cos(a + spot.halfWidth) * reach,
            )
            val mid = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f)
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, spot.colorIndex),
                brightness,
            )
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
                        TonalPalette.withAlpha(tint, spot.alpha * dim),
                        TonalPalette.withAlpha(tint, spot.alpha * 0.35f * dim),
                        Color.Transparent,
                    ),
                    start = origin,
                    end = mid,
                ),
                blendMode = BlendMode.Plus,
            )
            val poolR = minDim * (0.12f + spot.halfWidth * 0.55f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.22f * dim * brightness.coerceAtMost(1.2f)),
                        TonalPalette.withAlpha(tint, 0.28f * dim),
                        Color.Transparent,
                    ),
                    center = mid,
                    radius = poolR,
                ),
                radius = poolR,
                center = mid,
                blendMode = BlendMode.Plus,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color.White, 0.45f * dim),
                        Color.Transparent,
                    ),
                    center = origin,
                    radius = minDim * 0.06f,
                ),
                radius = minDim * 0.06f,
                center = origin,
                blendMode = BlendMode.Plus,
            )
        }

        dust.forEach { mote ->
            val spot = spots[mote.spotIndex]
            val sway = spot.swayAmp * sin(time * spot.swayFreq)
            val a = spot.angle + sway
            val origin = Offset(spot.ox * w, spot.oy * h)
            val reach = spot.reach * h
            val along = (mote.along + mote.drift * sin(time * 0.4f + mote.twinklePhase)).coerceIn(0.05f, 0.98f)
            val halfAt = spot.halfWidth * along
            val sideA = a + halfAt * mote.side
            val px = origin.x + sin(sideA) * reach * along
            val py = origin.y + cos(sideA) * reach * along
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * mote.twinkleFreq + mote.twinklePhase))
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, spot.colorIndex),
                brightness,
            )
            val r = mote.sizeFrac * minDim * (0.7f + 0.5f * twinkle)
            drawCircle(
                color = TonalPalette.withAlpha(
                    TonalPalette.mix(tint, Color.White, 0.55f),
                    0.55f * twinkle * dim,
                ),
                radius = r.coerceAtLeast(0.6f),
                center = Offset(px, py),
                blendMode = BlendMode.Plus,
            )
        }
    }
}

private data class CrossingSpot(
    val ox: Float,
    val oy: Float,
    val angle: Float,
    val halfWidth: Float,
    val reach: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alpha: Float,
    val colorIndex: Int,
)

private data class CrossingDust(
    val spotIndex: Int,
    val along: Float,
    val side: Float,
    val sizeFrac: Float,
    val twinklePhase: Float,
    val twinkleFreq: Float,
    val drift: Float,
)
