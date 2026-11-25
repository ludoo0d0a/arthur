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

/** One full primary rotation at speed 1.0 ≈ 11s (scales ~8–14s with user speed). */
private const val MICRO_RAY_LOOP_MS = 11000f
private const val MICRO_RAY_WIDTH_FRAC = 0.22f
private const val MICRO_RAY_PRIMARY_ALPHA_ACTIVE = 0.26f
private const val MICRO_RAY_PRIMARY_ALPHA_IDLE = 0.14f
private const val MICRO_RAY_STREAK_ALPHA_ACTIVE = 0.22f
private const val MICRO_RAY_STREAK_ALPHA_IDLE = 0.11f
private const val MICRO_RAY_SECONDARY_ALPHA_ACTIVE = 0.18f
private const val MICRO_RAY_SECONDARY_ALPHA_IDLE = 0.09f

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
            animation = tween((MICRO_RAY_LOOP_MS / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
                                primaryColor.copy(
                                    alpha = if (isActive) MICRO_RAY_PRIMARY_ALPHA_ACTIVE else MICRO_RAY_PRIMARY_ALPHA_IDLE,
                                ),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxRadius * pulseScale,
                        ),
                        alpha = 0.78f,
                    )
                    val rayWidth = size.width * MICRO_RAY_WIDTH_FRAC
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(
                                    alpha = if (isActive) MICRO_RAY_STREAK_ALPHA_ACTIVE else MICRO_RAY_STREAK_ALPHA_IDLE,
                                ),
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
                                secondaryColor.copy(
                                    alpha = if (isActive) MICRO_RAY_SECONDARY_ALPHA_ACTIVE else MICRO_RAY_SECONDARY_ALPHA_IDLE,
                                ),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxRadius * 0.8f * pulseScale,
                        ),
                        alpha = 0.65f,
                    )
                }
            }
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = if (isActive) 0.52f else 0.32f),
                    Color.Transparent,
                    Color(0xFF0D001A).copy(alpha = 0.38f),
                ),
                center = center,
                radius = maxRadius * 0.85f,
            ),
        )
    }
}
