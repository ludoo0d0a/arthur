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

/** Bakes one frozen frame of Crystal Cavern Glow for Auto/Ambient album art. */
internal object CrystalCavernGlowStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        val t = (((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())) + 1f) % 1f
        val dim = 0.85f + 0.15f * pulse
        val shimmer = 0.78f + 0.22f * loopedFbm(t, radius = 1.5f, octaves = 2, seedOffset = 31)
        val accent = mixArgb(0xFFA0E0FF.toInt(), palette.colorAt(0), 0.25f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0C1220.toInt(), 0xFF1A2840.toInt(), 0xFF101A2C.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // A broad, low contrast pool of cyan light gives depth to the chamber.
        val glowX = w * (0.49f + (loopedFbm(t, radius = 1.2f, octaves = 3, seedOffset = 13) - 0.5f) * 0.12f)
        val glowY = h * 0.72f
        paint.shader = RadialGradient(
            glowX, glowY, minDim * 0.66f,
            intArrayOf(withAlpha(accent, (0.24f * shimmer * dim * 255).toInt()), withAlpha(0xFF355E82.toInt(), (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, minDim * 0.66f, paint)
        paint.shader = null

        // Jagged overhang and uneven cave floor frame the open chamber.
        path.reset()
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w, h * 0.18f)
        path.lineTo(w * 0.78f, h * 0.24f); path.lineTo(w * 0.62f, h * 0.15f)
        path.lineTo(w * 0.48f, h * 0.22f); path.lineTo(w * 0.30f, h * 0.14f)
        path.lineTo(w * 0.14f, h * 0.23f); path.lineTo(0f, h * 0.18f); path.close()
        paint.color = 0xFF080D18.toInt()
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(0f, h * 0.86f); path.lineTo(w * 0.18f, h * 0.81f)
        path.lineTo(w * 0.35f, h * 0.88f); path.lineTo(w * 0.55f, h * 0.83f)
        path.lineTo(w * 0.77f, h * 0.89f); path.lineTo(w, h * 0.82f)
        path.lineTo(w, h); path.lineTo(0f, h); path.close()
        paint.color = 0xFF0A1120.toInt()
        canvas.drawPath(path, paint)

        // Crystal clusters rise from the floor, with a brighter facet facing the glow.
        val count = 8
        for (i in 0 until count) {
            val x = w * (i + 0.5f) / count
            val height = h * (0.17f + 0.24f * loopedFbm(t, radius = 1.1f, seedOffset = i * 17 + 3))
            val width = minDim * (0.045f + 0.025f * loopedFbm(t, radius = 1.2f, seedOffset = i * 29 + 7))
            val lean = (loopedFbm(t, radius = 1.35f, seedOffset = i * 37 + 19) - 0.5f) * minDim * 0.04f
            val baseY = h * (0.84f + 0.045f * loopedFbm(t, radius = 1.1f, seedOffset = i + 71))
            drawCrystal(canvas, x, baseY, x + lean, baseY - height, width, accent, shimmer * dim, i)
        }

        // Sparse fine seams in the surrounding stone.
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.003f
        for (i in 0 until 4) {
            paint.color = withAlpha(accent, ((0.10f + 0.05f * shimmer) * dim * 255).toInt())
            val sideLeft = i % 2 == 0
            canvas.drawLine(
                if (sideLeft) 0f else w,
                h * (0.42f + i * 0.08f),
                if (sideLeft) w * (0.12f + i * 0.045f) else w * (0.88f - i * 0.045f),
                h * (0.36f + i * 0.11f),
                paint,
            )
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawCrystal(
        canvas: Canvas,
        x: Float,
        baseY: Float,
        tipX: Float,
        tipY: Float,
        halfWidth: Float,
        glow: Int,
        intensity: Float,
        index: Int,
    ) {
        val shoulder = tipY + (baseY - tipY) * 0.34f
        path.reset()
        path.moveTo(tipX, tipY)
        path.lineTo(x + halfWidth, shoulder)
        path.lineTo(x + halfWidth * 0.72f, baseY)
        path.lineTo(x - halfWidth * 0.72f, baseY)
        path.lineTo(x - halfWidth, shoulder)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = if (index % 2 == 0) 0xFF244264.toInt() else 0xFF1B3450.toInt()
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(tipX, tipY)
        path.lineTo(x + halfWidth, shoulder)
        path.lineTo(x, baseY)
        path.close()
        paint.color = withAlpha(glow, (0.18f * intensity * 255).toInt())
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = halfWidth * 0.055f
        paint.color = withAlpha(glow, (0.32f * intensity * 255).toInt())
        canvas.drawLine(tipX, tipY, x, baseY, paint)
        paint.strokeWidth = halfWidth * 0.035f
        paint.color = withAlpha(glow, (0.38f * intensity * 255).toInt())
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
