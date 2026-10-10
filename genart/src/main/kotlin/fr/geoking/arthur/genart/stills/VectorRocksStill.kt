package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Vector Rocks for Auto/Ambient album art. */
internal object VectorRocksStill {
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
        val time = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(5, 7, 12)
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        for (i in 0 until 8) {
            val sides = 5 + rnd.nextInt(6)
            val cx = ((rnd.nextFloat() + rnd.nextFloat() * 0.35f * time) % 1.3f)
            val cy = ((rnd.nextFloat() + rnd.nextFloat() * 0.3f * time) % 1.3f)
            val x = (cx - 0.15f) * w
            val y = (cy - 0.15f) * h
            val r = minDim * (0.04f + rnd.nextFloat() * 0.07f)
            val angle = time * (0.1f + rnd.nextFloat() * 0.35f) * 2f * PI.toFloat() + rnd.nextFloat() * 2f * PI.toFloat()
            val tint = palette.colorAt(i)
            val wr = (0xE8 * 0.8f + Color.red(tint) * 0.2f).toInt().coerceIn(0, 255)
            val wg = (0xF0 * 0.8f + Color.green(tint) * 0.2f).toInt().coerceIn(0, 255)
            val wb = (0xFF * 0.8f + Color.blue(tint) * 0.2f).toInt().coerceIn(0, 255)
            paint.color = Color.argb((180 * dim).toInt(), wr, wg, wb)
            path.reset()
            for (s in 0..sides) {
                val a = angle + s * 2f * PI.toFloat() / sides
                val jr = r * (0.65f + rnd.nextFloat() * 0.5f)
                val px = x + cos(a) * jr
                val py = y + sin(a) * jr
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((20 * dim).toInt(), wr, wg, wb)
            canvas.drawCircle(x, y, r * 0.35f, paint)
            paint.style = Paint.Style.STROKE
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
