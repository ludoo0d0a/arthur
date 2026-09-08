package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Tree in Wind" for Android Auto album art — deterministic per [generation]. */
internal object TreeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trunkPath = Path()

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

        val pulseMix = 0.85f + pulse * 0.15f
        val trunkBaseX = w * 0.5f
        val trunkBaseY = h
        val trunkTopY = h * 0.5f
        val trunkBottomHalfWidth = w * 0.035f
        val trunkTopHalfWidth = w * 0.012f

        trunkPath.reset()
        trunkPath.moveTo(trunkBaseX - trunkBottomHalfWidth, trunkBaseY)
        trunkPath.lineTo(trunkBaseX - trunkTopHalfWidth, trunkTopY)
        trunkPath.lineTo(trunkBaseX + trunkTopHalfWidth, trunkTopY)
        trunkPath.lineTo(trunkBaseX + trunkBottomHalfWidth, trunkBaseY)
        trunkPath.close()

        paint.color = Color.rgb(0x4A, 0x32, 0x22)
        canvas.drawPath(trunkPath, paint)

        val pivotX = w * 0.5f
        val pivotY = h * 0.5f
        val time = phase * 2f * PI.toFloat() + rotationDeg * PI.toFloat() / 180f
        val swayDeg = sin(time) * 7f
        val canopyCenterY = pivotY - minDim * 0.16f
        val canopyRadius = minDim * 0.34f * pulseMix

        canvas.save()
        canvas.rotate(swayDeg, pivotX, pivotY)

        val blobCount = 6
        for (i in 0 until blobCount) {
            val dx = -0.5f + rnd.nextFloat()
            val dy = -0.42f + rnd.nextFloat() * 0.54f
            val radiusFrac = 0.3f + rnd.nextFloat() * 0.26f
            val cx = pivotX + dx * canopyRadius * 1.5f
            val cy = canopyCenterY + dy * canopyRadius * 1.5f
            val radius = radiusFrac * canopyRadius

            val base = palette.colorAt(i)
            val greenTint = Color.rgb(
                (Color.red(base) * 0.4f + 0x3D * 0.6f).toInt().coerceIn(0, 255),
                (Color.green(base) * 0.4f + 0x8B * 0.6f).toInt().coerceIn(0, 255),
                (Color.blue(base) * 0.4f + 0x4E * 0.6f).toInt().coerceIn(0, 255),
            )

            paint.color = Color.argb(
                (0.28f * 255).toInt(),
                Color.red(greenTint),
                Color.green(greenTint),
                Color.blue(greenTint),
            )
            canvas.drawCircle(cx, cy, radius * 1.35f, paint)

            val coreColor = Color.rgb(
                (Color.red(greenTint) * 0.5f + 0x9C * 0.5f).toInt().coerceIn(0, 255),
                (Color.green(greenTint) * 0.5f + 0xCB * 0.5f).toInt().coerceIn(0, 255),
                (Color.blue(greenTint) * 0.5f + 0x6B * 0.5f).toInt().coerceIn(0, 255),
            )
            paint.color = Color.argb(
                (0.55f * 255).toInt(),
                Color.red(coreColor),
                Color.green(coreColor),
                Color.blue(coreColor),
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        canvas.restore()
        paint.alpha = 255
    }
}
