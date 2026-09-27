package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Plasma Nova for Auto/Ambient album art. */
internal object PlasmaNovaStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.48f
        val dim = 0.65f + 0.35f * pulse
        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val expand = when {
            loop < 0.35f -> loop / 0.35f
            loop < 0.7f -> 1f
            else -> (1f - (loop - 0.7f) / 0.3f).coerceIn(0.35f, 1f)
        }
        val reach = hypot(w, h) * 0.42f * expand

        paint.shader = null
        paint.color = 0xFF010106.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        // Stars
        paint.color = Color.WHITE
        for (i in 0 until 70) {
            val alpha = (40 + rnd.nextInt(140))
            paint.alpha = (alpha * dim).toInt().coerceIn(0, 255)
            canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, 0.5f + rnd.nextFloat() * 1.3f, paint)
        }
        paint.alpha = 255

        val magenta = 0xFFFF2EB8.toInt()
        val cyan = 0xFF4EFFF8.toInt()
        val violet = 0xFFB44DFF.toInt()
        val ice = 0xFFF7FCFF.toInt()

        paint.shader = RadialGradient(
            cx, cy, (reach * 1.15f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.35f * dim * 255).toInt(), Color.red(magenta), Color.green(magenta), Color.blue(magenta)),
                Color.argb((0.2f * dim * 255).toInt(), Color.red(violet), Color.green(violet), Color.blue(violet)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, reach * 1.15f, paint)
        paint.shader = null

        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        for (i in 0 until 72) {
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val lengthMul = 0.35f + rnd.nextFloat() * 0.8f
            val wobble = (rnd.nextFloat() - 0.5f) * 0.4f
            val len = reach * lengthMul
            val accent = when {
                rnd.nextFloat() > 0.7f -> cyan
                rnd.nextFloat() > 0.35f -> magenta
                else -> violet
            }
            val tint = palette.colorAt(i)
            val r = (Color.red(accent) + Color.red(tint)) / 2
            val g = (Color.green(accent) + Color.green(tint)) / 2
            val b = (Color.blue(accent) + Color.blue(tint)) / 2
            val ex = cx + cos(angle + wobble) * len
            val ey = cy + sin(angle + wobble) * len
            val a = (0.45f * dim * 255).toInt().coerceIn(0, 255)
            paint.strokeWidth = 0.9f + rnd.nextFloat() * 1.9f
            paint.shader = LinearGradient(
                cx, cy, ex, ey,
                Color.argb(a, Color.red(ice), Color.green(ice), Color.blue(ice)),
                Color.argb(0, r, g, b),
                Shader.TileMode.CLAMP,
            )
            canvas.drawLine(cx, cy, ex, ey, paint)
            paint.shader = null
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.014f
        paint.shader = LinearGradient(
            0f, cy, w, cy,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((0.14f * dim * 255).toInt(), Color.red(magenta), Color.green(magenta), Color.blue(magenta)),
                Color.argb((0.28f * dim * 255).toInt(), Color.red(ice), Color.green(ice), Color.blue(ice)),
                Color.argb((0.14f * dim * 255).toInt(), Color.red(cyan), Color.green(cyan), Color.blue(cyan)),
                Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawLine(0f, cy, w, cy, paint)
        paint.shader = null

        val coreR = minDim * (0.1f + 0.04f * expand) * 2.2f
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, coreR.coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.85f * dim * 255).toInt(), Color.red(ice), Color.green(ice), Color.blue(ice)),
                Color.argb((0.45f * dim * 255).toInt(), Color.red(cyan), Color.green(cyan), Color.blue(cyan)),
                Color.argb((0.2f * dim * 255).toInt(), Color.red(magenta), Color.green(magenta), Color.blue(magenta)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coreR, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
