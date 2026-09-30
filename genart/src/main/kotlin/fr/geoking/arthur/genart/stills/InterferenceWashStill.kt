package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen interference-wash frame for Auto/Ambient album art. */
internal object InterferenceWashStill {
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
        val t = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f + rnd.nextFloat() * 0.1f)
        val energy = 0.7f + 0.3f * pulse

        val c0 = palette.colors.getOrElse(0) { 0xFF0EA5E9.toInt() }
        val c1 = palette.colors.getOrElse(1) { 0xFFA78BFA.toInt() }
        val c2 = palette.colors.getOrElse(2) { 0xFFF472B6.toInt() }

        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.45f,
            maxOf(w, h) * 0.85f,
            intArrayOf(Color.argb(90, Color.red(c0), Color.green(c0), Color.blue(c0)), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cols = 56
        val rows = 56
        val cellW = w / cols
        val cellH = h / rows
        val bands = 4
        for (iy in 0 until rows) {
            for (ix in 0 until cols) {
                val u = ix / cols.toFloat()
                val v = iy / rows.toFloat()
                var field = 0f
                for (k in 0 until bands) {
                    val fx = 2.2f + k * 1.15f
                    val fy = 1.7f + k * 0.9f
                    val p = t * (2f * PI.toFloat()) * (0.35f + k * 0.08f) + k * 1.7f
                    field += sin((u * fx + v * fy) * 2f * PI.toFloat() + p)
                    field += cos((u * fy - v * fx) * 2f * PI.toFloat() - p * 0.7f) * 0.65f
                }
                val n = ((field / (bands * 1.65f)) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val rgb = if (n < 0.5f) lerpArgb(c0, c1, n * 2f) else lerpArgb(c1, c2, (n - 0.5f) * 2f)
                val alpha = ((0.2f + 0.6f * n) * energy * 255f).toInt().coerceIn(0, 255)
                paint.color = Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
                canvas.drawRect(ix * cellW, iy * cellH, (ix + 1) * cellW + 1f, (iy + 1) * cellH + 1f, paint)
            }
        }
    }

    private fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt()
        return Color.rgb(r, g, bl)
    }
}
