package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Gradient Mesh for Auto/Ambient album art. */
internal object GradientMeshStill {
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
        val minDim = minOf(w, h)
        paint.color = 0xFF05040A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 6) {
            val xA = rnd.nextFloat()
            val yA = rnd.nextFloat()
            val xB = rnd.nextFloat()
            val yB = rnd.nextFloat()
            val radiusFrac = 0.28f + rnd.nextFloat() * 0.27f
            val morphPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.22f + rnd.nextFloat() * 0.2f
            val morph = ((sin(loop * 2f * PI.toFloat() + morphPhase) + 1f) / 2f)
            val x = (xA + (xB - xA) * morph) * w
            val y = (yA + (yB - yA) * morph) * h
            val radius = radiusFrac * minDim * (0.85f + 0.15f * morph)
            val tintA = palette.colorAt(i)
            val tintB = palette.colorAt(i + 2)
            val r = (Color.red(tintA) + (Color.red(tintB) - Color.red(tintA)) * morph).toInt()
            val g = (Color.green(tintA) + (Color.green(tintB) - Color.green(tintA)) * morph).toInt()
            val b = (Color.blue(tintA) + (Color.blue(tintB) - Color.blue(tintA)) * morph).toInt()
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, r, g, b),
                    Color.argb((alpha * 0.45f).toInt(), r, g, b),
                    Color.argb((0.05f * dim * 255).toInt(), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
