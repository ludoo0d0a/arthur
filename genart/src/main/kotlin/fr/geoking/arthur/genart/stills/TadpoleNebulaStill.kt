package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
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

/** Bakes one frozen Hubble-palette Tadpole Nebula frame for Auto/Ambient album art. */
internal object TadpoleNebulaStill {
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
        val t01 = ((((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f) + 1f) % 1f
        val dim = 0.72f + 0.28f * pulse

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF02010A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val clusterCx = w * (0.48f + 0.03f * (loopedFbm(t01, 1.1f, seedOffset = 3) * 2f - 1f))
        val clusterCy = h * (0.42f + 0.03f * (loopedFbm(t01, 1.2f, seedOffset = 11) * 2f - 1f))
        val hub = cosmicArgb(0, palette, t01, 0)
        paint.shader = RadialGradient(
            clusterCx, clusterCy, minDim * 0.55f,
            intArrayOf(withAlpha(hub, (0.22f * dim * 255).toInt()), withAlpha(hub, (0.06f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(clusterCx, clusterCy, minDim * 0.55f, paint)
        paint.shader = null

        // Emission clouds
        for (i in 0 until 18) {
            val x0 = 0.08f + rnd.nextFloat() * 0.84f
            val y0 = 0.08f + rnd.nextFloat() * 0.84f
            val drift = 0.02f + rnd.nextFloat() * 0.05f
            val n = loopedFbm(t01, 0.8f + i * 0.04f, seedOffset = i * 619 + 71)
            val nx = loopedFbm(t01, 1.15f, seedOffset = i * 701 + 3)
            val ny = loopedFbm(t01, 1.25f, seedOffset = i * 709 + 5)
            val x = ((x0 + (nx * 2f - 1f) * drift + 1f) % 1f) * w
            val y = ((y0 + (ny * 2f - 1f) * drift + 1f) % 1f) * h
            val radius = (0.18f + rnd.nextFloat() * 0.3f) * minDim * (0.82f + 0.28f * n)
            val alphaBase = 0.10f + rnd.nextFloat() * 0.18f
            val tint = cosmicArgb(i % 3, palette, t01, i)
            val a = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(withAlpha(tint, a), withAlpha(tint, (a * 0.45f).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius.coerceAtLeast(1f), paint)
            paint.shader = null
        }

        // Tadpole pillars
        val tadpoleCount = 2 + (generation % 2).toInt()
        for (i in 0 until tadpoleCount) {
            val headX0 = 0.28f + rnd.nextFloat() * 0.34f
            val headY0 = 0.32f + rnd.nextFloat() * 0.36f
            val sway = (loopedFbm(t01, 1.3f, seedOffset = i * 811 + 9) * 2f - 1f) * 0.04f
            val angle = (0.2f + rnd.nextFloat() * 1.0f) + if (i % 2 == 0) 0f else PI.toFloat() * 0.85f + sway
            val hx = ((headX0 + sway * 0.3f + 1f) % 1f) * w
            val hy = ((headY0 - sway * 0.2f + 1f) % 1f) * h
            val hr = (0.055f + rnd.nextFloat() * 0.04f) * minDim * (0.9f + 0.15f * pulse)
            val headTint = cosmicArgb((i + 1) % 3, palette, t01, i + 4)
            paint.shader = RadialGradient(
                hx, hy, hr * 1.55f,
                intArrayOf(
                    withAlpha(brighten(headTint, 1.25f), (0.55f * dim * 255).toInt()),
                    withAlpha(headTint, (0.22f * dim * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, hr * 1.55f, paint)
            paint.shader = RadialGradient(
                hx - hr * 0.15f, hy - hr * 0.1f, hr,
                intArrayOf(withAlpha(0xFFF8FAFC.toInt(), (0.35f * dim * 255).toInt()), withAlpha(headTint, (0.4f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, hr, paint)
            paint.shader = null

            val segments = 5 + rnd.nextInt(5)
            val tailLen = (0.18f + rnd.nextFloat() * 0.14f) * minDim
            for (s in 1..segments) {
                val u = s / segments.toFloat()
                val falloff = 1f - u * 0.85f
                val wobble = sin(t01 * 2f * PI.toFloat() + s * 0.7f + i) * hr * 0.18f
                val tx = hx + cos(angle) * tailLen * u + cos(angle + PI.toFloat() / 2f) * wobble
                val ty = hy + sin(angle) * tailLen * u + sin(angle + PI.toFloat() / 2f) * wobble
                val tr = hr * (0.85f - u * 0.55f) * falloff
                val bodyTint = cosmicArgb((i + 1 + s) % 3, palette, t01, i + s)
                val a = (0.32f * falloff * dim * 255).toInt().coerceIn(0, 255)
                paint.shader = RadialGradient(
                    tx, ty, (tr * 1.6f).coerceAtLeast(1f),
                    intArrayOf(withAlpha(bodyTint, a), withAlpha(bodyTint, (a * 0.35f).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(tx, ty, (tr * 1.6f).coerceAtLeast(1f), paint)
                paint.shader = null
            }
        }

        // Dark dust lanes
        for (i in 0 until 7) {
            val x0 = 0.15f + rnd.nextFloat() * 0.7f
            val y0 = 0.2f + rnd.nextFloat() * 0.6f
            val drift = 0.01f + rnd.nextFloat() * 0.03f
            val n = loopedFbm(t01, 1.2f, seedOffset = i * 91 + 17)
            val x = ((x0 + (n * 2f - 1f) * drift + 1f) % 1f) * w
            val y = ((y0 + (loopedFbm(t01, 1.3f, seedOffset = 91) * 2f - 1f) * drift + 1f) % 1f) * h
            val r = (0.08f + rnd.nextFloat() * 0.14f) * minDim * (0.9f + 0.2f * n)
            val a = ((0.18f + rnd.nextFloat() * 0.24f) * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, r.coerceAtLeast(1f),
                intArrayOf(Color.argb(a, 5, 3, 12), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r.coerceAtLeast(1f), paint)
            paint.shader = null
        }

        // Starfield
        for (j in 0 until 200) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val a = 0.18f + rnd.nextFloat() * 0.7f
            val starSize = (0.0007f + rnd.nextFloat() * 0.0028f) * minDim
            val tint = when {
                j % 5 == 0 -> 0xFF7DD3FC.toInt()
                j % 7 == 0 -> 0xFFF9A8D4.toInt()
                j % 11 == 0 -> 0xFFFDBA74.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            val mixed = mixArgb(tint, palette.colorAt(j % 6), 0.2f)
            paint.color = withAlpha(mixed, (a * dim * 255).toInt())
            canvas.drawCircle(sx, sy, starSize.coerceAtLeast(0.7f), paint)
            if (j % 17 == 0) {
                val spike = starSize * 5f
                paint.strokeWidth = (starSize * 0.45f).coerceAtLeast(0.8f)
                paint.color = withAlpha(mixed, (a * 0.55f * dim * 255).toInt())
                canvas.drawLine(sx - spike, sy, sx + spike, sy, paint)
                canvas.drawLine(sx, sy - spike, sx, sy + spike, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // Open cluster
        for (i in 0 until 18) {
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val rr = (0.02f + rnd.nextFloat() * 0.12f) * minDim
            val x = clusterCx + cos(angle + t01 * 0.15f) * rr
            val y = clusterCy + sin(angle + t01 * 0.15f) * rr
            val core = (0.0016f + rnd.nextFloat() * 0.0028f) * minDim
            val a = (0.5f + rnd.nextFloat() * 0.45f) * dim
            paint.shader = RadialGradient(
                x, y, core * 3.2f,
                intArrayOf(withAlpha(0xFFE0F2FE.toInt(), (a * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, core * 3.2f, paint)
            paint.shader = null
            paint.color = withAlpha(0xFFFFFFFF.toInt(), (a * 255).toInt())
            canvas.drawCircle(x, y, core.coerceAtLeast(1f), paint)
        }

        // Foreground planets
        val planetCount = 1 + (generation % 2).toInt()
        for (i in 0 until planetCount) {
            val x0 = 0.12f + rnd.nextFloat() * 0.76f
            val y0 = 0.55f + rnd.nextFloat() * 0.33f
            val drift = 0.015f + rnd.nextFloat() * 0.025f
            val nx = loopedFbm(t01, 1.2f, seedOffset = i * 41 + 7)
            val ny = loopedFbm(t01, 1.35f, seedOffset = i * 43 + 11)
            val breath = 0.88f + 0.18f * loopedFbm(t01, 1.5f, seedOffset = i * 47 + 13)
            val px = ((x0 + (nx * 2f - 1f) * drift + 1f) % 1f) * w
            val py = ((y0 + (ny * 2f - 1f) * drift + 1f) % 1f) * h
            val pr = (0.035f + rnd.nextFloat() * 0.045f) * minDim * breath
            val base = mixArgb(palette.colorAt(i + 2), cosmicArgb(i % 3, palette, t01, i + 2), 0.35f)
            val ring = rnd.nextFloat() > 0.4f
            if (ring) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = pr * 0.11f
                paint.color = withAlpha(base, (0.4f * dim * 255).toInt())
                canvas.drawOval(RectF(px - pr * 1.85f, py - pr * 0.28f, px + pr * 1.85f, py + pr * 0.28f), paint)
                paint.style = Paint.Style.FILL
            }
            paint.shader = RadialGradient(
                px - pr * 0.28f, py - pr * 0.22f, pr * 1.2f,
                intArrayOf(
                    withAlpha(brighten(base, 1.2f), (0.95f * dim * 255).toInt()),
                    withAlpha(base, (0.85f * dim * 255).toInt()),
                    withAlpha(0xFF12081C.toInt(), (0.92f * dim * 255).toInt()),
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, pr, paint)
            paint.shader = RadialGradient(
                px + pr * 0.38f, py + pr * 0.12f, pr * 0.95f,
                intArrayOf(Color.TRANSPARENT, Color.argb((0.5f * dim * 255).toInt(), 0, 0, 0)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, pr, paint)
            paint.shader = null
        }

        paint.alpha = 255
        paint.strokeWidth = 1f
        paint.style = Paint.Style.FILL
    }

    private fun cosmicArgb(toneKind: Int, palette: AnimationPalette, colorPhase: Float, index: Int): Int {
        val sho = when (toneKind % 3) {
            0 -> 0xFF22D3EE.toInt()
            1 -> 0xFFFB923C.toInt()
            else -> 0xFFE879F9.toInt()
        }
        val alt = when ((toneKind + 1) % 3) {
            0 -> 0xFF38BDF8.toInt()
            1 -> 0xFFF97316.toInt()
            else -> 0xFFD946EF.toInt()
        }
        val cycle = loopedFbm(colorPhase, radius = 1.2f + (index % 5) * 0.07f, seedOffset = index * 37 + 5)
        val cosmic = mixArgb(sho, alt, cycle)
        return mixArgb(cosmic, palette.colorAt(index), 0.28f)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun brighten(rgb: Int, factor: Float): Int {
        val r = (Color.red(rgb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(rgb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(rgb) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
