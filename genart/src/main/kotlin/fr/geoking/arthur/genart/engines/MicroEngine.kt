package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette

/**
 * Volumetric light rays — ported from Julius MicroEffectCanvas (voice status → isActive).
 */
@Composable
internal fun MicroEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micro_rays")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val primaryColor = TonalPalette.brightness(
        paletteColors.firstOrNull() ?: Color(0xFFA732FF),
        brightness,
    )
    val secondaryColor = TonalPalette.brightness(
        paletteColors.getOrNull(1) ?: Color(0xFFB388FF),
        brightness,
    )
    val baseColor = if (isActive) Color(0xFF2E1065) else Color(0xFF1A0038)
    val rayPrimaryCount = when (quality) {
        GenartQuality.Low -> 6
        GenartQuality.Medium -> 8
        GenartQuality.High -> 10
    }
    val raySecondaryCount = when (quality) {
        GenartQuality.Low -> 4
        GenartQuality.Medium -> 6
        GenartQuality.High -> 8
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width.coerceAtLeast(size.height) * 1.2f
        val center = Offset(centerX, centerY)

        drawRect(color = Color(0xFF0D001A))

        rotate(rotation, pivot = center) {
            for (i in 0 until rayPrimaryCount) {
                val angle = i * 360f / rayPrimaryCount
                rotate(angle, pivot = center) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isActive) 0.15f else 0.08f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxRadius * pulseScale,
                        ),
                        alpha = 0.6f,
                    )
                    val rayWidth = size.width * 0.15f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = if (isActive) 0.1f else 0.05f),
                                Color.Transparent,
                            ),
                            startX = centerX - rayWidth,
                            endX = centerX + rayWidth,
                        ),
                        topLeft = Offset(centerX - rayWidth, centerY - maxRadius),
                        size = Size(rayWidth * 2, maxRadius * 2),
                    )
                }
            }
        }

        rotate(-rotation * 0.7f, pivot = center) {
            for (i in 0 until raySecondaryCount) {
                val angle = i * 360f / raySecondaryCount + 15f
                rotate(angle, pivot = center) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = if (isActive) 0.12f else 0.06f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxRadius * 0.8f * pulseScale,
                        ),
                        alpha = 0.5f,
                    )
                }
            }
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = if (isActive) 0.8f else 0.4f),
                    Color.Transparent,
                    Color(0xFF0D001A).copy(alpha = 0.7f),
                ),
                center = center,
                radius = maxRadius * 0.7f,
            ),
        )
    }
}
