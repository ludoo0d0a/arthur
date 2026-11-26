package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of School of Fish for Auto/Ambient album art. */
internal object FishSchoolStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF061525.toInt(), 0xFF020810.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase
        val schoolX = (0.5f + 0.28f * cos(time * 0.45f)) * w
        val schoolY = (0.5f + 0.18f * sin(time * 0.6f)) * h
        val schoolHeading = Math.toDegrees(
            atan2(
                0.18f * 0.6f * cos(time * 0.6f),
                -0.28f * 0.45f * sin(time * 0.45f),
            ).toDouble(),
        ).toFloat()
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 32) {
            val arc = (i % 4) * (PI.toFloat() * 0.5f)
            val arcSpread = (rnd.nextFloat() - 0.5f) * 0.7f
            val orbitRadius = 0.06f + rnd.nextFloat() * 0.08f
            val angleOffset = arc + arcSpread
            val angularSpeed = 0.55f + rnd.nextFloat() * 0.3f
            val leadBoost = if (i < 3) 1.15f else 1f
            val scale = if (i < 3) {
                1.05f + rnd.nextFloat() * 0.3f
            } else {
                0.55f + rnd.nextFloat() * 0.5f
            }
            val verticalTilt = 0.45f + rnd.nextFloat() * 0.3f
            val angle = angleOffset + time * angularSpeed * 0.55f + rotationDeg * PI.toFloat() / 180f * 0.1f
            val fx = schoolX + cos(angle) * orbitRadius * minDim
            val fy = schoolY + sin(angle) * orbitRadius * minDim * verticalTilt
            val localHeading = Math.toDegrees(
                atan2(
                    cos(angle + 0.12f) - cos(angle),
                    -(sin(angle + 0.12f) - sin(angle)),
                ).toDouble(),
            ).toFloat()
            val heading = schoolHeading * 0.72f + localHeading * 0.28f
            val bodyLen = scale * leadBoost * minDim * 0.028f
            val bodyH = bodyLen * 0.38f
            val tint = palette.colorAt(i)
            val alpha = ((0.50f + 0.38f * scale) * dim * leadBoost.coerceAtMost(1.1f) * 255)
                .toInt().coerceIn(0, 255)

            canvas.save()
            canvas.rotate(heading, fx, fy)
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            path.reset()
            path.moveTo(fx + bodyLen, fy)
            path.cubicTo(
                fx + bodyLen * 0.7f, fy - bodyH,
                fx - bodyLen * 0.3f, fy - bodyH * 0.8f,
                fx - bodyLen * 0.55f, fy,
            )
            path.cubicTo(
                fx - bodyLen * 0.3f, fy + bodyH * 0.8f,
                fx + bodyLen * 0.7f, fy + bodyH,
                fx + bodyLen, fy,
            )
            path.close()
            canvas.drawPath(path, paint)

            path.reset()
            path.moveTo(fx + bodyLen * 0.05f, fy - bodyH * 0.55f)
            path.lineTo(fx - bodyLen * 0.15f, fy - bodyH * 1.15f)
            path.lineTo(fx - bodyLen * 0.25f, fy - bodyH * 0.45f)
            path.close()
            canvas.drawPath(path, paint)

            path.reset()
            path.moveTo(fx - bodyLen * 0.55f, fy)
            path.lineTo(fx - bodyLen * 0.95f, fy - bodyH * 0.75f)
            path.lineTo(fx - bodyLen * 0.72f, fy)
            path.lineTo(fx - bodyLen * 0.95f, fy + bodyH * 0.75f)
            path.close()
            canvas.drawPath(path, paint)

            paint.color = Color.argb((alpha * 0.75f).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(fx + bodyLen * 0.55f, fy - bodyH * 0.12f, bodyH * 0.14f, paint)
            paint.color = Color.argb((alpha * 0.55f).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawCircle(fx + bodyLen * 0.58f, fy - bodyH * 0.12f, bodyH * 0.07f, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }
}
