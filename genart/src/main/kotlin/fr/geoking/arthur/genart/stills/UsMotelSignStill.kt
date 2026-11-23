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

/** Bakes one frozen frame of US Motel Sign for Auto/Ambient album art. */
internal object UsMotelSignStill {
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
        val unit = minOf(w, h)
        val time = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 31)
        val flicker = 0.92f + 0.08f * loopedFbm(time, radius = 2.1f, octaves = 2, seedOffset = 73)
        val dim = (0.85f + 0.15f * pulse).coerceIn(0f, 1.2f)
        val warm = mixArgb(0xFFFFE060.toInt(), palette.colorAt(0), 0.2f)
        val red = 0xFFFF5A4E.toInt()
        val cyan = 0xFF6EDDE8.toInt()
        val horizon = h * (0.70f + 0.012f * (wash - 0.5f))
        val cx = w * 0.66f
        val cy = h * 0.39f
        val sw = unit * 0.43f
        val sh = unit * 0.31f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080A10.toInt(), 0xFF111321.toInt(), 0xFF17151A.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // A hazy desert horizon behind two low mesas.
        paint.shader = LinearGradient(
            0f, horizon - h * 0.12f, 0f, horizon + h * 0.12f,
            intArrayOf(Color.TRANSPARENT, withAlpha(warm, (0.14f * flicker * dim * 255).toInt())),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.12f, w, horizon + h * 0.12f, paint)
        paint.shader = null
        paint.color = 0xFF11131A.toInt()
        canvas.drawOval(-w * 0.12f, horizon - h * 0.10f, w * 0.44f, horizon + h * 0.09f, paint)
        paint.color = 0xFF0D1017.toInt()
        canvas.drawOval(w * 0.48f, horizon - h * 0.08f, w * 1.20f, horizon + h * 0.08f, paint)
        paint.color = 0xFF090B10.toInt()
        canvas.drawRect(0f, horizon, w, h, paint)

        // Faint road shoulders leading to the sign-lit horizon.
        paint.color = 0xFF353039.toInt()
        paint.strokeWidth = unit * 0.008f
        canvas.drawLine(w * 0.43f, h, w * 0.48f, horizon, paint)
        canvas.drawLine(w * 0.57f, h, w * 0.52f, horizon, paint)
        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(warm, (0.08f * flicker * dim * 255).toInt())),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
        paint.shader = null

        // Pole and muted red/cyan light bloom.
        paint.color = 0xFF17191F.toInt()
        paint.strokeWidth = unit * 0.025f
        canvas.drawLine(cx, cy + sh * 0.45f, cx, h * 0.88f, paint)
        paint.strokeWidth = unit * 0.014f
        canvas.drawLine(cx - sw * 0.24f, h * 0.62f, cx + sw * 0.24f, h * 0.62f, paint)
        paint.shader = RadialGradient(
            cx, cy, unit * 0.48f,
            intArrayOf(withAlpha(warm, (0.27f * flicker * dim * 255).toInt()), withAlpha(red, (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, unit * 0.48f, paint)
        paint.shader = null

        // Enamel sign face with neon frame.
        val left = cx - sw / 2f
        val top = cy - sh / 2f
        rect.set(left, top, left + sw, top + sh)
        paint.color = 0xFF18171C.toInt()
        canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 0.009f
        paint.color = withAlpha(red, (0.65f * flicker * dim * 255).toInt())
        canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint)
        paint.style = Paint.Style.FILL
        rect.inset(unit * 0.025f, unit * 0.025f)
        paint.color = withAlpha(warm, (0.18f * dim * 255).toInt())
        canvas.drawRoundRect(rect, unit * 0.022f, unit * 0.022f, paint)

        // Simple neon strokes suggest a header and room-number panel without literal copy.
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = unit * 0.020f
        paint.color = withAlpha(warm, (0.92f * flicker * dim * 255).toInt())
        canvas.drawLine(cx - sw * 0.29f, cy - sh * 0.19f, cx + sw * 0.29f, cy - sh * 0.19f, paint)
        paint.strokeWidth = unit * 0.009f
        paint.color = withAlpha(cyan, (0.8f * flicker * dim * 255).toInt())
        canvas.drawLine(cx - sw * 0.21f, cy - sh * 0.01f, cx + sw * 0.21f, cy - sh * 0.01f, paint)
        paint.strokeWidth = unit * 0.008f
        paint.color = withAlpha(warm, (0.74f * flicker * dim * 255).toInt())
        canvas.drawLine(cx - sw * 0.24f, cy + sh * 0.13f, cx + sw * 0.24f, cy + sh * 0.13f, paint)
        for (i in -1..1) {
            paint.color = withAlpha(warm, (0.8f * flicker * dim * 255).toInt())
            canvas.drawCircle(cx + sw * i * 0.20f, cy + sh * 0.32f, unit * 0.018f, paint)
        }
        paint.color = withAlpha(cyan, (0.75f * flicker * dim * 255).toInt())
        canvas.drawCircle(cx + sw * 0.39f, cy - sh * 0.30f, unit * 0.022f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.TRANSPARENT
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
