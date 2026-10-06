package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Crystal Prism Cave for Auto/Ambient album art. */
internal object PrismCaveStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val time = phase + rotationDeg * 0.01f

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.4f, minDim * 1.1f,
            intArrayOf(0xFF14101C.toInt(), 0xFF07060C.toInt(), 0xFF020208.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        repeat(4) { i ->
            val ox = (0.15f + rnd.nextFloat() * 0.7f) * w
            val oy = rnd.nextFloat() * 0.3f * h
            val angle = 0.5f + rnd.nextFloat() * 2f + 0.03f * sin(time + i)
            val half = 0.06f + rnd.nextFloat() * 0.04f
            val reach = h * 1.2f
            val tint = palette.colorAt(i)
            path.reset()
            path.moveTo(ox, oy)
            path.lineTo(ox + sin(angle - half) * reach, oy + cos(angle - half) * reach)
            path.lineTo(ox + sin(angle + half) * reach, oy + cos(angle + half) * reach)
            path.close()
            paint.shader = LinearGradient(
                ox, oy, ox + sin(angle) * reach * 0.5f, oy + cos(angle) * reach * 0.5f,
                intArrayOf(
                    Color.argb((70 * dim).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        repeat(14) { i ->
            val cx = rnd.nextFloat() * w
            val cy = rnd.nextFloat() * h
            val pulseMul = 1f + 0.03f * sin(time + i)
            val rx = (0.05f + rnd.nextFloat() * 0.07f) * minDim * pulseMul
            val ry = (0.06f + rnd.nextFloat() * 0.08f) * minDim * pulseMul
            val tint = palette.colorAt(i)
            val light = mixArgb(tint, Color.WHITE, 0.35f)
            val dark = mixArgb(tint, 0xFF101018.toInt(), 0.45f)
            path.reset()
            path.moveTo(cx - rx, cy)
            path.lineTo(cx, cy - ry)
            path.lineTo(cx + rx, cy)
            path.lineTo(cx, cy + ry)
            path.close()
            paint.shader = LinearGradient(
                cx - rx, cy - ry, cx + rx, cy + ry,
                intArrayOf(withAlpha(light, (110 * dim).toInt()), withAlpha(dark, (120 * dim).toInt())),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        paint.color = Color.argb((75 * dim).toInt(), 232, 240, 255)
        repeat(45) {
            canvas.drawCircle(
                rnd.nextFloat() * w,
                rnd.nextFloat() * h,
                (0.0015f + rnd.nextFloat() * 0.003f) * minDim,
                paint,
            )
        }
    }

    private fun withAlpha(argb: Int, alpha: Int): Int =
        (argb and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }
}
