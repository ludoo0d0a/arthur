package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of the vanishing tunnel for Auto/Ambient album art. */
internal object TunnelStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val RINGS = 14
    private const val SPOKES = 12
    private const val K = 0.55f

    fun draw(
        canvas: Canvas,
        size: Int,
        @Suppress("UNUSED_PARAMETER") generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = min(w, h) * 0.78f
        val dim = 0.65f + 0.35f * pulse.coerceIn(0f, 1f)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx,
            cy,
            maxR,
            intArrayOf(0xFF1E1B4B.toInt(), 0xFF0B1026.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val scroll = phase01(phase / (2f * PI.toFloat()) + rotationDeg / 360f)
        val spin = scroll * 0.35f * 2f * PI.toFloat()
        val step = 1f / RINGS
        val spokeInner = maxR * 0.04f

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (s in 0 until SPOKES) {
            val angle = spin + s * (2f * PI.toFloat() / SPOKES)
            val c = palette.colorAt(s)
            paint.strokeWidth = 2.2f
            paint.color = Color.argb(
                (0.28f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawLine(
                cx + cos(angle) * spokeInner,
                cy + sin(angle) * spokeInner,
                cx + cos(angle) * maxR,
                cy + sin(angle) * maxR,
                paint,
            )
        }

        for (i in 0 until RINGS) {
            val depth = phase01(i * step + scroll)
            val scale = 1f / (1f + depth * RINGS * K)
            val radius = (maxR * scale).coerceAtLeast(2f)
            val alpha = (0.22f + 0.68f * scale).coerceIn(0.15f, 0.92f) * dim
            val stroke = (1.4f + 5f * scale).coerceIn(1.4f, 6.5f)
            val c = palette.colorAt(i)
            paint.strokeWidth = stroke
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }

        val core = palette.colorAt(0)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx,
            cy,
            (maxR * 0.12f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), Color.red(core), Color.green(core), Color.blue(core)),
                Color.argb((0.12f * dim * 255).toInt().coerceIn(0, 255), Color.red(core), Color.green(core), Color.blue(core)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, maxR * 0.12f, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun phase01(t: Float): Float = ((t % 1f) + 1f) % 1f
}
