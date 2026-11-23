package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes a moonlit tide and its soft lace of foam for album art. */
internal object TideFoamStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        val time = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = 0.88f + 0.12f * pulse
        val foam = mixArgb(0xFFE8F0F8.toInt(), palette.colorAt(0), 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101820.toInt(), 0xFF192833.toInt(), 0xFF0E1C28.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glowX = w * 0.55f
        val glowY = h * 0.57f
        paint.shader = RadialGradient(
            glowX, glowY, minDim * 0.72f,
            intArrayOf(withAlpha(foam, (0.10f * dim * 255).toInt()), 0x2624475A, Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, minDim * 0.72f, paint)
        paint.shader = null

        fun shoreY(x: Float, offset: Float = 0f): Float {
            val x01 = x / w
            val swell = sin(x01 * 2f * PI.toFloat() + time * 2f * PI.toFloat())
            val ripple = sin(x01 * 5f * PI.toFloat() - time * 2f * PI.toFloat())
            val warp = loopedFbm(time + x01 * 0.12f, radius = 1.15f, octaves = 2, seedOffset = 61)
            return h * (0.61f + 0.052f * swell + 0.014f * ripple + 0.022f * (warp - 0.5f)) + offset
        }

        fun tidePath(offset: Float, closed: Boolean): Path = Path().apply {
            val segments = 48
            for (i in 0..segments) {
                val x = w * i / segments
                val y = shoreY(x, offset)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            if (closed) {
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
        }

        val shore = Path().apply {
            val segments = 48
            moveTo(0f, 0f)
            lineTo(w, 0f)
            for (i in segments downTo 0) {
                val x = w * i / segments
                lineTo(x, shoreY(x, -minDim * 0.01f))
            }
            close()
        }
        paint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.68f,
            intArrayOf(0xFF101820.toInt(), 0xFF1A2934.toInt(), 0xFF263844.toInt()),
            floatArrayOf(0f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(shore, paint)
        paint.shader = null
        for (i in 0 until 9) {
            val x = w * (0.08f + i * 0.84f / 8f)
            val y = shoreY(x, -minDim * (0.035f + (i % 3) * 0.014f))
            val radius = minDim * (0.006f + (i % 3) * 0.002f)
            paint.color = withAlpha(0xFF080F15.toInt(), (0.55f * dim * 255).toInt())
            canvas.drawCircle(x, y, radius, paint)
        }

        // Teal water deepens toward the bottom of the frame.
        paint.shader = LinearGradient(
            0f, h * 0.58f, 0f, h,
            intArrayOf(0xFF24475A.toInt(), 0xFF1A3040.toInt(), 0xFF0E1C28.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(tidePath(0f, closed = true), paint)
        paint.shader = null

        // Soft parallel wash traces retreat from the shore.
        for (band in 1..3) {
            val path = tidePath(minDim * (0.016f + band * 0.025f), closed = false)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * (0.010f / band)
            paint.color = withAlpha(foam, (0.15f / band * dim * 255).toInt())
            canvas.drawPath(path, paint)
            paint.strokeWidth = minDim * (0.0022f / band)
            paint.color = withAlpha(foam, (0.20f / band * dim * 255).toInt())
            canvas.drawPath(path, paint)
        }

        // Uneven scallops break the white leading edge into lace-like foam.
        val lace = Path()
        val count = 56
        for (i in 0 until count) {
            val x = w * i / (count - 1)
            val scallop = sin(i * PI.toFloat() * 0.92f) * minDim * 0.005f
            val y = shoreY(x, -minDim * 0.006f) + scallop
            if (i == 0) lace.moveTo(x, y) else lace.lineTo(x, y)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.009f
        paint.color = withAlpha(foam, (0.52f * dim * 255).toInt())
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(lace, paint)
        paint.strokeWidth = minDim * 0.002f
        paint.color = withAlpha(Color.WHITE, (0.42f * dim * 255).toInt())
        canvas.drawPath(lace, paint)

        // Thin moonlit ripples travel across the foreground water.
        val details = 7
        for (i in 0 until details) {
            val yBase = h * (0.72f + i * 0.045f)
            val path = Path()
            val segments = 32
            for (step in 0..segments) {
                val x = w * step / segments
                val x01 = x / w
                val y = yBase + minDim * 0.008f * sin(x01 * 4f * PI.toFloat() + time * 2f * PI.toFloat() + i)
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            paint.strokeWidth = minDim * 0.0018f
            paint.color = withAlpha(foam, ((0.10f + 0.02f * (i % 3)) * dim * 255).toInt())
            canvas.drawPath(path, paint)
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
