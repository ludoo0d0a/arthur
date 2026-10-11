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
import fr.geoking.arthur.genart.stills.PpssppCelShaderStill

/** PPSSPP Cel-Shader — live Compose wrapper over the Android Canvas still draw. */
@Composable
internal fun PpssppCelShaderEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val palette = remember(paletteColors) {
        AnimationPalette(
            name = "genart",
            colors = paletteColors.map { it.toArgb() }.ifEmpty {
                listOf(0xFF6366F1.toInt(), 0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt())
            },
        )
    }
    val transition = rememberInfiniteTransition(label = "ppssppcelshader")
    val elapsedSec by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween((3600000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ppssppcelshader_t",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((2000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ppssppcelshader_pulse",
    )
    // brightness reserved for Ambient dimming via isActive; quality reserved for future grid scaling
    @Suppress("UNUSED_PARAMETER")
    val unused = quality to brightness

    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            PpssppCelShaderStill.draw(
                canvas = canvas,
                size = maxOf(w, h),
                width = w,
                height = h,
                generation = 0L,
                phase = elapsedSec,
                rotationDeg = 0f,
                pulse = pulse,
                palette = palette,
                isActive = isActive,
            )
        }
    }
}
