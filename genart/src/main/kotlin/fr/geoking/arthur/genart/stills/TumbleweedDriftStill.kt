package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Tumbleweed Drift" for Android Auto album art — deterministic per [generation]. */
internal object TumbleweedDriftStill {
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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF241A12.toInt(), 0xFF120C10.toInt(), 0xFF05040A.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val ballCount = 2
        val lobeCount = 9
        val t = (phase / (2f * PI.toFloat())).let { ((it % 1f) + 1f) % 1f }

        for (i in 0 until ballCount) {
            val startXFrac = -0.2f + rnd.nextFloat() * 0.2f
            val endXFrac = 1f + rnd.nextFloat() * 0.2f
            val yFrac = 0.72f + rnd.nextFloat() * 0.18f
            val radiusFrac = 0.05f + rnd.nextFloat() * 0.03f
            val driftSpeed = 0.6f + rnd.nextFloat() * 0.5f
            val phaseOffset = rnd.nextFloat()

            val ballT = (t * driftSpeed + phaseOffset).let { ((it % 1f) + 1f) % 1f }
            val drift = startXFrac + (endXFrac - startXFrac) * ballT
            val x = drift * w
            val y = yFrac * h
            val radius = radiusFrac * w.coerceAtMost(h * 2f) * (0.9f + pulse * 0.1f)
            val edgeFade = sin(ballT * PI.toFloat()).coerceAtLeast(0f)
            val ballRotationDeg = rotationDeg + drift * 360f * 6f

            val base = palette.colorAt(i)
            val factor = 0.22f
            val alpha = (0.55f * edgeFade * 255).toInt().coerceIn(0, 140)
            paint.color = Color.argb(
                alpha,
                (Color.red(base) * factor).toInt().coerceIn(0, 255),
                (Color.green(base) * factor).toInt().coerceIn(0, 255),
                (Color.blue(base) * factor).toInt().coerceIn(0, 255),
            )
            if (alpha <= 2) continue

            val lobeAngles = (0 until lobeCount).map { j ->
                val baseAngle = (j / lobeCount.toFloat()) * 2f * PI.toFloat()
                val jitter = (rnd.nextFloat() - 0.5f) * 0.5f
                val distanceFrac = 0.45f + rnd.nextFloat() * 0.55f
                val sizeFrac = 0.35f + rnd.nextFloat() * 0.3f
                Triple(baseAngle + jitter, distanceFrac, sizeFrac)
            }

            canvas.save()
            canvas.rotate(ballRotationDeg, x, y)
            canvas.drawCircle(x, y, radius * 0.55f, paint)
            lobeAngles.forEach { (angle, distanceFrac, sizeFrac) ->
                val lobeDistance = radius * distanceFrac
                val lobeRadius = radius * sizeFrac * 0.55f
                val lx = x + lobeDistance * cos(angle)
                val ly = y + lobeDistance * sin(angle)
                canvas.drawCircle(lx, ly, lobeRadius, paint)
            }
            canvas.restore()
        }
        paint.alpha = 255
    }
}
