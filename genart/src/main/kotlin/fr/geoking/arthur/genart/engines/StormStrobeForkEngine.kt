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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.exp

private data class ForkFlash(
    val x: Float,
    val phaseOffset: Float,
    val spread: Float,
    val branchAngle: Float,
)

/** A distant fork of lightning diffused by layered storm clouds. */
@Composable
internal fun StormStrobeForkEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 5)
    val flashes = androidx.compose.runtime.remember(count) {
        List(count) { i ->
            ForkFlash(
                x = seededRange(i * 19 + 5, 0.28f, 0.72f),
                phaseOffset = seededUnit(i * 31 + 9),
                spread = seededRange(i * 43 + 13, 0.10f, 0.22f),
                branchAngle = seededRange(i * 53 + 17, -0.10f, 0.10f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "stormstrobefork")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stormstrobefork_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val cloudDrift = loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = 9)
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF080C16), brightness * dim),
                0.55f to TonalPalette.brightness(Color(0xFF111A28), brightness * dim),
                1f to TonalPalette.brightness(Color(0xFF1A2230), brightness * dim * (0.92f + 0.08f * cloudDrift)),
            ),
        )

        // Sparse, localized backlight pulses. The surrounding sky stays dark.
        flashes.forEachIndexed { index, flash ->
            val local = (time + flash.phaseOffset) % 1f
            val pulse = exp(-((local - 0.16f) * (local - 0.16f)) / 0.0016f) * dim
            if (pulse < 0.012f) return@forEachIndexed

            val cx = flash.x * w + (cloudDrift - 0.5f) * w * 0.18f + time * w * 0.12f
            val cy = h * 0.43f
            val radius = minDim * (0.28f + 0.16f * pulse)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(Color(0xFFDCE8FF), 0.28f * pulse * brightness),
                    0.38f to TonalPalette.withAlpha(Color(0xFFAFC7EA), 0.16f * pulse * brightness),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )

            // A forked zigzag, softened into the distance and partly hidden by cloud.
            val trunk = Path().apply {
                moveTo(cx, cy - minDim * 0.09f)
                lineTo(cx - minDim * 0.018f, cy - minDim * 0.01f)
                lineTo(cx + minDim * 0.005f, cy + minDim * 0.055f)
                lineTo(cx - minDim * 0.014f, cy + minDim * 0.13f)
                lineTo(cx + minDim * flash.branchAngle, cy + minDim * 0.22f)
            }
            val boltColor = TonalPalette.withAlpha(Color(0xFFDCE8FF), 0.85f * pulse * brightness)
            drawPath(
                path = trunk,
                color = boltColor,
                style = Stroke(width = minDim * 0.004f, cap = StrokeCap.Round),
            )

            val branch = Path().apply {
                moveTo(cx + minDim * 0.005f, cy + minDim * 0.055f)
                lineTo(cx + minDim * 0.055f, cy + minDim * 0.09f)
                lineTo(cx + minDim * flash.spread, cy + minDim * 0.15f)
            }
            drawPath(
                path = branch,
                color = TonalPalette.withAlpha(Color(0xFFC7D8F2), 0.38f * pulse * brightness),
                style = Stroke(width = minDim * 0.0025f, cap = StrokeCap.Round),
            )
            if (index % 2 == 0) {
                val shortBranch = Path().apply {
                    moveTo(cx - minDim * 0.014f, cy + minDim * 0.13f)
                    lineTo(cx - minDim * 0.065f, cy + minDim * 0.17f)
                    lineTo(cx - minDim * flash.spread, cy + minDim * 0.20f)
                }
                drawPath(
                    path = shortBranch,
                    color = TonalPalette.withAlpha(Color(0xFFC7D8F2), 0.30f * pulse * brightness),
                    style = Stroke(width = minDim * 0.002f, cap = StrokeCap.Round),
                )
            }
        }

        // Low, overlapping cloud shelves obscure the top of each distant fork.
        val cloudColor = TonalPalette.brightness(Color(0xFF0A0E18), brightness * dim)
        val cloudHighlight = TonalPalette.mix(Color(0xFF1A2230), TonalPalette.pick(paletteColors, 0), 0.18f)
        val drift = (cloudDrift - 0.5f) * w * 0.18f + time * w * 0.22f
        drawOval(cloudColor, Offset(-w * 0.1f + drift, h * 0.28f), androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.30f))
        drawOval(cloudColor, Offset(w * 0.24f - drift * 0.7f, h * 0.22f), androidx.compose.ui.geometry.Size(w * 0.86f, h * 0.34f))
        drawOval(cloudHighlight.copy(alpha = 0.22f * dim), Offset(w * 0.02f + drift, h * 0.39f), androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.13f))
        drawOval(cloudHighlight.copy(alpha = 0.18f * dim), Offset(w * 0.46f - drift * 0.7f, h * 0.40f), androidx.compose.ui.geometry.Size(w * 0.63f, h * 0.12f))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(Color(0xFF080C16), 0.12f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF080C16), 0.45f * dim),
                startY = h * 0.48f,
                endY = h,
            ),
        )
    }
}
