package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one frozen frame of Glass Tiles for Auto/Ambient album art. */
internal object GlassTilesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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

        paint.color = 0xFF040508.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val cols = 4
        val rows = 5
        val gapX = w * 0.04f
        val gapY = h * 0.03f
        val tileW = (w - gapX * (cols + 1)) / cols
        val tileH = (h - gapY * (rows + 1)) / rows
        val dim = 0.65f + 0.35f * pulse

        val rect = RectF()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val x = gapX + c * (tileW + gapX)
                val y = gapY + r * (tileH + gapY)
                val baseAlpha = 0.35f + rnd.nextFloat() * 0.4f
                val cornerRadius = 16f

                // Shadow
                rect.set(x + tileW * 0.08f, y + tileH * 0.08f, x + tileW * 1.08f, y + tileH * 1.08f)
                paint.color = Color.argb((0.45f * dim * 255).toInt(), 0, 0, 0)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                // Glass Body Gradient
                rect.set(x, y, x + tileW, y + tileH)
                val color = palette.colorAt(idx)
                val alpha1 = (baseAlpha * dim * 255).toInt().coerceIn(0, 255)
                val alpha2 = (baseAlpha * 0.4f * dim * 255).toInt().coerceIn(0, 255)

                paint.shader = LinearGradient(
                    x, y, x + tileW, y + tileH,
                    Color.argb(alpha1, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha2, Color.red(color), Color.green(color), Color.blue(color)),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                paint.shader = null
            }
        }
    }
}
