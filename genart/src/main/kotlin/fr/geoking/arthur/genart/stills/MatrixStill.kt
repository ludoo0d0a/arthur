package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val MATRIX_CHARS = "ｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ0123456789XYZ:*+-<>="

/** Bakes one frozen frame of Matrix Rain for Auto/Ambient album art. */
internal object MatrixStill {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()

        // Dark background
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF030A05.toInt(), 0xFF010402.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        bgPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse
        val columnCount = 36

        for (i in 0 until columnCount) {
            val xFrac = (i + 0.5f) / columnCount + (rnd.nextFloat() - 0.5f) * 0.02f
            val yOffset = rnd.nextFloat()
            val fallSpeed = 0.45f + rnd.nextFloat() * 0.8f
            val length = 10 + rnd.nextInt(15)
            val fontSize = (0.024f + rnd.nextFloat() * 0.014f) * h
            val x = xFrac * w
            val charSpacing = fontSize * 1.08f

            val headCycle = ((yOffset + fallSpeed * (drift / (2f * PI.toFloat()))) % 1.4f + 1.4f) % 1.4f
            val headY = (headCycle - 0.2f) * h

            textPaint.textSize = fontSize
            glowPaint.textSize = fontSize

            val tint = palette.colorAt(i)
            val tintRed = Color.red(tint)
            val tintGreen = Color.green(tint)
            val tintBlue = Color.blue(tint)

            for (cIdx in 0 until length) {
                val cy = headY - cIdx * charSpacing
                if (cy < -fontSize || cy > h + fontSize) continue

                val charPos = rnd.nextInt(MATRIX_CHARS.length)
                val charStr = MATRIX_CHARS[charPos].toString()
                val fade = 1f - (cIdx.toFloat() / length)

                if (cIdx == 0) {
                    // Head character: glowing green-white
                    val glowColor = 0xFF00FF66.toInt()

                    haloPaint.color = Color.argb(
                        (0.35f * dim * 255).toInt().coerceIn(0, 255),
                        0, 255, 102,
                    )
                    canvas.drawCircle(x, cy - fontSize * 0.3f, fontSize * 0.9f, haloPaint)

                    glowPaint.color = Color.argb(
                        (0.85f * dim * 255).toInt().coerceIn(0, 255),
                        0, 255, 102,
                    )
                    glowPaint.setShadowLayer(fontSize * 0.6f, 0f, 0f, glowColor)
                    canvas.drawText(charStr, x, cy, glowPaint)

                    textPaint.color = Color.argb(
                        (dim * 255).toInt().coerceIn(0, 255),
                        ((226 + tintRed) / 2).coerceIn(0, 255),
                        ((255 + tintGreen) / 2).coerceIn(0, 255),
                        ((236 + tintBlue) / 2).coerceIn(0, 255),
                    )
                    textPaint.clearShadowLayer()
                    canvas.drawText(charStr, x, cy, textPaint)
                } else {
                    // Stream tail characters: vivid green fading to dark green
                    val alpha = (fade * fade * dim * 0.9f).coerceIn(0.04f, 1f)
                    val r = ((0 + (0 * (1f - fade))) * 0.85f + tintRed * 0.15f).toInt().coerceIn(0, 255)
                    val g = (((255 * fade) + (56 * (1f - fade))) * 0.85f + tintGreen * 0.15f).toInt().coerceIn(0, 255)
                    val b = (((85 * fade) + (20 * (1f - fade))) * 0.85f + tintBlue * 0.15f).toInt().coerceIn(0, 255)

                    textPaint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
                    textPaint.clearShadowLayer()

                    if (cIdx < 3) {
                        textPaint.setShadowLayer(fontSize * 0.3f, 0f, 0f, 0xFF00FF41.toInt())
                    }
                    canvas.drawText(charStr, x, cy, textPaint)
                }
            }
        }
    }
}
