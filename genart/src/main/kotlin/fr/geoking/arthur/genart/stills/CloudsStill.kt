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

/** Bakes one frozen frame of "Drifting Clouds" for Auto/Ambient album art. */
internal object CloudsStill {
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
        val minDim = w.coerceAtMost(h)

        val skyTop = palette.primary
        val skyMid = palette.secondary
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                Color.argb(
                    255,
                    (Color.red(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                    (Color.green(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                    (Color.blue(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                ),
                Color.argb(
                    255,
                    (Color.red(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                    (Color.green(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                    (Color.blue(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                ),
                0xFF0A1018.toInt(),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cloudCount = 6
        val pulseMix = 0.85f + pulse * 0.15f
        val drift = phase + rotationDeg * PI.toFloat() / 180f

        for (i in 0 until cloudCount) {
            val x0 = rnd.nextFloat()
            val yFrac = 0.12f + rnd.nextFloat() * 0.46f
            val speedMul = 0.08f + rnd.nextFloat() * 0.14f
            val bobAmpFrac = 0.004f + rnd.nextFloat() * 0.014f
            val bobFreq = 0.3f + rnd.nextFloat() * 0.8f
            val bobPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val scale = (0.55f + rnd.nextFloat() * 0.6f) * pulseMix
            val tint = palette.colorAt(i)
            val puffCount = 4 + rnd.nextInt(3)

            val x = (((x0 + drift * speedMul * 0.15f) % 1f + 1f) % 1f) * w
            val y = yFrac * h +
                sin(drift * bobFreq + bobPhase) * bobAmpFrac * h

            for (p in 0 until puffCount) {
                val dx = -0.55f + rnd.nextFloat() * 1.1f
                val dy = -0.28f + rnd.nextFloat() * 0.5f
                val radiusFrac = 0.22f + rnd.nextFloat() * 0.26f
                val cx = x + dx * scale * minDim * 0.55f
                val cy = y + dy * scale * minDim * 0.35f
                val radius = radiusFrac * scale * minDim * 0.22f

                paint.color = Color.argb(
                    (0.10f * 255).toInt(),
                    Color.red(tint),
                    Color.green(tint),
                    Color.blue(tint),
                )
                canvas.drawCircle(cx, cy, radius * 1.45f, paint)

                paint.color = Color.argb((0.22f * 255).toInt(), 232, 238, 245)
                canvas.drawCircle(cx, cy, radius, paint)
            }
        }
        paint.alpha = 255
    }
}
