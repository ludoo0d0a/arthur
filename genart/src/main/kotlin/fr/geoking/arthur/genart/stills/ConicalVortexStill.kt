package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Conical Vortex for Auto/Ambient album art. */
internal object ConicalVortexStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val maxRadius = maxOf(w, h) * 0.8f

        paint.color = 0xFF06040A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val sectors = 12
        val sweep = 360f / sectors
        val time = (phase / (2f * PI.toFloat())) + (rotationDeg / 360f)
        val dim = 0.65f + 0.35f * pulse
        val baseAngleDeg = time * 360f

        canvas.save()
        canvas.rotate(baseAngleDeg, centerX, centerY)

        // Draw shadow spokes
        paint.color = Color.argb((0.5f * dim * 255).toInt(), 0, 0, 0)
        paint.strokeWidth = 14f
        paint.style = Paint.Style.STROKE

        for (i in 0 until sectors) {
            val angleRad = (i * sweep) * (PI / 180.0).toFloat()
            val edgeX = centerX + maxRadius * cos(angleRad)
            val edgeY = centerY + maxRadius * sin(angleRad)
            canvas.drawLine(centerX, centerY, edgeX, edgeY, paint)
        }

        // Draw color wedges
        paint.style = Paint.Style.FILL
        for (i in 0 until sectors) {
            val startAngle = i * sweep
            val angleRad1 = startAngle * (PI / 180.0).toFloat()
            val angleRad2 = (startAngle + sweep) * (PI / 180.0).toFloat()

            val path = Path().apply {
                moveTo(centerX, centerY)
                lineTo(centerX + maxRadius * cos(angleRad1), centerY + maxRadius * sin(angleRad1))
                lineTo(centerX + maxRadius * cos(angleRad2), centerY + maxRadius * sin(angleRad2))
                close()
            }

            val color = palette.colorAt(i)
            val alpha1 = (0.95f * dim * 255).toInt().coerceIn(0, 255)
            val alpha2 = (0.5f * dim * 255).toInt().coerceIn(0, 255)
            val alpha3 = (0.1f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                centerX, centerY, maxRadius,
                intArrayOf(
                    Color.argb(alpha1, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha2, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha3, Color.red(color), Color.green(color), Color.blue(color)),
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        canvas.restore()
    }
}
