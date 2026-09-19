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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.sin

private const val MATRIX_CHARS = "ｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ0123456789XYZ:*+-<>="

/** Matrix rain engine — vertical digital streams with glowing head shadow green effects. */
@Composable
internal fun MatrixEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val columnCount = qualityCount(quality, low = 22, medium = 38, high = 56)
    val columns = remember(columnCount) {
        List(columnCount) { i ->
            val charLen = seededRange(i * 19 + 5, 10f, 24f).toInt()
            val initialChars = CharArray(charLen) { cIdx ->
                val charPos = (seededUnit(i * 37 + cIdx * 11) * MATRIX_CHARS.length).toInt() % MATRIX_CHARS.length
                MATRIX_CHARS[charPos]
            }
            MatrixColumnSeed(
                xFrac = (i + 0.5f) / columnCount + seededRange(i * 13 + 3, -0.015f, 0.015f),
                yOffset = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.45f, 1.25f),
                length = charLen,
                fontSizeFrac = seededRange(i * 53 + 13, 0.022f, 0.038f),
                mutateFreq = seededRange(i * 67 + 17, 1.5f, 4.5f),
                colorIndex = i,
                chars = initialChars,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "matrix")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "matrix_t",
    )

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }
    val glowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dark matrix digital background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF030A05), Color(0xFF010402)),
            ),
        )

        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas

            columns.forEach { col ->
                val fontSize = col.fontSizeFrac * h
                val x = col.xFrac * w
                val charSpacing = fontSize * 1.08f
                val headCycle = (col.yOffset + col.fallSpeed * time) % 1.4f
                val headY = (headCycle - 0.2f) * h

                textPaint.textSize = fontSize
                glowPaint.textSize = fontSize

                for (cIdx in 0 until col.length) {
                    val cy = headY - cIdx * charSpacing
                    if (cy < -fontSize || cy > h + fontSize) continue

                    // Slowly mutate character over time
                    val charMutationSeed = (time * col.mutateFreq + cIdx * 0.37f).toInt()
                    val charIndexInString = (col.chars[cIdx].code + charMutationSeed * 13) % MATRIX_CHARS.length
                    val charStr = MATRIX_CHARS[charIndexInString].toString()

                    val fade = 1f - (cIdx.toFloat() / col.length)

                    if (cIdx == 0) {
                        // Head character: bright green-white with shadow green glow
                        val baseHead = TonalPalette.mix(
                            Color(0xFFE2FFEC),
                            TonalPalette.pick(paletteColors, col.colorIndex),
                            0.2f,
                        )
                        val headColor = TonalPalette.brightness(baseHead, brightness)
                        val glowColor = Color(0xFF00FF66)

                        // Green shadow layer text
                        glowPaint.color = glowColor.copy(alpha = 0.85f * dim).toArgb()
                        glowPaint.setShadowLayer(fontSize * 0.6f, 0f, 0f, glowColor.toArgb())
                        canvas.drawText(charStr, x, cy, glowPaint)

                        // Sharp head text
                        textPaint.color = TonalPalette.withAlpha(headColor, dim).toArgb()
                        textPaint.clearShadowLayer()
                        canvas.drawText(charStr, x, cy, textPaint)
                    } else {
                        // Stream tail characters: vivid green fading to dark green
                        val tailBase = TonalPalette.mix(
                            Color(0xFF00FF55),
                            Color(0xFF003814),
                            1f - fade,
                        )
                        val tintedTail = TonalPalette.mix(tailBase, TonalPalette.pick(paletteColors, col.colorIndex), 0.15f)
                        val charColor = TonalPalette.brightness(tintedTail, brightness)
                        val alpha = (fade * fade * dim * 0.92f).coerceIn(0.04f, 1f)

                        textPaint.color = TonalPalette.withAlpha(charColor, alpha).toArgb()
                        textPaint.clearShadowLayer()

                        // Slight shadow effect on upper tail characters
                        if (cIdx < 3) {
                            textPaint.setShadowLayer(fontSize * 0.3f, 0f, 0f, Color(0xFF00FF41).toArgb())
                        }

                        canvas.drawText(charStr, x, cy, textPaint)
                    }
                }
            }
        }
    }
}

private data class MatrixColumnSeed(
    val xFrac: Float,
    val yOffset: Float,
    val fallSpeed: Float,
    val length: Int,
    val fontSizeFrac: Float,
    val mutateFreq: Float,
    val colorIndex: Int,
    val chars: CharArray,
)
