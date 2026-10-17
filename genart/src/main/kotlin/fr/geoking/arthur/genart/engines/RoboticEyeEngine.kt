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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.stills.RoboticEyeStill

/**
 * Cybernetic Robotic Eye — Terminator / M3GAN 2.0 synthetic eye with mechanical iris blades,
 * vibrating micro-saccade pupil, and HUD tech animations.
 */
@Composable
internal fun RoboticEyeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val palette = remember(paletteColors) {
        AnimationPalette(
            name = "roboticeye",
            colors = paletteColors.map { it.toArgb() }.ifEmpty {
                listOf(0xFFFF0033.toInt(), 0xFFFF2244.toInt(), 0xFFFF6600.toInt())
            },
        )
    }
    val transition = rememberInfiniteTransition(label = "roboticeye")
    val elapsedSec by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween((3600000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "roboticeye_t",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((1800 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "roboticeye_pulse",
    )

    @Suppress("UNUSED_PARAMETER")
    val _unused = quality to brightness

    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            RoboticEyeStill.draw(
                canvas = canvas,
                size = maxOf(w, h),
                width = w,
                height = h,
                generation = 0L,
                phase = elapsedSec,
                rotationDeg = 0f,
                pulse = if (isActive) pulse else 0.5f,
                palette = palette,
            )
        }
    }
}
