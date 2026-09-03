package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import kotlin.math.PI
import kotlin.math.sin

/** Ported from Julius WavesEffectCanvas. */
@Composable
internal fun WavesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waves_loop")
    val durationScale = speed.coerceAtLeast(0.2f)
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((4000 / durationScale).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase1",
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((5000 / durationScale).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase2",
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((6000 / durationScale).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase3",
    )
    val amplitudeAnim by animateFloatAsState(
        targetValue = if (isActive) 1.5f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "amplitude",
    )
    val primaryColor = TonalPalette.brightness(
        paletteColors.firstOrNull() ?: Color(0xFF6366F1),
        brightness,
    )
    val secondaryColor = TonalPalette.brightness(
        paletteColors.getOrNull(1) ?: primaryColor,
        brightness,
    )
    val tertiaryColor = TonalPalette.brightness(
        paletteColors.getOrNull(2) ?: secondaryColor,
        brightness,
    )
    val quaternaryColor = TonalPalette.brightness(
        paletteColors.getOrNull(3) ?: tertiaryColor,
        brightness,
    )
    val quinaryColor = TonalPalette.brightness(
        paletteColors.getOrNull(4) ?: quaternaryColor,
        brightness,
    )
    val lowQuality = quality == GenartQuality.Low

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E293B),
                    Color(0xFF0F172A),
                    Color(0xFF020617),
                ),
                startY = 0f,
                endY = size.height,
            ),
        )

        drawWaveLayer(
            phase = phase1,
            amplitude = 40f * amplitudeAnim,
            frequency = 0.015f,
            color = primaryColor,
            centerY = centerY * 0.3f,
            isActive = isActive,
            alpha = 0.6f,
        )
        drawWaveLayer(
            phase = phase2,
            amplitude = 50f * amplitudeAnim,
            frequency = 0.012f,
            color = secondaryColor,
            centerY = centerY * 0.6f,
            isActive = isActive,
            alpha = 0.5f,
        )
        drawWaveLayer(
            phase = phase3,
            amplitude = 45f * amplitudeAnim,
            frequency = 0.010f,
            color = tertiaryColor,
            centerY = centerY * 0.9f,
            isActive = isActive,
            alpha = 0.4f,
        )

        if (isActive && !lowQuality) {
            drawWaveLayer(
                phase = phase1 * 1.3f,
                amplitude = 35f * amplitudeAnim,
                frequency = 0.018f,
                color = quaternaryColor,
                centerY = centerY * 0.45f,
                isActive = true,
                alpha = 0.3f,
            )
            drawWaveLayer(
                phase = phase2 * 0.8f,
                amplitude = 55f * amplitudeAnim,
                frequency = 0.008f,
                color = quinaryColor,
                centerY = centerY * 1.15f,
                isActive = true,
                alpha = 0.3f,
            )
        }

        if (!lowQuality) {
            drawRadialWaves(
                center = Offset(centerX, centerY),
                phase = phase1,
                isActive = isActive,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
            )
        }
    }
}

private fun DrawScope.drawWaveLayer(
    phase: Float,
    amplitude: Float,
    frequency: Float,
    color: Color,
    centerY: Float,
    isActive: Boolean,
    alpha: Float,
) {
    val path = Path()
    val waveColor = if (isActive) {
        Color(
            red = (color.red * 0.7f + 0.3f).coerceIn(0f, 1f),
            green = (color.green * 0.7f + 0.3f).coerceIn(0f, 1f),
            blue = (color.blue * 0.7f + 0.3f).coerceIn(0f, 1f),
            alpha = alpha,
        )
    } else {
        color.copy(alpha = alpha)
    }

    val segments = size.width.toInt().coerceAtLeast(2)
    val step = size.width / segments
    path.moveTo(0f, centerY)
    for (i in 0..segments) {
        val x = i * step
        val y = centerY + sin(x * frequency + phase) * amplitude
        path.lineTo(x, y)
    }
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                waveColor,
                waveColor.copy(alpha = alpha * 0.5f),
                Color.Transparent,
            ),
            startY = centerY - amplitude,
            endY = size.height,
        ),
    )

    val outlinePath = Path()
    outlinePath.moveTo(0f, centerY + sin(phase) * amplitude)
    for (i in 1..segments) {
        val x = i * step
        val y = centerY + sin(x * frequency + phase) * amplitude
        outlinePath.lineTo(x, y)
    }
    drawPath(
        path = outlinePath,
        color = waveColor.copy(alpha = (alpha + 0.2f).coerceIn(0f, 1f)),
        style = Stroke(width = 2f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRadialWaves(
    center: Offset,
    phase: Float,
    isActive: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
) {
    val maxRadius = size.width.coerceAtLeast(size.height) * 0.8f
    val waveCount = if (isActive) 8 else 5
    for (i in 0 until waveCount) {
        val radius = (maxRadius / waveCount) * (i + 1) + sin(phase + i) * 20f
        val alpha = (1f - (i / waveCount.toFloat())) * 0.15f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    secondaryColor.copy(alpha = alpha * 0.7f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            style = Stroke(
                width = if (isActive) 3f else 2f,
                cap = StrokeCap.Round,
            ),
        )
    }
}
