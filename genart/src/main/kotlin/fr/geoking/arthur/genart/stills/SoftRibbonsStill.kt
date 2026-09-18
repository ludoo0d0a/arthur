package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Ribbons for Auto/Ambient album art. */
internal object SoftRibbonsStill {
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
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.75f,
            intArrayOf(0xFF0C0A14.toInt(), 0xFF040308.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.3f
        val dim = 0.65f + 0.35f * pulse
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        for (i in 0 until 5) {
            val baseY = (0.2f + rnd.nextFloat() * 0.6f) * h
            val amplitude = (0.04f + rnd.nextFloat() * 0.08f) * h * dim
            val cycles = 1.2f + rnd.nextFloat() * 1.6f
            val ribbonPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val thickness = 1.5f + rnd.nextFloat() * 2.5f
            val speedMul = 0.5f + rnd.nextFloat() * 0.7f
            val freq = cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
            path.reset()
            val segments = 40
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + amplitude * sin(x * freq + time * speedMul + ribbonPhase)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val tint = palette.colorAt(i)
            paint.strokeWidth = thickness * 4f
            paint.color = Color.argb(
                (0.12f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(path, paint)
            paint.strokeWidth = thickness
            paint.color = Color.argb(
                (0.45f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
