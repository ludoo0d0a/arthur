package fr.geoking.arthur.fractal

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mandelbrot / Julia canvases adapted from
 * [tbahlai/agsl](https://github.com/tbahlai/agsl) (ProAndroidDev AGSL article).
 *
 * API 33+ only — callers should fall back to the shared CPU path below that.
 * Zen pacing: very slow auto-zoom / orbit. Colors from [FractalCoherentPalette].
 */
@Composable
internal fun MandelbrotBahlaiCanvas(
    isActive: Boolean,
    colorSeed: Int = 1,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    var zoom by remember { mutableFloatStateOf(2.5f) }
    var center by remember { mutableStateOf(Offset(-0.5f, 0f)) }
    var userNavigating by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "mandelbrot_bahlai")
    val autoZoom by infiniteTransition.animateFloat(
        initialValue = 2.5f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            // Zen: ~2.5 min full zoom cycle
            animation = tween(150_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autoZoom",
    )
    val displayZoom = if (userNavigating) zoom else autoZoom

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (!isActive) return@rememberTransformableState
        userNavigating = true
        zoom = (zoom / zoomChange).coerceIn(0.00001f, 5f)
        val sensitivity = zoom / 1000f
        center = Offset(
            center.x - offsetChange.x * sensitivity,
            center.y + offsetChange.y * sensitivity,
        )
    }

    val shader = remember { FractalAgslShaders.createMandelbrotBahlaiShader() }
    val stops = remember(colorSeed) { FractalCoherentPalette.fourStops(colorSeed) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .drawWithCache {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color1", stops[0].toArgb())
                shader.setColorUniform("color2", stops[1].toArgb())
                shader.setColorUniform("color3", stops[2].toArgb())
                shader.setColorUniform("color4", stops[3].toArgb())
                val brush = ShaderBrush(shader)
                onDrawBehind {
                    shader.setFloatUniform("zoom", displayZoom)
                    shader.setFloatUniform("center", center.x, center.y)
                    drawRect(brush = brush)
                }
            },
    )
}

@Composable
internal fun JuliaBahlaiCanvas(
    isActive: Boolean,
    colorSeed: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    var mousePos by remember { mutableStateOf(Offset.Unspecified) }
    var dragging by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "julia_bahlai")
    val orbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Zen: ~2 min gentle orbit
            animation = tween(120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit",
    )

    val shader = remember { FractalAgslShaders.createJuliaBahlaiShader() }
    val stops = remember(colorSeed) { FractalCoherentPalette.fourStops(colorSeed) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        mousePos = change.position
                    },
                )
            }
            .drawWithCache {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color1", stops[0].toArgb())
                shader.setColorUniform("color2", stops[1].toArgb())
                shader.setColorUniform("color3", stops[2].toArgb())
                shader.setColorUniform("color4", stops[3].toArgb())
                val brush = ShaderBrush(shader)
                onDrawBehind {
                    val auto = Offset(
                        size.width * (0.5f + 0.22f * cos(orbit * 2f * PI.toFloat())),
                        size.height * (0.5f + 0.18f * sin(orbit * 2f * PI.toFloat() * 1.15f)),
                    )
                    val pos = when {
                        dragging && mousePos != Offset.Unspecified -> mousePos
                        mousePos != Offset.Unspecified && isActive -> mousePos
                        else -> auto
                    }
                    shader.setFloatUniform("mouse", pos.x, pos.y)
                    drawRect(brush = brush)
                }
            },
    )
}
