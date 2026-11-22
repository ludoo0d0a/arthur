package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one frozen frame of "Drifting Clouds" for Auto/Ambient album art. */
internal object CloudsStill {
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
        val minDim = w.coerceAtMost(h)
        val t01 = ((phase / (2f * PI.toFloat())) % 1f + 1f) % 1f

        val skyTop = palette.primary
        val skyMid = palette.secondary
        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                Color.argb(
                    255,
                    (Color.red(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                    (Color.green(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                    (Color.blue(skyTop) * 0.45f).toInt().coerceIn(0, 255),
                ),
                Color.argb(
                    255,
                    (Color.red(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                    (Color.green(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                    (Color.blue(skyMid) * 0.28f).toInt().coerceIn(0, 255),
                ),
                0xFF0A1018.toInt(),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cloudCount = 8
        val pulseMix = 0.85f + pulse * 0.15f
        val drift = t01 + rotationDeg / 360f

        // Far (blurred, slower) then near (crisper) for parallax depth.
        for (layer in 0..1) {
            val far = layer == 0
            if (far) {
                paint.maskFilter = BlurMaskFilter((size * 0.022f).coerceAtLeast(5f), BlurMaskFilter.Blur.NORMAL)
            } else {
                paint.maskFilter = null
            }
            val layerCount = if (far) cloudCount / 2 else cloudCount - cloudCount / 2
            val speedScale = if (far) 0.45f else 1f
            val alphaScale = if (far) 0.55f else 1f
            val yBias = if (far) 0.08f else 0f

            for (i in 0 until layerCount) {
                val x0 = rnd.nextFloat()
                val yFrac = 0.10f + yBias + rnd.nextFloat() * 0.48f
                val speedMul = (0.08f + rnd.nextFloat() * 0.14f) * speedScale
                val bobAmpFrac = 0.004f + rnd.nextFloat() * 0.014f
                val scale = (0.55f + rnd.nextFloat() * 0.6f) * pulseMix * (if (far) 1.15f else 1f)
                val tint = palette.colorAt(i + layer * 3)
                val puffCount = 4 + rnd.nextInt(3)

                val x = (((x0 + drift * speedMul) % 1f + 1f) % 1f) * w
                val bobNoise = loopedFbm(t01, radius = 1.2f + i * 0.07f, seedOffset = i * 97 + layer * 13) * 2f - 1f
                val y = yFrac * h + bobNoise * bobAmpFrac * h

                for (p in 0 until puffCount) {
                    val dx = -0.55f + rnd.nextFloat() * 1.1f
                    val dy = -0.28f + rnd.nextFloat() * 0.5f
                    val radiusFrac = 0.22f + rnd.nextFloat() * 0.26f
                    val cx = x + dx * scale * minDim * 0.55f
                    val cy = y + dy * scale * minDim * 0.35f
                    val radius = radiusFrac * scale * minDim * 0.22f

                    paint.color = Color.argb(
                        (0.10f * alphaScale * 255).toInt().coerceIn(0, 255),
                        Color.red(tint),
                        Color.green(tint),
                        Color.blue(tint),
                    )
                    canvas.drawCircle(cx, cy, radius * 1.45f, paint)

                    paint.color = Color.argb((0.22f * alphaScale * 255).toInt().coerceIn(0, 255), 232, 238, 245)
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }
        }
        paint.maskFilter = null
        paint.alpha = 255
    }
}
