package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Lit Wire Lattice Depth for Auto/Ambient album art. */
internal object LitLatticeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private data class Vec3(val x: Float, val y: Float, val z: Float)

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
        val cx = w * 0.5f
        val cy = h * 0.5f
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val angle = phase * 2f * PI.toFloat() + rotationDeg * PI.toFloat() / 180f * 0.3f
        val tilt = 0.35f
        val grid = 5

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0B1220.toInt(), 0xFF030712.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val spotX = cx + cos(angle * 0.55f) * minDim * 0.28f
        val spotY = cy + sin(angle * 0.45f) * minDim * 0.18f
        val tint = palette.colorAt(1)
        paint.shader = RadialGradient(
            spotX, spotY, minDim * 0.45f,
            intArrayOf(
                Color.argb((55 * dim).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(spotX, spotY, minDim * 0.45f, paint)
        paint.shader = null

        val points = buildList {
            for (ix in 0 until grid) {
                for (iy in 0 until grid) {
                    for (iz in 0 until grid) {
                        add(
                            Vec3(
                                (ix / (grid - 1f)) * 2f - 1f,
                                (iy / (grid - 1f)) * 2f - 1f,
                                (iz / (grid - 1f)) * 2f - 1f,
                            ),
                        )
                    }
                }
            }
        }
        val focal = maxOf(w, h) * 0.9f
        data class Proj(val x: Float, val y: Float, val depth: Float, val lit: Float)
        val light = Vec3(cos(angle * 0.4f + 0.8f), 0.55f, sin(angle * 0.4f + 0.8f))
        val projected = points.map { p ->
            val ry = rotateY(p, angle)
            val rx = rotateX(ry, tilt)
            val depth = rx.z + 2.6f
            val scale = focal / depth
            val lit = ((rx.x * light.x + rx.y * light.y + rx.z * light.z) * 0.5f + 0.5f).coerceIn(0f, 1f)
            Proj(cx + rx.x * scale * 0.42f, cy + rx.y * scale * 0.42f, depth, lit)
        }

        val wire = palette.colorAt(0)
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.0022f
        val strideY = grid
        val strideZ = grid * grid
        for (ix in 0 until grid) {
            for (iy in 0 until grid) {
                for (iz in 0 until grid) {
                    val i = ix * strideZ + iy * strideY + iz
                    fun edge(j: Int) {
                        val pa = projected[i]
                        val pb = projected[j]
                        val fog = (1f - ((pa.depth + pb.depth) * 0.5f - 1.6f) / 2.4f).coerceIn(0.15f, 1f)
                        paint.color = Color.argb(
                            (45 * fog * dim).toInt().coerceIn(0, 255),
                            Color.red(wire), Color.green(wire), Color.blue(wire),
                        )
                        canvas.drawLine(pa.x, pa.y, pb.x, pb.y, paint)
                    }
                    if (ix + 1 < grid) edge(i + strideZ)
                    if (iy + 1 < grid) edge(i + strideY)
                    if (iz + 1 < grid) edge(i + 1)
                }
            }
        }

        paint.style = Paint.Style.FILL
        projected.forEach { p ->
            val fog = (1f - (p.depth - 1.6f) / 2.4f).coerceIn(0.2f, 1f)
            val spot = (1f - (hypot(p.x - spotX, p.y - spotY) / (minDim * 0.45f)).coerceIn(0f, 1f))
            val glow = (0.25f + 0.75f * p.lit) * (0.55f + 0.45f * spot)
            val r = minDim * (0.006f + 0.01f * glow) * fog
            paint.shader = RadialGradient(
                p.x, p.y, r * 2.2f,
                intArrayOf(
                    Color.argb((140 * glow * dim).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((110 * glow * dim).toInt().coerceIn(0, 255), Color.red(wire), Color.green(wire), Color.blue(wire)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(p.x, p.y, r * 2.2f, paint)
            paint.shader = null
            paint.color = Color.argb(
                ((0.4f + 0.5f * glow) * fog * dim * 255f).toInt().coerceIn(0, 255),
                Color.red(wire), Color.green(wire), Color.blue(wire),
            )
            canvas.drawCircle(p.x, p.y, r, paint)
        }
        // suppress unused warning from generation seed variety
        rnd.nextFloat()
    }

    private fun rotateY(p: Vec3, a: Float): Vec3 {
        val c = cos(a)
        val s = sin(a)
        return Vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
    }

    private fun rotateX(p: Vec3, a: Float): Vec3 {
        val c = cos(a)
        val s = sin(a)
        return Vec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
    }
}
