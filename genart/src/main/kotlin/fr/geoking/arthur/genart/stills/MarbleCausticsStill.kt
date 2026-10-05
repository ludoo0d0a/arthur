package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Marble Caustics for Auto/Ambient album art. */
internal object MarbleCausticsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
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
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.18f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF12161C.toInt(), 0xFF070A0E.toInt(), 0xFF030508.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.82f, (minDim * 0.75f).coerceAtLeast(1f),
            Color.argb(102, 28, 36, 48),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.82f, minDim * 0.75f, paint)
        paint.shader = null

        data class Orb(
            val x0: Float,
            val y0: Float,
            val radiusFrac: Float,
            val bobAmp: Float,
            val bobFreq: Float,
            val bobPhase: Float,
            val highlightAngle: Float,
            val colorIndex: Int,
        )

        val orbCount = 4
        val orbs = List(orbCount) { i ->
            Orb(
                x0 = 0.18f + rnd.nextFloat() * 0.64f,
                y0 = 0.28f + rnd.nextFloat() * 0.24f,
                radiusFrac = 0.1f + rnd.nextFloat() * 0.08f,
                bobAmp = 0.008f + rnd.nextFloat() * 0.012f,
                bobFreq = 0.2f + rnd.nextFloat() * 0.3f,
                bobPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                highlightAngle = -1.1f + rnd.nextFloat() * 0.7f,
                colorIndex = i,
            )
        }

        for (j in 0 until orbCount * 5) {
            val orb = orbs[j % orbCount]
            val bob = orb.bobAmp * sin(time * orb.bobFreq + orb.bobPhase)
            val cx = orb.x0 * w
            val cy = (orb.y0 + bob) * h
            val r = orb.radiusFrac * minDim
            val floorY = cy + r * 1.15f
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val lengthFrac = 0.08f + rnd.nextFloat() * 0.14f
            val curve = 0.02f + rnd.nextFloat() * 0.05f
            val thickness = (0.008f + rnd.nextFloat() * 0.012f) * minDim
            val shimmerFreq = 0.3f + rnd.nextFloat() * 0.6f
            val shimmerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alpha = 0.12f + rnd.nextFloat() * 0.2f
            val shimmer = 0.55f + 0.45f * (0.5f + 0.5f * sin(time * shimmerFreq + shimmerPhase))
            val tint = palette.colorAt(orb.colorIndex)
            val tr = Color.red(tint)
            val tg = Color.green(tint)
            val tb = Color.blue(tint)
            val a = alpha * shimmer * dim

            path.reset()
            path.moveTo(cx, floorY)
            for (s in 1..10) {
                val u = s / 10f
                val len = lengthFrac * minDim * u
                val bend = curve * minDim * sin(u * PI.toFloat())
                val px = cx + cos(angle) * len + cos(angle + PI.toFloat() / 2f) * bend
                val py = floorY + sin(angle) * len * 0.35f + bend * 0.2f
                path.lineTo(px, py)
            }
            paint.shader = RadialGradient(
                cx, floorY, (lengthFrac * minDim).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((a * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                    Color.argb((a * 0.35f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = thickness * (0.7f + 0.5f * shimmer)
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null

            paint.shader = RadialGradient(
                cx, floorY, (r * 1.4f).coerceAtLeast(1f),
                Color.argb((a * 0.55f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, floorY, r * 1.4f, paint)
            paint.shader = null
        }

        orbs.forEach { orb ->
            val bob = orb.bobAmp * sin(time * orb.bobFreq + orb.bobPhase)
            val cx = orb.x0 * w
            val cy = (orb.y0 + bob) * h
            val r = orb.radiusFrac * minDim
            val tint = palette.colorAt(orb.colorIndex)
            val tr = (Color.red(tint) + 221) / 2
            val tg = (Color.green(tint) + 232) / 2
            val tb = (Color.blue(tint) + 248) / 2

            oval.set(cx - r * 0.9f, cy + r * 0.75f, cx + r * 0.9f, cy + r * 1.2f)
            paint.shader = RadialGradient(
                cx, cy + r * 1.05f, (r * 1.2f).coerceAtLeast(1f),
                Color.argb((0.35f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(oval, paint)
            paint.shader = null

            paint.shader = RadialGradient(
                cx - r * 0.3f, cy - r * 0.35f, (r * 1.2f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                    Color.argb((0.18f * dim * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                    Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 8, 12, 18),
                ),
                floatArrayOf(0f, 0.25f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (r * 0.035f).coerceAtLeast(0.8f)
            paint.color = Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(cx, cy, r, paint)
            paint.style = Paint.Style.FILL

            val hx = cx + cos(orb.highlightAngle) * r * 0.4f
            val hy = cy + sin(orb.highlightAngle) * r * 0.4f
            paint.shader = RadialGradient(
                hx, hy, (r * 0.32f).coerceAtLeast(1f),
                Color.argb((0.9f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, r * 0.32f, paint)
            paint.shader = null

            paint.color = Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(cx + r * 0.48f, cy - r * 0.1f, r * 0.05f, paint)
        }
        paint.alpha = 255
        paint.shader = null
    }
}
