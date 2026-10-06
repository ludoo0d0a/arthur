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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Canyon-wall silhouettes with one lateral beam cutting the gorge, drifting dust,
 * and 2–3 parallax depth planes — instant 3D read without a mesh.
 */
@Composable
internal fun CanyonLightEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val planeCount = qualityCount(quality, low = 2, medium = 3, high = 3)
    val dustCount = qualityCount(quality, low = 28, medium = 48, high = 72)

    val planes = remember(planeCount) {
        List(planeCount) { i ->
            val depthFrac = if (planeCount > 1) i / (planeCount - 1f) else 0.5f
            CanyonLightPlaneSeed(
                depthFrac = depthFrac,
                leftInset = 0.08f + depthFrac * 0.18f + seededRange(i * 13 + 5, -0.02f, 0.02f),
                rightInset = 0.08f + depthFrac * 0.18f + seededRange(i * 17 + 7, -0.02f, 0.02f),
                ridgeAmp = seededRange(i * 19 + 11, 0.02f, 0.055f),
                ridgeFreq = seededRange(i * 23 + 13, 0.008f, 0.018f),
                ridgePhase = seededRange(i * 29 + 17, 0f, 2f * PI.toFloat()),
                parallax = seededRange(i * 31 + 19, 0.008f, 0.028f) * (0.4f + depthFrac),
                colorIndex = i,
            )
        }
    }
    val dust = remember(dustCount) {
        List(dustCount) { j ->
            CanyonLightDustSeed(
                along = seededRange(j * 19 + 7, 0.12f, 0.92f),
                side = seededRange(j * 23 + 11, -0.85f, 0.85f),
                sizeFrac = seededRange(j * 31 + 13, 0.0015f, 0.005f),
                twinklePhase = seededRange(j * 37 + 17, 0f, 2f * PI.toFloat()),
                twinkleFreq = seededRange(j * 41 + 19, 0.4f, 1.6f),
                drift = seededRange(j * 43 + 23, 0.008f, 0.03f),
                yJitter = seededUnit(j * 47 + 29),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "canyon_light")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((45000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "canyon_light_t",
    )

    val dim = if (isActive) 1f else 0.5f
    val parallaxScale = if (isActive) 1f else 0.35f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t) * 2f * PI.toFloat()

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.35f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, Color(0xFF1A0E08), Color(0xFF080402)),
            ),
        )

        val segments = (h / 10f).toInt().coerceIn(20, 120)
        val step = h / segments

        // Far → near planes
        planes.forEach { plane ->
            val shift = sin(time * 0.35f + plane.ridgePhase) * plane.parallax * parallaxScale
            val rock = TonalPalette.brightness(
                TonalPalette.mix(
                    Color(0xFF3A2218),
                    TonalPalette.pick(paletteColors, plane.colorIndex),
                    0.25f,
                ),
                brightness * (0.35f + plane.depthFrac * 0.55f),
            )
            val alpha = (0.55f + plane.depthFrac * 0.4f).coerceIn(0.4f, 0.95f)

            // Left wall
            val leftPath = Path()
            val leftBase = (plane.leftInset + shift) * w
            leftPath.moveTo(0f, 0f)
            leftPath.lineTo(leftBase, 0f)
            for (s in 1..segments) {
                val y = s * step
                val ridge = sin(y * plane.ridgeFreq + plane.ridgePhase) * plane.ridgeAmp * w
                leftPath.lineTo(leftBase + ridge, y)
            }
            leftPath.lineTo(0f, h)
            leftPath.close()
            drawPath(path = leftPath, color = TonalPalette.withAlpha(rock, alpha))

            // Right wall
            val rightPath = Path()
            val rightBase = (1f - plane.rightInset - shift) * w
            rightPath.moveTo(w, 0f)
            rightPath.lineTo(rightBase, 0f)
            for (s in 1..segments) {
                val y = s * step
                val ridge = sin(y * plane.ridgeFreq + plane.ridgePhase + 1.7f) * plane.ridgeAmp * w
                rightPath.lineTo(rightBase - ridge, y)
            }
            rightPath.lineTo(w, h)
            rightPath.close()
            drawPath(path = rightPath, color = TonalPalette.withAlpha(rock, alpha))
        }

        // Lateral beam through the gorge (from left mid-height)
        val nearest = planes.last()
        val gorgeLeft = (nearest.leftInset + 0.06f) * w
        val gorgeRight = (1f - nearest.rightInset - 0.06f) * w
        val beamY = h * (0.38f + 0.04f * sin(time * 0.25f))
        val beamHalf = h * (0.055f + 0.012f * sin(time * 0.4f))
        val beamTint = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFF5D78A), TonalPalette.pick(paletteColors, 0), 0.3f),
            brightness,
        )
        val beamPath = Path().apply {
            moveTo(gorgeLeft, beamY - beamHalf * 0.35f)
            lineTo(gorgeRight, beamY - beamHalf)
            lineTo(gorgeRight, beamY + beamHalf)
            lineTo(gorgeLeft, beamY + beamHalf * 0.35f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(beamTint, 0.08f * dim),
                    TonalPalette.withAlpha(beamTint, 0.32f * dim),
                    TonalPalette.withAlpha(beamTint, 0.18f * dim),
                    Color.Transparent,
                ),
                startX = gorgeLeft,
                endX = gorgeRight,
            ),
        )
        // Soft pool where beam hits far wall
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.White, 0.28f * dim * brightness.coerceAtMost(1.2f)),
                    TonalPalette.withAlpha(beamTint, 0.2f * dim),
                    Color.Transparent,
                ),
                center = Offset(gorgeRight - minDim * 0.02f, beamY),
                radius = minDim * 0.14f,
            ),
            radius = minDim * 0.14f,
            center = Offset(gorgeRight - minDim * 0.02f, beamY),
        )

        dust.forEach { mote ->
            val along = (mote.along + mote.drift * sin(time * 0.3f + mote.twinklePhase)).coerceIn(0.05f, 0.98f)
            val px = gorgeLeft + (gorgeRight - gorgeLeft) * along
            val py = beamY + mote.side * beamHalf * (0.5f + along * 0.5f) +
                (mote.yJitter - 0.5f) * beamHalf * 0.4f
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * mote.twinkleFreq + mote.twinklePhase))
            val r = mote.sizeFrac * minDim * (0.7f + 0.5f * twinkle)
            drawCircle(
                color = TonalPalette.withAlpha(
                    TonalPalette.mix(beamTint, Color.White, 0.55f),
                    0.5f * twinkle * dim,
                ),
                radius = r.coerceAtLeast(0.5f),
                center = Offset(px, py),
            )
        }

        // Floor shadow in gorge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color(0xFF040201), 0.65f * dim)),
                startY = h * 0.72f,
                endY = h,
            ),
        )
    }
}

private data class CanyonLightPlaneSeed(
    val depthFrac: Float,
    val leftInset: Float,
    val rightInset: Float,
    val ridgeAmp: Float,
    val ridgeFreq: Float,
    val ridgePhase: Float,
    val parallax: Float,
    val colorIndex: Int,
)

private data class CanyonLightDustSeed(
    val along: Float,
    val side: Float,
    val sizeFrac: Float,
    val twinklePhase: Float,
    val twinkleFreq: Float,
    val drift: Float,
    val yJitter: Float,
)
