package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.fbm2D
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one frozen frame of Marble Drift for Auto/Ambient album art. */
internal object MarbleDriftStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1614.toInt(), 0xFF0C0A0E.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val gridSide = 8
        val warpDrift = loopedFbm(loop, radius = 1.4f, seedOffset = 11) * 0.35f
        val veinDrift = loopedFbm(loop, radius = 1.8f, seedOffset = 47) * 0.25f

        for (i in 0 until gridSide * gridSide) {
            val col = i % gridSide
            val row = i / gridSide
            val gx = (col + 0.5f) / gridSide
            val gy = (row + 0.5f) / gridSide
            val jitterX = -0.04f + rnd.nextFloat() * 0.08f
            val jitterY = -0.04f + rnd.nextFloat() * 0.08f
            val radiusFrac = 0.14f + rnd.nextFloat() * 0.14f
            val alphaBase = 0.22f + rnd.nextFloat() * 0.33f
            val seedOffset = i * 733 + 19

            val nx = gx * 2.8f + warpDrift
            val ny = gy * 2.8f + veinDrift
            val warpX = fbm2D(nx, ny, octaves = 3, seedOffset = seedOffset) * 0.85f
            val warpY = fbm2D(nx + 5.1f, ny + 3.7f, octaves = 3, seedOffset = seedOffset + 101) * 0.85f
            val vein = fbm2D(
                nx * 1.6f + warpX,
                ny * 1.6f + warpY,
                octaves = 4,
                seedOffset = seedOffset + 211,
            )
            val veinSharp = (vein * vein * (3f - 2f * vein)).coerceIn(0f, 1f)

            val tint = palette.colorAt(i)
            val stoneR = (107 + Color.red(tint)) / 2
            val stoneG = (94 + Color.green(tint)) / 2
            val stoneB = (82 + Color.blue(tint)) / 2
            val jadeR = (58 + Color.red(tint)) / 2
            val jadeG = (107 + Color.green(tint)) / 2
            val jadeB = (90 + Color.blue(tint)) / 2
            val onyxR = (42 + Color.red(tint)) / 2
            val onyxG = (36 + Color.green(tint)) / 2
            val onyxB = (48 + Color.blue(tint)) / 2

            val (vr, vg, vb) = when {
                veinSharp > 0.62f -> {
                    val u = (veinSharp - 0.62f) / 0.38f
                    Triple(
                        (jadeR + ((142 - jadeR) * u).toInt()).coerceIn(0, 255),
                        (jadeG + ((207 - jadeG) * u).toInt()).coerceIn(0, 255),
                        (jadeB + ((176 - jadeB) * u).toInt()).coerceIn(0, 255),
                    )
                }
                veinSharp > 0.38f -> {
                    val u = (veinSharp - 0.38f) / 0.24f
                    Triple(
                        (stoneR + ((jadeR - stoneR) * u).toInt()).coerceIn(0, 255),
                        (stoneG + ((jadeG - stoneG) * u).toInt()).coerceIn(0, 255),
                        (stoneB + ((jadeB - stoneB) * u).toInt()).coerceIn(0, 255),
                    )
                }
                else -> {
                    val u = veinSharp / 0.38f
                    Triple(
                        (onyxR + ((stoneR - onyxR) * u).toInt()).coerceIn(0, 255),
                        (onyxG + ((stoneG - onyxG) * u).toInt()).coerceIn(0, 255),
                        (onyxB + ((stoneB - onyxB) * u).toInt()).coerceIn(0, 255),
                    )
                }
            }

            val breathe = 0.92f + 0.08f * loopedFbm(loop, radius = 1.1f, seedOffset = seedOffset + 7)
            val cx = (((gx + jitterX) % 1f) + 1f) % 1f * w
            val cy = (((gy + jitterY) % 1f) + 1f) % 1f * h
            val radius = (radiusFrac * minDim * breathe * (0.85f + 0.3f * veinSharp)).coerceAtLeast(1f)
            val alpha = (alphaBase * (0.45f + 0.55f * veinSharp) * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.argb(alpha, vr, vg, vb),
                    Color.argb((alpha * 0.35f).toInt(), vr, vg, vb),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, (minDim * 0.78f).coerceAtLeast(1f),
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), 5, 4, 8),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.5f, minDim * 0.95f, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
