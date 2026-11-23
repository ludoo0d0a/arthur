package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of wet asphalt from a bicycle POV. */
internal object VeloRainAsphaltStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t, radius = 1.35f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(t, radius = 1.15f, octaves = 2, seedOffset = 37)
        val dim = 0.78f + 0.22f * pulse
        val horizon = h * (0.43f + 0.012f * (wash - 0.5f))
        val cool = mixArgb(0xFF90B0C0.toInt(), palette.colorAt(0), 0.16f)
        val amber = 0xFFC89A70.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF2A3038.toInt(), 0xFF202830.toInt(), 0xFF14181C.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.43f, 0.60f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffuse rain haze and distant mixed street-light glow.
        paint.shader = LinearGradient(
            0f, horizon - h * 0.09f, 0f, horizon + h * 0.09f,
            intArrayOf(Color.TRANSPARENT, withAlpha(cool, (0.14f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.09f, w, horizon + h * 0.09f, paint)
        paint.shader = RadialGradient(
            w * (0.55f + 0.06f * (warp - 0.5f)), horizon, minDim * 0.34f,
            intArrayOf(withAlpha(amber, (0.13f * dim * 255).toInt()), withAlpha(cool, (0.06f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * (0.55f + 0.06f * (warp - 0.5f)), horizon, minDim * 0.34f, paint)
        paint.shader = null

        // Perspective shoulders and center dashes.
        paint.color = withAlpha(cool, (0.15f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.004f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.47f, horizon, -w * 0.16f, h, paint)
        canvas.drawLine(w * 0.53f, horizon, w * 1.16f, h, paint)
        for (i in 0 until 6) {
            val depth = (i + 1) / 7f
            val y = horizon + (h - horizon) * depth * depth
            val dashW = minDim * (0.004f + 0.010f * depth)
            val dashH = minDim * (0.008f + 0.026f * depth)
            paint.color = withAlpha(0xFFB8C0C0.toInt(), (0.13f * depth * dim * 255).toInt())
            rect.set(w * 0.5f - dashW * 0.5f, y, w * 0.5f + dashW * 0.5f, y + dashH)
            canvas.drawRoundRect(rect, dashW, dashW, paint)
        }

        // Narrow, broken light streaks reflected in the wet road.
        for (i in 0 until 18) {
            val seed = (i * 73 + 19).toFloat()
            val x0 = fract(seed * 0.6180339f)
            val phaseOffset = fract(seed * 0.271828f)
            val speed = 0.32f + fract(seed * 0.1414f) * 0.50f
            val phaseT = (t * speed + phaseOffset) % 1f
            val depth = 0.18f + 0.82f * phaseT
            val y = horizon + (h - horizon) * depth * depth
            val x = w * (0.5f + (x0 - 0.5f) * (0.08f + 0.95f * depth)) +
                sin((t + phaseOffset) * 2f * PI.toFloat()) * minDim * 0.008f
            val streakW = minDim * (0.008f + fract(seed * 0.36f) * 0.037f) * (0.28f + depth * 1.7f)
            val streakH = minDim * (0.003f + depth * 0.018f) * (0.75f + 0.25f * wash)
            val light = if (i % 3 == 0) cool else amber
            paint.shader = LinearGradient(
                x - streakW, 0f, x + streakW, 0f,
                intArrayOf(Color.TRANSPARENT, withAlpha(light, ((0.05f + 0.16f * depth) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
            rect.set(x - streakW, y, x + streakW, y + streakH)
            canvas.drawRoundRect(rect, streakH, streakH, paint)
            paint.shader = null
            if (i % 4 == 0) {
                paint.color = withAlpha(0xFFC8D8E0.toInt(), (0.12f * depth * dim * 255).toInt())
                canvas.drawCircle(x, y + streakH * 1.6f, minDim * (0.0015f + 0.0025f * depth), paint)
            }
        }

        // A few fine rain traces on the foreground surface.
        for (i in 0 until 24) {
            val seed = (i * 31 + 7).toFloat()
            val xUnit = fract(seed * 0.4142f)
            val d = 0.38f + 0.62f * fract(t * 0.24f + seed * 0.11f)
            val x = w * (0.5f + (xUnit - 0.5f) * d * 1.8f)
            val y = horizon + (h - horizon) * d
            paint.color = withAlpha(cool, (0.075f * d * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.0012f
            canvas.drawLine(x, y, x - minDim * 0.002f, y + minDim * (0.008f + 0.012f * d), paint)
        }
    }

    private fun fract(value: Float): Float = ((value % 1f) + 1f) % 1f

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
