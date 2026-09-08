package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Soft Day-Night Wash" for Auto/Ambient album art. */
internal object DayNightWashStill {
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

        val dayNightT = ((sin(phase * 2.0 - PI / 2.0) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)

        val dayTop = palette.primary
        val dayBot = palette.secondary
        val nightTop = 0xFF060814.toInt()
        val nightBot = 0xFF01020A.toInt()

        val skyTop = mixColor(dayTop, nightTop, dayNightT)
        val skyBot = mixColor(dayBot, nightBot, dayNightT)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            skyTop, skyBot,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val starCount = 18
        val nightAmount = dayNightT
        if (nightAmount > 0.02f) {
            paint.style = Paint.Style.FILL
            for (i in 0 until starCount) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val radius = 1.0f + rnd.nextFloat() * 1.6f
                val twinkleFreq = 0.3f + rnd.nextFloat() * 0.7f
                val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
                val twinkle = 0.5f + 0.5f * sin(phase * twinkleFreq + phaseOffset)
                val tint = palette.colorAt(i)
                val alpha = nightAmount * (0.25f + 0.55f * twinkle) * (0.6f + 0.4f * pulse)
                paint.color = Color.argb(
                    (alpha * 255).toInt().coerceIn(0, 255),
                    Color.red(tint),
                    Color.green(tint),
                    Color.blue(tint),
                )
                canvas.drawCircle(x, y, radius, paint)
            }
        }
        paint.alpha = 255
    }

    private fun mixColor(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = Color.red(a) + (Color.red(b) - Color.red(a)) * u
        val g = Color.green(a) + (Color.green(b) - Color.green(a)) * u
        val bl = Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u
        return Color.argb(255, r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), bl.toInt().coerceIn(0, 255))
    }
}
