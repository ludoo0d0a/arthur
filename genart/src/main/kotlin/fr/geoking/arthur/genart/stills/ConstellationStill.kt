package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one static frame of the Constellation Twinkle starfield for Auto album art. */
internal object ConstellationStill {
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
        val minDim = minOf(w, h)

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            0xFF07040F.toInt(),
            0xFF050914.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val count = 26
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val radii = FloatArray(count)
        val freqs = FloatArray(count)
        val phases = FloatArray(count)
        val colorIndices = IntArray(count)
        for (i in 0 until count) {
            xs[i] = rnd.nextFloat() * w
            ys[i] = rnd.nextFloat() * h
            radii[i] = 1.2f + rnd.nextFloat() * 2f
            freqs[i] = 0.4f + rnd.nextFloat() * 1.2f
            phases[i] = rnd.nextFloat() * 2f * PI.toFloat()
            colorIndices[i] = i
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.75f
        for (i in 0 until count) {
            if (i % 2 != 0) continue
            val j = (i + 1) % count
            val c = palette.primary
            paint.color = Color.argb(
                (0.09f * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawLine(xs[i], ys[i], xs[j], ys[j], paint)
        }

        paint.style = Paint.Style.FILL
        for (i in 0 until count) {
            val twinkle = 0.5f + 0.5f * sin(phase * freqs[i] + phases[i])
            val c = palette.colorAt(colorIndices[i])
            val alpha = (0.25f + 0.65f * twinkle).coerceIn(0f, 1f)
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawCircle(xs[i], ys[i], radii[i], paint)
        }

        // One shooting star pose.
        val dim = 0.7f + 0.3f * pulse
        val angle = 0.45f + rnd.nextFloat() * 0.25f
        val startX = 0.1f + rnd.nextFloat() * 0.3f
        val startY = 0.08f + rnd.nextFloat() * 0.2f
        val lengthFrac = 0.22f
        val life = 0.45f
        val travel = life * lengthFrac * 2.4f
        val x = (startX + cos(angle) * travel) * w
        val y = (startY + sin(angle) * travel) * h
        val trail = lengthFrac * minDim
        val sx = x - cos(angle) * trail
        val sy = y - sin(angle) * trail
        val tint = palette.colorAt(5)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 2.4f
        paint.shader = LinearGradient(
            sx, sy, x, y,
            Color.TRANSPARENT,
            Color.argb((0.8f * dim * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 255) / 2,
                (Color.green(tint) + 255) / 2,
                (Color.blue(tint) + 255) / 2),
            Shader.TileMode.CLAMP,
        )
        canvas.drawLine(sx, sy, x, y, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((0.9f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(x, y, 3f, paint)

        // Planet with terminator + optional ring.
        val px = (0.62f + rnd.nextFloat() * 0.18f) * w
        val py = (0.55f + rnd.nextFloat() * 0.2f) * h
        val pr = (0.055f + rnd.nextFloat() * 0.03f) * minDim
        val planetTint = palette.colorAt(3)
        canvas.save()
        canvas.rotate(rotationDeg * 0.4f + phase * 40f, px, py)
        if (rnd.nextFloat() > 0.35f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = pr * 0.12f
            paint.color = Color.argb(
                (0.35f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(planetTint),
                Color.green(planetTint),
                Color.blue(planetTint),
            )
            canvas.drawOval(RectF(px - pr * 1.7f, py - pr * 0.28f, px + pr * 1.7f, py + pr * 0.28f), paint)
        }
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            px - pr * 0.25f, py - pr * 0.2f, pr * 1.15f,
            intArrayOf(
                Color.argb((0.95f * dim * 255).toInt().coerceIn(0, 255),
                    (Color.red(planetTint) * 1.15f).toInt().coerceIn(0, 255),
                    (Color.green(planetTint) * 1.15f).toInt().coerceIn(0, 255),
                    (Color.blue(planetTint) * 1.15f).toInt().coerceIn(0, 255)),
                Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255),
                    Color.red(planetTint), Color.green(planetTint), Color.blue(planetTint)),
                Color.argb((0.9f * dim * 255).toInt().coerceIn(0, 255), 26, 16, 40),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(px, py, pr, paint)
        paint.shader = RadialGradient(
            px + pr * 0.35f, py + pr * 0.1f, pr * 0.95f,
            intArrayOf(Color.TRANSPARENT, Color.argb((0.45f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)),
            floatArrayOf(0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(px, py, pr, paint)
        paint.shader = null
        canvas.restore()
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
