package fr.geoking.arthur.genart.engines

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.floor

/**
 * Soft CRT boot wash: scanlines, blinking prompt cursor, calm phosphor — no flicker/strobe.
 */
@Composable
internal fun CrtBootEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val scanCount = qualityCount(quality, low = 36, medium = 48, high = 64)
    val lineCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val transition = rememberInfiniteTransition(label = "crtboot")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crtboot_t",
    )

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
        }
    }

    val dim = if (isActive) 1f else 0.55f
    val phosphor = TonalPalette.mix(Color(0xFF66FF88), TonalPalette.pick(paletteColors, 0), 0.2f)
    val cycle = phase01(t)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF0A1810), brightness * dim),
                    TonalPalette.brightness(Color(0xFF041008), brightness * dim),
                    Color(0xFF020804),
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.75f,
            ),
        )

        // Soft vignette wash (no strobe)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(phosphor, 0.04f * dim),
                    Color.Transparent,
                    TonalPalette.withAlpha(phosphor, 0.03f * dim),
                ),
            ),
        )

        val green = TonalPalette.brightness(phosphor, brightness * dim)
        val lines = listOf(
            "SYSTEM READY",
            "MEMORY CHECK .......... OK",
            "DISPLAY INIT ........... OK",
            "AUDIO BUS .............. IDLE",
            "AMBIENT LINK ........... WAIT",
            "LOADING PALETTE ........ DONE",
            "SESSION ................ OPEN",
            ">_",
        ).take(lineCount.coerceAtMost(8))

        val reveal = floor(cycle * (lines.size + 1f)).toInt().coerceIn(0, lines.size)
        val fontSize = h * 0.028f
        textPaint.textSize = fontSize
        val lineGap = fontSize * 1.55f
        val startY = h * 0.28f
        val startX = w * 0.12f

        drawIntoCanvas { composeCanvas ->
            val native = composeCanvas.nativeCanvas
            for (i in 0 until reveal) {
                val line = lines[i]
                val isPrompt = line.startsWith(">")
                val cursorOn = !isPrompt || sin01(cycle * 4f * PI.toFloat()) > 0.35f
                val text = if (isPrompt && !cursorOn) ">" else line
                textPaint.color = TonalPalette.withAlpha(green, 0.85f * dim).toArgb()
                native.drawText(text, startX, startY + i * lineGap, textPaint)
            }
            // Soft block cursor next to last revealed line when prompt
            if (reveal > 0 && lines.getOrNull(reveal - 1)?.startsWith(">") == true) {
                val cursorAlpha = 0.25f + 0.55f * sin01(cycle * 4f * PI.toFloat())
                textPaint.color = TonalPalette.withAlpha(green, cursorAlpha * dim).toArgb()
                val cx = startX + fontSize * 0.7f
                val cy = startY + (reveal - 1) * lineGap
                native.drawRect(cx, cy - fontSize * 0.85f, cx + fontSize * 0.55f, cy + fontSize * 0.1f, textPaint)
            }
        }

        // Soft scanlines (alpha ~0.04)
        val scanStep = h / scanCount.toFloat()
        var sy = 0f
        while (sy < h) {
            drawLine(
                color = TonalPalette.withAlpha(green, 0.04f * dim),
                start = Offset(0f, sy),
                end = Offset(w, sy),
                strokeWidth = 1f,
            )
            sy += scanStep
        }
    }
}
