package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Energy Tendrils for Auto/Ambient album art. */
internal object EnergyTendrilsStill {
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
        val dim = 0.65f + 0.35f * pulse
        val ox = w * 0.28f
        val oy = h * 0.72f
        val cyan = 0xFF3DFFF0.toInt()
        val magenta = 0xFFFF2EC8.toInt()
        val violet = 0xFFA855FF.toInt()
        val ice = 0xFFF4FBFF.toInt()

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF01020A.toInt(), 0xFF050818.toInt(), 0xFF0A1A28.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.shader = LinearGradient(
            0f, h * 0.45f, 0f, h,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((0.32f * dim * 255).toInt(), Color.red(cyan), Color.green(cyan), Color.blue(cyan)),
                Color.argb((0.55f * dim * 255).toInt(), 10, 48, 64),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.45f, w, h, paint)
        paint.shader = null

        val coreR = minDim * 0.2f * 2.4f
        paint.shader = RadialGradient(
            ox, oy, coreR.coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.7f * dim * 255).toInt(), Color.red(ice), Color.green(ice), Color.blue(ice)),
                Color.argb((0.4f * dim * 255).toInt(), Color.red(magenta), Color.green(magenta), Color.blue(magenta)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(ox, oy, coreR, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 7) {
            val baseAngle = -PI.toFloat() * 0.5f + (rnd.nextFloat() - 0.5f) * 2.2f
            val lengthFrac = 0.45f + rnd.nextFloat() * 0.4f
            val curve = (rnd.nextFloat() - 0.5f) * 1.1f
            val thickness = 3.5f + rnd.nextFloat() * 5f
            val accent = if (rnd.nextFloat() > 0.5f) magenta else violet
            val tint = palette.colorAt(i)
            val r = (Color.red(accent) + Color.red(tint)) / 2
            val g = (Color.green(accent) + Color.green(tint)) / 2
            val b = (Color.blue(accent) + Color.blue(tint)) / 2
            val len = minDim * lengthFrac
            val path = Path()
            val steps = 28
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val ang = baseAngle + curve * frac * frac
                val rr = len * frac
                val noise = sin(frac * 6f + loop * 2f * PI.toFloat()) * minDim * 0.012f * frac
                val px = ox + cos(ang) * rr + cos(ang + PI.toFloat() * 0.5f) * noise
                val py = oy + sin(ang) * rr + sin(ang + PI.toFloat() * 0.5f) * noise
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val a = (0.45f * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb((a * 0.35f).toInt().coerceIn(0, 255), r, g, b)
            paint.strokeWidth = thickness * 2.2f
            canvas.drawPath(path, paint)
            paint.shader = LinearGradient(
                ox, oy, ox + cos(baseAngle) * len, oy + sin(baseAngle) * len,
                Color.argb(a, Color.red(ice), Color.green(ice), Color.blue(ice)),
                Color.argb((a * 0.35f).toInt(), Color.red(violet), Color.green(violet), Color.blue(violet)),
                Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = thickness * 0.55f
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        paint.style = Paint.Style.FILL
        for (i in 0 until 40) {
            paint.color = Color.argb(
                ((0.2f + rnd.nextFloat() * 0.45f) * dim * 255).toInt().coerceIn(0, 255),
                Color.red(ice), Color.green(ice), Color.blue(ice),
            )
            canvas.drawCircle(
                (0.1f + rnd.nextFloat() * 0.8f) * w,
                (0.05f + rnd.nextFloat() * 0.7f) * h,
                0.8f + rnd.nextFloat() * 1.6f,
                paint,
            )
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 12) {
            val x = (0.08f + rnd.nextFloat() * 0.84f) * w
            val gw = (0.02f + rnd.nextFloat() * 0.04f) * w
            paint.color = Color.argb(
                ((0.12f + rnd.nextFloat() * 0.16f) * dim * 255).toInt().coerceIn(0, 255),
                Color.red(cyan), Color.green(cyan), Color.blue(cyan),
            )
            canvas.drawLine(x, h * 0.92f, x + gw, h * 0.92f, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
