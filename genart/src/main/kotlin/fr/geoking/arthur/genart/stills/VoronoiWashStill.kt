package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Voronoi Wash for Auto/Ambient album art. */
internal object VoronoiWashStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        paint.color = 0xFF07060C.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val count = 9
        val positions = ArrayList<Pair<Float, Float>>(count)
        val seeds = ArrayList<SiteSeed>(count)
        for (i in 0 until count) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val driftX = (rnd.nextFloat() - 0.5f) * 0.08f
            val driftY = (rnd.nextFloat() - 0.5f) * 0.08f
            val radiusFrac = 0.12f + rnd.nextFloat() * 0.1f
            val sides = 5 + i % 3
            val rot0 = rnd.nextFloat() * 2f * PI.toFloat()
            val rotSpeed = 0.1f + rnd.nextFloat() * 0.3f
            val x = (((x0 + loop * driftX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * driftY) % 1f) + 1f) % 1f * h
            positions.add(x to y)
            seeds.add(SiteSeed(radiusFrac, sides, rot0, rotSpeed))
        }

        for (i in 0 until count) {
            val (cx, cy) = positions[i]
            val seed = seeds[i]
            var nearest = minDim * 0.35f
            for (j in 0 until count) {
                if (j == i) continue
                val (ox, oy) = positions[j]
                val d = hypot(cx - ox, cy - oy)
                if (d < nearest) nearest = d
            }
            val radius = (nearest * 0.48f).coerceIn(minDim * 0.08f, seed.radiusFrac * minDim * 1.4f)
            val rot = seed.rot0 + loop * 2f * PI.toFloat() * seed.rotSpeed
            path.reset()
            for (k in 0 until seed.sides) {
                val a = rot + k * 2f * PI.toFloat() / seed.sides
                val r = radius * (0.92f + 0.08f * sin(a * 2f))
                val px = cx + cos(a) * r
                val py = cy + sin(a) * r
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            val tint = palette.colorAt(i)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (0.22f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            paint.color = Color.argb(
                (0.45f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (0.35f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawCircle(cx, cy, 2.5f, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private data class SiteSeed(
        val radiusFrac: Float,
        val sides: Int,
        val rot0: Float,
        val rotSpeed: Float,
    )
}
