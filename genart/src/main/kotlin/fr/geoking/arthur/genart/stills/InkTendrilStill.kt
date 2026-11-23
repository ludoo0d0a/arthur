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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Ink Tendril for Auto/Ambient album art. */
internal object InkTendrilStill {
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
        val unit = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = 0.85f + 0.15f * pulse
        val drift = loopedFbm(t, radius = 1.1f, octaves = 3, seedOffset = 37) - 0.5f
        val breath = 0.94f + 0.06f * loopedFbm(t, radius = 1.5f, octaves = 2, seedOffset = 83)
        val ink = mixArgb(0xFF101018.toInt(), palette.colorAt(0), 0.12f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFECE6DC.toInt(), 0xFFC8C0B4.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(
            w * (0.55f + drift * 0.08f), h * 0.48f, unit * 0.72f,
            intArrayOf(withAlpha(0xFFFFF9EC.toInt(), (0.34f * dim * 255).toInt()), withAlpha(0xFFE8D8C6.toInt(), (0.14f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.55f, h * 0.48f, unit * 0.72f, paint)
        paint.shader = null

        val ox = w * 0.5f
        val oy = h * 0.88f
        val fan = 10
        for (i in 0 until fan) {
            val spread = (i - (fan - 1) * 0.5f) / ((fan - 1) * 0.5f)
            val angle = -PI.toFloat() * 0.5f + spread * 0.92f + drift * 0.12f
            val length = unit * (0.28f + 0.23f * (1f - abs(spread)))
            val sway = sin(t * 2f * PI.toFloat() + i * 0.71f) * unit * 0.018f
            val ex = ox + cos(angle) * length + sway
            val ey = oy + sin(angle) * length
            val bx = ox + cos(angle) * length * 0.48f + sway * 0.3f - sin(angle) * unit * 0.055f
            val by = oy + sin(angle) * length * 0.48f + cos(angle) * unit * 0.055f
            val width = unit * (0.008f + (1f - abs(spread)) * 0.008f)

            path.reset()
            path.moveTo(ox, oy)
            path.quadTo(bx, by, ex, ey)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = width * 4f
            paint.color = withAlpha(ink, (0.10f * breath * dim * 255).toInt())
            canvas.drawPath(path, paint)
            paint.strokeWidth = width
            paint.color = withAlpha(ink, (0.56f * breath * dim * 255).toInt())
            canvas.drawPath(path, paint)

            for (side in -1..1 step 2) {
                val fraction = 0.38f + (i % 3) * 0.1f
                val sx = ox + (ex - ox) * fraction
                val sy = oy + (ey - oy) * fraction
                val branchAngle = angle + side * (0.50f + 0.12f * sin(t * 2f * PI.toFloat() + i))
                val branchLength = length * (0.32f + 0.08f * loopedFbm(t, radius = 1.2f, seedOffset = 109 + i))
                val tx = sx + cos(branchAngle) * branchLength
                val ty = sy + sin(branchAngle) * branchLength
                path.reset()
                path.moveTo(sx, sy)
                path.quadTo(
                    sx + cos(branchAngle - 0.45f) * branchLength * 0.55f,
                    sy + sin(branchAngle - 0.45f) * branchLength * 0.55f,
                    tx, ty,
                )
                val branchWidth = width * (0.30f + 0.1f * ((i + side + 2) % 3))
                paint.strokeWidth = branchWidth * 3.5f
                paint.color = withAlpha(ink, (0.08f * dim * 255).toInt())
                canvas.drawPath(path, paint)
                paint.strokeWidth = branchWidth
                paint.color = withAlpha(ink, (0.42f * breath * dim * 255).toInt())
                canvas.drawPath(path, paint)
            }
        }

        paint.style = Paint.Style.FILL
        for (i in 0 until 9) {
            val x = w * (0.28f + i * 0.055f)
            val y = h * (0.70f - 0.035f * (i % 3))
            val r = unit * (0.022f + 0.006f * loopedFbm(t, radius = 1.2f, seedOffset = 151 + i))
            paint.shader = RadialGradient(
                x, y, r,
                intArrayOf(withAlpha(ink, (0.22f * breath * dim * 255).toInt()), withAlpha(ink, (0.11f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, paint)
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

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
