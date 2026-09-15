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

/** Bakes one frozen frame of Spectral Folds for Auto/Ambient album art. */
internal object SpectralFoldsStill {
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
        paint.color = 0xFF06040A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        val count = 8
        for (i in 0 until count) {
            val xCenterFrac = rnd.nextFloat()
            val widthFrac = 0.25f + rnd.nextFloat() * 0.25f
            val waveSpeed = 0.7f + rnd.nextFloat() * 0.7f
            val foldPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val c = palette.colorAt(i)

            val wave = sin(loop * 2f * PI.toFloat() * waveSpeed + foldPhase)
            val cx = (xCenterFrac + wave * 0.08f) * w
            val fw = widthFrac * w
            val shadowOffset = w * 0.03f

            val shadowPath = Path().apply {
                moveTo(cx - fw / 2f + shadowOffset, 0f)
                lineTo(cx + fw / 2f + shadowOffset, 0f)
                lineTo(cx + fw * 0.2f + shadowOffset, h)
                lineTo(cx - fw * 0.2f + shadowOffset, h)
                close()
            }

            val foldPath = Path().apply {
                moveTo(cx - fw / 2f, 0f)
                lineTo(cx + fw / 2f, 0f)
                lineTo(cx + fw * 0.2f, h)
                lineTo(cx - fw * 0.2f, h)
                close()
            }

            // Shadow
            paint.shader = LinearGradient(
                cx - fw / 2f + shadowOffset, 0f, cx + fw / 2f + shadowOffset, 0f,
                Color.argb((0.45f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(shadowPath, paint)

            // Fold
            paint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.argb((0.65f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.argb((0.25f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(foldPath, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
