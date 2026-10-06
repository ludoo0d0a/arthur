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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Glass Orb on Desk for Auto/Ambient album art. */
internal object GlassOrbStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val timeAngle = loop * 2f * PI.toFloat()

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1C1824.toInt(), 0xFF0E0C12.toInt(), 0xFF060508.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.82f, (minDim * 0.75f).coerceAtLeast(1f),
            Color.argb(115, 42, 36, 52),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.82f, minDim * 0.75f, paint)
        paint.shader = null

        val breathe = 1f + 0.012f * (loopedFbm(loop, radius = 1.3f, seedOffset = 17) * 2f - 1f)
        val cx = w * 0.5f
        val cy = h * 0.48f
        val r = 0.28f * minDim * breathe
        val tint = palette.colorAt(0)
        val tr = (Color.red(tint) + 232) / 2
        val tg = (Color.green(tint) + 240) / 2
        val tb = (Color.blue(tint) + 255) / 2
        val a = dim

        oval.set(cx - r * 1.15f, cy + r * 0.72f, cx + r * 1.15f, cy + r * 1.27f)
        paint.shader = RadialGradient(
            cx, cy + r * 1.05f, (r * 1.35f).coerceAtLeast(1f),
            Color.argb((0.45f * a * 255).toInt().coerceIn(0, 255), 0, 0, 0),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(oval, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            cx - r * 0.32f, cy - r * 0.38f, (r * 1.25f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((a * 0.65f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.argb((a * 0.55f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.argb((a * 0.28f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.argb((a * 0.7f * 255).toInt().coerceIn(0, 255), 10, 10, 18),
            ),
            floatArrayOf(0f, 0.28f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        val swirlSpin = 0.35f
        val swirl = timeAngle * swirlSpin
        val sx = cx + cos(swirl) * r * 0.2f
        val sy = cy + sin(swirl * 0.85f) * r * 0.16f
        paint.shader = RadialGradient(
            sx, sy, (r * 0.48f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((a * 0.5f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.argb((a * 0.22f * 255).toInt().coerceIn(0, 255), 168, 200, 232),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sx, sy, r * 0.48f, paint)
        paint.shader = null

        val sx2 = cx + cos(swirl + 2.1f) * r * 0.28f
        val sy2 = cy + sin(swirl + 1.4f) * r * 0.22f
        paint.shader = RadialGradient(
            sx2, sy2, (r * 0.32f).coerceAtLeast(1f),
            Color.argb((a * 0.18f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sx2, sy2, r * 0.32f, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (r * 0.035f).coerceAtLeast(1f)
        paint.color = Color.argb((a * 0.4f * 255).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.FILL

        val highlightAngle = -0.72f
        val hx = cx + cos(highlightAngle) * r * 0.4f
        val hy = cy + sin(highlightAngle) * r * 0.4f
        paint.shader = RadialGradient(
            hx, hy, (r * 0.26f).coerceAtLeast(1f),
            Color.argb((a * 0.95f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(hx, hy, r * 0.26f, paint)
        paint.shader = null

        paint.color = Color.argb((a * 0.5f * 255).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.12f, r * 0.055f, paint)

        for (j in 0 until 32) {
            val x0 = rnd.nextFloat()
            val y0 = 0.15f + rnd.nextFloat() * 0.6f
            val driftAmp = 0.01f + rnd.nextFloat() * 0.03f
            val seedOffset = j * 619 + 41
            val driftX = (loopedFbm(loop, radius = 1.2f, seedOffset = seedOffset) * 2f - 1f) * driftAmp
            val driftY = (loopedFbm(loop, radius = 1.5f, seedOffset = seedOffset + 3) * 2f - 1f) * driftAmp
            val px = (((x0 + driftX) % 1f) + 1f) % 1f * w
            val py = (((y0 + driftY) % 1f) + 1f) % 1f * h
            val twinkle = 0.35f + 0.65f * rnd.nextFloat()
            val moteR = (minDim * (0.0015f + rnd.nextFloat() * 0.003f) * (0.7f + 0.5f * twinkle))
                .coerceAtLeast(0.5f)
            paint.color = Color.argb(
                (0.4f * twinkle * dim * 255).toInt().coerceIn(0, 255),
                232, 224, 240,
            )
            canvas.drawCircle(px, py, moteR, paint)
        }
        paint.alpha = 255
        paint.shader = null
    }
}
