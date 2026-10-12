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
import fr.geoking.arthur.genart.stills.MilkyWayStill

/** Milky Way Galaxy — live Compose engine wrapper around [MilkyWayStill]. */
@Composable
internal fun MilkyWayEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val palette = remember(paletteColors) {
        AnimationPalette(
            name = "milkyway",
            colors = paletteColors.map { it.toArgb() }.ifEmpty {
                listOf(0xFF9B9BFF.toInt(), 0xFF5B3BFF.toInt(), 0xFFFFD700.toInt())
            },
        )
    }
    val effectiveSpeed = speed.coerceAtLeast(0.15f)
    val transition = rememberInfiniteTransition(label = "milkyway")
    val phaseSec by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween((3600000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "milkyway_phase",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((3000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "milkyway_pulse",
    )

    @Suppress("UNUSED_VARIABLE")
    val dim = brightness

    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            MilkyWayStill.draw(
                canvas = canvas,
                size = maxOf(w, h),
                width = w,
                height = h,
                generation = 42L,
                phase = phaseSec,
                rotationDeg = 0f,
                pulse = pulse,
                palette = palette,
                quality = quality,
                isActive = isActive,
            )
        }
    }
}
