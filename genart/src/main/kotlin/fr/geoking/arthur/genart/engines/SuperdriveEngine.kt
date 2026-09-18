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
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Superdrive Vibes Engine: High-energy, vibrant multi-directional soundwave & vibe oscillations
 * inspired by Incubus album art aesthetic.
 */
@Composable
internal fun SuperdriveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val lineCount = qualityCount(quality, low = 12, medium = 18, high = 26)
    val vibeLines = remember(lineCount) {
        List(lineCount) { i ->
            SuperdriveVibeLine(
                angleRad = seededRange(i * 17 + 3, 0f, 2f * PI.toFloat()),
                baseYFrac = seededRange(i * 29 + 7, 0.1f, 0.9f),
                amplitude = seededRange(i * 41 + 11, 20f, 60f),
                frequency = seededRange(i * 53 + 13, 2f, 6f),
                phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                strokeWidth = seededRange(i * 79 + 23, 2f, 5f),
                alpha = seededRange(i * 97 + 31, 0.4f, 0.85f),
                colorIdx = i,
            )
        }
    }

    val ringCount = qualityCount(quality, low = 4, medium = 7, high = 10)
    val pulseRings = remember(ringCount) {
        List(ringCount) { i ->
            SuperdrivePulseRing(
                radiusFrac = seededRange(i * 31 + 5, 0.15f, 0.85f),
                vibeFreq = seededRange(i * 47 + 9, 4f, 12f),
                vibeAmp = seededRange(i * 61 + 15, 8f, 24f),
                phaseOffset = seededRange(i * 73 + 21, 0f, 2f * PI.toFloat()),
                colorIdx = i + 2,
            )
        }
    }

    val particleCount = qualityCount(quality, low = 20, medium = 40, high = 60)
    val particles = remember(particleCount) {
        List(particleCount) { i ->
            SuperdriveParticle(
                xFrac = seededUnit(i * 83 + 37),
                yFrac = seededUnit(i * 101 + 43),
                radius = seededRange(i * 113 + 47, 2f, 6f),
                phaseOffset = seededRange(i * 127 + 53, 0f, 2f * PI.toFloat()),
                colorIdx = i + 1,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "superdrive_vibes")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "superdrive_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        // High contrast dark background with glowing central radial bloom
        drawRect(color = Color(0xFF07040F))

        val centerCol = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 1.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(centerCol, 0.45f * dim),
                    TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 1), 0.2f * dim),
                    Color.Transparent,
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = minDim * 0.7f,
            ),
            radius = minDim * 0.7f,
            center = Offset(w * 0.5f, h * 0.5f),
        )

        val timeRad = time * 2f * PI.toFloat()

        // 1. Draw radiating concentric vibe rings
        pulseRings.forEach { ring ->
            val ringRadius = ring.radiusFrac * minDim * 0.5f
            val path = Path()
            val points = 60
            val ringCol = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIdx), brightness)

            for (p in 0..points) {
                val angle = (p.toFloat() / points) * 2f * PI.toFloat()
                val vibe = sin(angle * ring.vibeFreq + timeRad * 2f + ring.phaseOffset) * ring.vibeAmp
                val r = ringRadius + vibe
                val x = w * 0.5f + r * cos(angle)
                val y = h * 0.5f + r * sin(angle)
                if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = TonalPalette.withAlpha(ringCol, 0.55f * dim),
                style = Stroke(width = 2.5f),
            )
        }

        // 2. Draw multi-directional vibe lines crossing the canvas
        vibeLines.forEach { line ->
            val path = Path()
            val steps = 50
            val lineCol = TonalPalette.brightness(TonalPalette.pick(paletteColors, line.colorIdx), brightness)
            val cosA = cos(line.angleRad)
            val sinA = sin(line.angleRad)

            val length = minDim * 1.2f
            val startX = w * 0.5f - length * 0.5f * cosA
            val startY = h * 0.5f - length * 0.5f * sinA

            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val dist = frac * length
                val osc = sin(frac * line.frequency * PI.toFloat() * 2f + timeRad * 3f + line.phaseOffset) * line.amplitude
                val px = startX + dist * cosA - osc * sinA
                val py = startY + dist * sinA + osc * cosA
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            drawPath(
                path = path,
                color = TonalPalette.withAlpha(lineCol, line.alpha * dim),
                style = Stroke(width = line.strokeWidth),
            )
        }

        // 3. Draw glowing vibe energy particles
        particles.forEach { p ->
            val oscX = sin(timeRad * 2f + p.phaseOffset) * 15f
            val oscY = cos(timeRad * 2f + p.phaseOffset) * 15f
            val px = p.xFrac * w + oscX
            val py = p.yFrac * h + oscY
            val pCol = TonalPalette.brightness(TonalPalette.pick(paletteColors, p.colorIdx), brightness * 1.3f)

            drawCircle(
                color = TonalPalette.withAlpha(pCol, 0.8f * dim),
                radius = p.radius,
                center = Offset(px, py),
            )
        }
    }
}

private data class SuperdriveVibeLine(
    val angleRad: Float,
    val baseYFrac: Float,
    val amplitude: Float,
    val frequency: Float,
    val phaseOffset: Float,
    val strokeWidth: Float,
    val alpha: Float,
    val colorIdx: Int,
)

private data class SuperdrivePulseRing(
    val radiusFrac: Float,
    val vibeFreq: Float,
    val vibeAmp: Float,
    val phaseOffset: Float,
    val colorIdx: Int,
)

private data class SuperdriveParticle(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIdx: Int,
)
