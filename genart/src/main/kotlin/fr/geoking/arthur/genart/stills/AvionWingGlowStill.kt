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

/** Bakes one frozen frame of Avion Wing Glow for Auto/Ambient album art. */
internal object AvionWingGlowStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val drift = loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 29) - 0.5f
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080A0C.toInt(), 0xFF1A2840.toInt(), 0xFF2A3858.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFFFD080.toInt(), palette.colorAt(0), 0.35f)
        val horizon = h * (0.50f + 0.025f * (wash - 0.5f))
        paint.shader = LinearGradient(
            0f, horizon - h * 0.10f, 0f, horizon + h * 0.22f,
            intArrayOf(
                withAlpha(0xFFB8C8D8.toInt(), (0.03f * dim * 255).toInt()),
                withAlpha(0xFFB8C8D8.toInt(), (0.12f * dim * 255).toInt()),
                withAlpha(0xFF2A3858.toInt(), (0.04f * dim * 255).toInt()),
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.10f, w, horizon + h * 0.22f, paint)
        paint.shader = null

        // Soft cloud bank beneath the horizon.
        for (i in 0..2) {
            val cloudX = w * (0.24f + 0.31f * i)
            val cloudY = horizon + h * (0.10f + i * 0.07f) + drift * h * 0.015f
            val cloudRadius = minOf(w, h) * (0.39f + 0.04f * i)
            paint.shader = RadialGradient(
                cloudX, cloudY, cloudRadius,
                intArrayOf(
                    withAlpha(0xFFB8C8D8.toInt(), ((0.14f - i * 0.025f) * dim * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(
                cloudX - cloudRadius, cloudY - cloudRadius * 0.30f,
                cloudX + cloudRadius, cloudY + cloudRadius * 0.30f,
                paint,
            )
            paint.shader = null
        }

        // Passenger-window view of a broad wing stretching toward the tip.
        val wing = Path().apply {
            moveTo(w * 0.39f, h * 0.78f)
            cubicTo(w * 0.55f, h * 0.66f, w * 0.76f, h * 0.54f, w * 0.95f, h * 0.52f)
            lineTo(w * 1.02f, h * 0.89f)
            cubicTo(w * 0.83f, h * 0.91f, w * 0.58f, h * 0.91f, w * 0.39f, h * 0.78f)
            close()
        }
        paint.shader = LinearGradient(
            w * 0.52f, h * 0.67f, w * 0.75f, h * 0.95f,
            intArrayOf(0xFF26334A.toInt(), 0xFF0A0C10.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(wing, paint)
        paint.shader = null

        val leadingEdge = Path().apply {
            moveTo(w * 0.39f, h * 0.78f)
            cubicTo(w * 0.55f, h * 0.66f, w * 0.76f, h * 0.54f, w * 0.95f, h * 0.52f)
        }
        val glowX = w * (0.67f + drift * 0.035f)
        val glowY = h * (0.64f + 0.02f * sin(t01 * 2f * PI.toFloat()))
        val glowRadius = minOf(w, h) * 0.32f
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(
                withAlpha(accent, (0.32f * dim * 255).toInt()),
                withAlpha(accent, (0.10f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minOf(w, h) * 0.018f
        paint.color = withAlpha(accent, (0.16f * dim * 255).toInt())
        canvas.drawPath(leadingEdge, paint)
        paint.strokeWidth = minOf(w, h) * 0.006f
        paint.color = withAlpha(accent, ((0.26f + 0.08f * wash) * dim * 255).toInt())
        canvas.drawPath(leadingEdge, paint)
        paint.style = Paint.Style.FILL

        // Warm nav light at the wingtip diffuses into the surrounding haze.
        val tipX = w * 0.925f
        val tipY = h * 0.535f
        val tipRadius = minOf(w, h) * 0.14f
        paint.shader = RadialGradient(
            tipX, tipY, tipRadius,
            intArrayOf(
                withAlpha(0xFFFFD080.toInt(), ((0.50f + 0.12f * wash) * dim * 255).toInt()),
                withAlpha(0xFFFFD080.toInt(), (0.18f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.25f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(tipX, tipY, tipRadius, paint)
        paint.shader = null

        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.04f, seedOffset = i * 97 + 5)
            val x = ((0.1f + i * 0.12f + t01 * 0.04f) % 1f) * w
            val y = (0.2f + (i % 4) * 0.15f + 0.012f * sin(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = withAlpha(0xFFDDE7F2.toInt(), ((0.035f + 0.055f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, minOf(w, h) * (0.009f + 0.012f * n), paint)
        }

        // Feathered cabin/window edges frame the view softly.
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                withAlpha(0xFF080A0C.toInt(), (0.62f * dim * 255).toInt()),
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                withAlpha(0xFF080A0C.toInt(), (0.48f * dim * 255).toInt()),
            ),
            floatArrayOf(0f, 0.07f, 0.92f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
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
}
