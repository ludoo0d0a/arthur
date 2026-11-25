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
import fr.geoking.arthur.genart.stills.PAPER_GRAIN_GRADIENT_LOOP_SEC
import fr.geoking.arthur.genart.stills.PaperGrainGradientStill

/** Paper Grain Gradient — live Compose wrapper over the Android Canvas still draw. */
@Composable
internal fun PaperGrainGradientEngine(
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
    val effectiveSpeed = speed.coerceAtLeast(0.2f)
    val loopMs = (PAPER_GRAIN_GRADIENT_LOOP_SEC * 1000f / effectiveSpeed).toInt()
    val transition = rememberInfiniteTransition(label = "papergraingradient")
    val elapsedSec by transition.animateFloat(
        initialValue = 0f,
        targetValue = PAPER_GRAIN_GRADIENT_LOOP_SEC,
        animationSpec = infiniteRepeatable(
            animation = tween(loopMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "papergraingradient_t",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(loopMs / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "papergraingradient_pulse",
    )
    @Suppress("UNUSED_PARAMETER")
    val unused = quality to brightness

    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            PaperGrainGradientStill.draw(
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