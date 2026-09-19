package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Clifford Wash for Auto/Ambient album art. */
internal object CliffordWashStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private val presets = listOf(
        floatArrayOf(-1.4f, 1.6f, 1.0f, 0.7f),
        floatArrayOf(1.7f, 1.7f, 0.6f, 1.2f),
        floatArrayOf(-1.7f, 1.3f, -0.1f, -1.2f),
        floatArrayOf(1.5f, -1.8f, 1.6f, 0.9f),
        floatArrayOf(-1.8f, -2.0f, -0.5f, -0.9f),
    )

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
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.22f
        val pointCount = 1800
        val dim = 0.6f + 0.4f * pulse
        val preset = presets[rnd.nextInt(presets.size)]
        val a = preset[0]
        val b = preset[1]
        val c = preset[2]
        val d = preset[3]

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.maskFilter = null
        paint.color = 0xFF04020A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val tint = palette.colorAt(0)
        paint.maskFilter = BlurMaskFilter((minDim * 0.06f).coerceAtLeast(6f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.42f,
            intArrayOf(
                Color.argb((0.25f * dim * 255).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.42f, paint)
        paint.maskFilter = null
        paint.shader = null

        canvas.save()
        canvas.rotate(phase * 360f + rotationDeg, cx, cy)

        var x = 0.1f
        var y = 0.1f
        repeat(40) {
            val nx = sin(a * y) + c * cos(a * x)
            val ny = sin(b * x) + d * cos(b * y)
            x = nx
            y = ny
        }

        val inkA = palette.colorAt(0)
        val inkB = palette.colorAt(2)
        val dotR = (minDim / 900f).coerceIn(0.6f, 1.8f)
        for (i in 0 until pointCount) {
            val nx = sin(a * y) + c * cos(a * x)
            val ny = sin(b * x) + d * cos(b * y)
            x = nx
            y = ny
            val mixT = i / pointCount.toFloat()
            val ir = (Color.red(inkA) + (Color.red(inkB) - Color.red(inkA)) * mixT).toInt().coerceIn(0, 255)
            val ig = (Color.green(inkA) + (Color.green(inkB) - Color.green(inkA)) * mixT).toInt().coerceIn(0, 255)
            val ib = (Color.blue(inkA) + (Color.blue(inkB) - Color.blue(inkA)) * mixT).toInt().coerceIn(0, 255)
            val alpha = ((0.12f + 0.45f * (1f - mixT * 0.35f)) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, ir, ig, ib)
            canvas.drawCircle(cx + x * scale, cy + y * scale, dotR, paint)
        }
        canvas.restore()
    }
}
