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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class DogWalkGrass(
    val x: Float,
    val height: Float,
    val swayPhase: Float,
    val swayAmp: Float,
)

/** Distant dog silhouette walking slowly along a soft dusk horizon with rolling hills and grass. */
@Composable
internal fun DogDistantWalkEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grassCount = qualityCount(quality, low = 6, medium = 12, high = 20)
    val grasses = remember(grassCount) {
        List(grassCount) { i ->
            DogWalkGrass(
                x = seededUnit(i * 23 + 5),
                height = seededRange(i * 37 + 11, 0.03f, 0.065f),
                swayPhase = seededRange(i * 41 + 7, 0f, 2f * PI.toFloat()),
                swayAmp = seededRange(i * 59 + 3, 0.004f, 0.012f),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "dog_distant_walk_loop")
    val walkAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_distant_walk_t",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_distant_walk_drift",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val t = phase01(walkAnim)
        val dt = phase01(driftAnim)
        val haze = loopedFbm(t, radius = 1.5f, octaves = 2, seedOffset = 23)

        // Dusk sky gradient with deeper tones
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF0E1428),
                0.25f to Color(0xFF1A2438),
                0.5f to Color(0xFF3A2E3A),
                0.78f to Color(0xFF6E4A3E),
                1f to Color(0xFF1A1210),
            ),
        )

        // Distant soft hills behind the horizon
        val hillColor = TonalPalette.brightness(Color(0xFF1E1810), brightness * restingDim * 0.6f)
        val horizonY = h * (0.58f + 0.01f * haze)
        val hill = Path().apply {
            moveTo(0f, horizonY + h * 0.02f)
            cubicTo(
                w * 0.15f, horizonY - h * 0.08f,
                w * 0.35f, horizonY - h * 0.05f,
                w * 0.5f, horizonY - h * 0.01f,
            )
            cubicTo(
                w * 0.65f, horizonY + h * 0.02f,
                w * 0.82f, horizonY - h * 0.06f,
                w, horizonY + h * 0.01f,
            )
            lineTo(w, horizonY + h * 0.05f)
            lineTo(0f, horizonY + h * 0.05f)
            close()
        }
        drawPath(hill, color = hillColor)

        // Ground plane
        drawRect(
            color = TonalPalette.brightness(Color(0xFF1C1612), brightness * restingDim),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY),
        )
        // Soft ground wash
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF4A3828), 0.35f * restingDim),
                1f to Color.Transparent,
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h * 0.2f),
        )

        // Dusk glow on horizon
        val duskGlow = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFD08858), TonalPalette.pick(paletteColors, 1), 0.2f),
            brightness * restingDim,
        )
        drawRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(duskGlow, 0.35f + 0.08f * haze),
                0.6f to TonalPalette.withAlpha(duskGlow, 0.12f),
                1f to Color.Transparent,
                center = Offset(w * 0.65f, horizonY - h * 0.02f),
                radius = w * 0.45f,
            ),
            topLeft = Offset(0f, horizonY - h * 0.15f),
            size = Size(w, h * 0.25f),
        )

        // Grass blades swaying along horizon
        val grassColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A2210), TonalPalette.pick(paletteColors, 2), 0.15f),
            brightness * restingDim,
        )
        grasses.forEach { g ->
            val baseX = g.x * w
            val baseY = horizonY + minDim * 0.012f
            val sway = sin(dt * 2f * PI.toFloat() + g.swayPhase) * g.swayAmp * minDim * (if (isActive) 1f else 0.4f)
            val tipX = baseX + sway
            val tipY = baseY - g.height * minDim
            drawLine(
                color = grassColor,
                start = Offset(baseX, baseY),
                end = Offset(tipX, tipY),
                strokeWidth = minDim * 0.005f,
                cap = StrokeCap.Round,
            )
        }

        // Distant walk: ping-pong along horizon (smooth, no pop)
        val walkCycle = if (t < 0.5f) t * 2f else (1f - t) * 2f
        val dogX = w * (0.18f + 0.64f * walkCycle)
        val bob = sin(t * 4f * PI.toFloat()) * minDim * 0.004f * (if (isActive) 1f else 0.4f)
        val dogY = horizonY - minDim * 0.02f + bob
        val scale = minDim * 0.035f
        val sil = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF0E0C0A), TonalPalette.pick(paletteColors, 0), 0.15f),
            brightness * restingDim,
        )

        // Body
        drawOval(
            color = sil,
            topLeft = Offset(dogX - scale * 1.4f, dogY - scale * 1.1f),
            size = Size(scale * 2.8f, scale * 1.5f),
        )
        // Head
        drawCircle(color = sil, radius = scale * 0.7f, center = Offset(dogX + scale * 1.5f, dogY - scale * 0.9f))
        // Snout
        drawOval(
            color = sil,
            topLeft = Offset(dogX + scale * 2.0f, dogY - scale * 0.85f),
            size = Size(scale * 0.55f, scale * 0.35f),
        )
        // Ears / legs as simple strokes
        if (quality != GenartQuality.Low) {
            val legPhase = sin(t * 8f * PI.toFloat()) * scale * 0.25f * (if (isActive) 1f else 0.3f)
            val legs = Path().apply {
                moveTo(dogX - scale * 0.8f, dogY)
                lineTo(dogX - scale * 0.9f, dogY + scale * 1.2f + legPhase)
                moveTo(dogX + scale * 0.2f, dogY)
                lineTo(dogX + scale * 0.35f, dogY + scale * 1.2f - legPhase)
                moveTo(dogX + scale * 0.9f, dogY)
                lineTo(dogX + scale * 1.0f, dogY + scale * 1.15f + legPhase * 0.7f)
                moveTo(dogX - scale * 0.3f, dogY)
                lineTo(dogX - scale * 0.35f, dogY + scale * 1.18f - legPhase * 0.5f)
            }
            drawPath(legs, color = sil, style = Stroke(width = scale * 0.35f, cap = StrokeCap.Round))
            // Ear triangles
            val ear = Path().apply {
                val hx = dogX + scale * 1.5f
                val hy = dogY - scale * 0.9f
                moveTo(hx - scale * 0.2f, hy - scale * 0.3f)
                lineTo(hx - scale * 0.55f, hy - scale * 1.0f)
                lineTo(hx + scale * 0.1f, hy - scale * 0.45f)
                close()
            }
            drawPath(ear, color = sil)
            // Tail
            val tail = Path().apply {
                val base = Offset(dogX - scale * 1.3f, dogY - scale * 0.6f)
                val wagPhase = sin(t * 6f * PI.toFloat()) * scale * 0.4f * (if (isActive) 1f else 0.3f)
                moveTo(base.x, base.y)
                quadraticTo(
                    base.x - scale * 0.4f, base.y - scale * 0.8f + wagPhase,
                    base.x - scale * 0.15f, base.y - scale * 1.3f + wagPhase * 0.5f,
                )
            }
            drawPath(tail, color = sil, style = Stroke(width = scale * 0.3f, cap = StrokeCap.Round))
        }

        // Soft ambient glow motes near horizon (Medium/High)
        if (quality != GenartQuality.Low) {
            val moteCount = if (quality == GenartQuality.High) 4 else 2
            for (i in 0 until moteCount) {
                val mx = w * (0.2f + 0.15f * i + 0.03f * sin(dt * 2f * PI.toFloat() + i * 1.7f))
                val my = horizonY - h * (0.02f + 0.015f * i + 0.01f * sin(dt * 3f * PI.toFloat() + i))
                val moteAlpha = (0.12f + 0.08f * sin(dt * 4f * PI.toFloat() + i * 2.5f)) * restingDim
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(duskGlow, moteAlpha),
                        1f to Color.Transparent,
                        center = Offset(mx, my),
                        radius = minDim * 0.03f,
                    ),
                    radius = minDim * 0.03f,
                    center = Offset(mx, my),
                )
            }
        }
    }
}
