package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Ported from Julius SphereEffectCanvas. */
@Composable
internal fun SphereEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere_loop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((15000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isActive) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
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
    val lowQuality = quality == GenartQuality.Low

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width.coerceAtMost(size.height) * 0.25f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                center = Offset(centerX, centerY),
                radius = size.width.coerceAtLeast(size.height) * 0.8f,
            ),
        )

        val baseRadius = maxRadius * scaleAnim
        val activeRadius = if (isActive) {
            baseRadius * (1f + pulse * 0.15f)
        } else {
            baseRadius
        }

        drawSphere(
            center = Offset(centerX, centerY),
            radius = activeRadius,
            rotation = rotation,
            isActive = isActive,
            pulse = pulse,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
        )

        if (!lowQuality) {
            drawOrbitingRings(
                center = Offset(centerX, centerY),
                baseRadius = activeRadius,
                rotation = rotation,
                isActive = isActive,
                count = 3,
                ringColors = listOf(primaryColor, secondaryColor, tertiaryColor),
            )
        }

        if (isActive && !lowQuality) {
            val glowRadius = activeRadius * (1.5f + pulse * 0.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f * pulse),
                        secondaryColor.copy(alpha = 0.1f * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(centerX, centerY),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(centerX, centerY),
            )
        }
    }
}

private fun DrawScope.drawSphere(
    center: Offset,
    radius: Float,
    rotation: Float,
    isActive: Boolean,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color,
) {
    val circles = if (isActive) 12 else 8
    val activePrimary = if (isActive) Color.White else primaryColor

    for (i in 0 until circles) {
        val angle = (i * 360f / circles + rotation) * PI.toFloat() / 180f
        val z = cos(angle)
        val ellipseRadius = radius * abs(z)
        val ellipseY = center.y + sin(angle) * radius * 0.3f
        val alpha = (0.3f + abs(z) * 0.7f) * (if (isActive) (0.8f + pulse * 0.2f) else 0.6f)
        val color = if (isActive) {
            lerp(
                activePrimary.copy(alpha = alpha),
                secondaryColor.copy(alpha = alpha),
                abs(z) * 0.5f + pulse * 0.3f,
            )
        } else {
            activePrimary.copy(alpha = alpha)
        }
        val ellipseWidth = ellipseRadius * 2
        val ellipseHeight = ellipseRadius * 0.6f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(color, color.copy(alpha = alpha * 0.3f), Color.Transparent),
                center = Offset(center.x, ellipseY),
                radius = ellipseRadius,
            ),
            topLeft = Offset(center.x - ellipseRadius, ellipseY - ellipseHeight / 2),
            size = Size(ellipseWidth, ellipseHeight),
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                activePrimary.copy(alpha = if (isActive) 0.6f else 0.4f),
                secondaryColor.copy(alpha = if (isActive) 0.3f else 0.2f),
                Color.Transparent,
            ),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawOrbitingRings(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    isActive: Boolean,
    count: Int,
    ringColors: List<Color>,
) {
    for (i in 0 until count) {
        val ringRadius = baseRadius * (1.3f + i * 0.4f)
        val ringRotation = (rotation * (if (i % 2 == 0) 1f else -1f) + i * 45f) * PI.toFloat() / 180f
        val alpha = if (isActive) 0.4f else 0.2f
        val ringPath = Path().apply {
            val segments = 64
            for (j in 0..segments) {
                val angle = (j * 360f / segments) * PI.toFloat() / 180f + ringRotation
                val x = center.x + cos(angle) * ringRadius
                val y = center.y + sin(angle) * ringRadius * 0.6f
                if (j == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = ringPath,
            color = ringColors[i % ringColors.size].copy(alpha = alpha),
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }
}
