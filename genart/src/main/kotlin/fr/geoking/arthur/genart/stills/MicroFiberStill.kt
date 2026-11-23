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

/** Bakes one frozen close-up woven-fiber frame for Auto/Ambient album art. */
internal object MicroFiberStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val breathe = loopedFbm(t01, radius = 1.3f, octaves = 2, seedOffset = 196)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFFE0C0A0.toInt(), palette.colorAt(0), 0.28f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(brightness(0xFF201818.toInt(), dim), brightness(0xFF101010.toInt(), dim)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glowX = w * (0.46f + 0.08f * (breathe - 0.5f))
        val glowY = h * 0.47f
        val glowRadius = minDim * 0.76f
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(withAlpha(accent, (0.22f * dim * 255).toInt()), withAlpha(0xFF806454.toInt(), (0.08f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = null

        val strandCount = 38
        val segments = 12
        val verticalStep = w / strandCount
        val horizontalStep = h / strandCount
        val path = Path()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0..strandCount) {
            val u = i.toFloat() / strandCount
            val centerLight = (1f - kotlin.math.abs(u - 0.5f) * 1.5f).coerceIn(0.12f, 1f)
            path.reset()
            for (segment in 0..segments) {
                val p = segment.toFloat() / segments
                val wave = sin(p * 2f * PI.toFloat() + i * 0.71f + t01 * 2f * PI.toFloat())
                val x = i * verticalStep + wave * minDim * 0.0028f + (breathe - 0.5f) * minDim * 0.002f
                val y = p * h
                if (segment == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val alpha = (if (i % 2 == 0) 0.22f else 0.11f) * centerLight * dim
            paint.color = withAlpha(if (i % 2 == 0) accent else 0xFF6A5145.toInt(), (alpha * 255).toInt())
            paint.strokeWidth = (minDim * 0.0035f).coerceAtLeast(0.7f)
            canvas.drawPath(path, paint)
        }
        for (i in 0..strandCount) {
            val u = i.toFloat() / strandCount
            val centerLight = (1f - kotlin.math.abs(u - 0.5f) * 1.5f).coerceIn(0.12f, 1f)
            path.reset()
            for (segment in 0..segments) {
                val p = segment.toFloat() / segments
                val wave = sin(p * 2f * PI.toFloat() + i * 0.57f - t01 * 2f * PI.toFloat())
                val y = i * horizontalStep + wave * minDim * 0.0028f + (breathe - 0.5f) * minDim * 0.002f
                val x = p * w
                if (segment == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val alpha = (if (i % 2 == 0) 0.12f else 0.24f) * centerLight * dim
            paint.color = withAlpha(if (i % 2 == 0) 0xFF9B806D.toInt() else accent, (alpha * 255).toInt())
            paint.strokeWidth = (minDim * 0.003f).coerceAtLeast(0.65f)
            canvas.drawPath(path, paint)
        }

        // Subtle diagonal fibers add a macro textile grain.
        paint.color = withAlpha(0xFFE8D4C0.toInt(), ((0.08f + 0.04f * breathe) * dim * 255).toInt())
        paint.strokeWidth = (minDim * 0.001f).coerceAtLeast(0.45f)
        for (i in -strandCount..strandCount) {
            val x = i * verticalStep
            canvas.drawLine(x, 0f, x + h * 0.26f, h, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))

    private fun brightness(color: Int, factor: Float): Int {
        val f = factor.coerceAtLeast(0f)
        return Color.rgb(
            (Color.red(color) * f).toInt().coerceIn(0, 255),
            (Color.green(color) * f).toInt().coerceIn(0, 255),
            (Color.blue(color) * f).toInt().coerceIn(0, 255),
        )
    }
}
